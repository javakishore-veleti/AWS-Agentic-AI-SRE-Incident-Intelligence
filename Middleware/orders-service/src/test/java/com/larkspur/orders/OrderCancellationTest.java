package com.larkspur.orders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withNoContent;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.larkspur.testing.CommerceServiceTest;
import com.larkspur.testing.DownstreamServers;
import com.larkspur.testing.ShopperTokens;
import java.net.ConnectException;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

@CommerceServiceTest
class OrderCancellationTest {
  @Autowired MockMvc mvc;
  @Autowired ObjectMapper json;
  @Autowired OrderCancellationRepository cancellations;

  @BeforeEach
  void resetDownstream() {
    DownstreamServers.reset();
  }

  @Test
  void cancelKeepsTheOrderAndWritesOneAudit() throws Exception {
    JsonNode order = openOrder();
    String id = order.get("id").asText();
    DownstreamServers.server("inventoryClient")
        .expect(requestTo("http://inventory.test/api/reservations/release"))
        .andRespond(withNoContent());
    mvc.perform(delete("/api/orders/" + id).header("Authorization", ShopperTokens.admin()))
        .andExpect(status().isNoContent());
    mvc.perform(get("/api/orders/" + id).header("Authorization", ShopperTokens.admin()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("CANCELLED"))
        .andExpect(jsonPath("$.orderNumber").value(order.get("orderNumber").asText()));
    mvc.perform(delete("/api/orders/" + id).header("Authorization", ShopperTokens.admin()))
        .andExpect(status().isNoContent());
    OrderCancellation audit = cancellations.findByOrderId(UUID.fromString(id)).orElseThrow();
    assertThat(audit.previousStatus.name()).isEqualTo(order.get("status").asText());
    assertThat(audit.actorEmail).isEqualTo("desk@heddle.example");
    assertThat(cancellations.findByOrderId(UUID.fromString(id))).isPresent();
  }

  @Test
  void customerCannotCancel() throws Exception {
    JsonNode order = openOrder();
    mvc.perform(delete("/api/orders/" + order.get("id").asText())
            .header("Authorization", ShopperTokens.bearer(UUID.randomUUID(), "Ada", "ada@example.test")))
        .andExpect(status().isForbidden());
    mvc.perform(get("/api/orders/" + order.get("id").asText()).header("Authorization", ShopperTokens.admin()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value(order.get("status").asText()));
  }

  @Test
  void unreachableInventoryLeavesTheOrderUnchanged() throws Exception {
    JsonNode order = openOrder();
    DownstreamServers.server("inventoryClient")
        .expect(requestTo("http://inventory.test/api/reservations/release"))
        .andRespond(withException(new ConnectException("down")));
    mvc.perform(delete("/api/orders/" + order.get("id").asText()).header("Authorization", ShopperTokens.admin()))
        .andExpect(status().isBadGateway())
        .andExpect(jsonPath("$.message").value("Inventory service is unreachable."));
    mvc.perform(get("/api/orders/" + order.get("id").asText()).header("Authorization", ShopperTokens.admin()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value(order.get("status").asText()));
    assertThat(cancellations.findByOrderId(UUID.fromString(order.get("id").asText()))).isEmpty();
  }

  private JsonNode openOrder() throws Exception {
    JsonNode rows = json.readTree(mvc.perform(get("/api/orders").header("Authorization", ShopperTokens.admin()))
        .andExpect(status().isOk())
        .andReturn()
        .getResponse()
        .getContentAsString());
    for (JsonNode row : rows) {
      if (!"CANCELLED".equals(row.get("status").asText())) {
        return row;
      }
    }
    throw new IllegalStateException("No open order");
  }
}
