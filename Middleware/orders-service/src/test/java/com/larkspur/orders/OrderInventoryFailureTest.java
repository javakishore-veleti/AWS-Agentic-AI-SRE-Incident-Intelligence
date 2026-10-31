package com.larkspur.orders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.larkspur.testing.CommerceServiceTest;
import com.larkspur.testing.DownstreamServers;
import com.larkspur.testing.ShopperTokens;
import java.net.ConnectException;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@CommerceServiceTest
class OrderInventoryFailureTest {
  @Autowired MockMvc mvc;
  @Autowired ObjectMapper json;

  @BeforeEach
  void resetDownstream() {
    DownstreamServers.reset();
  }

  @Test
  void inventoryConflictDeletesTheOrderAndUsesTheBodyMessage() throws Exception {
    Set<String> before = orderNumbers();
    UUID offerId = UUID.randomUUID();
    stubOffer(offerId, "ACTIVE", UUID.randomUUID(), "HED-CUP", "Cup");
    DownstreamServers.server("inventoryClient")
        .expect(requestTo("http://inventory.test/api/reservations"))
        .andRespond(withStatus(HttpStatus.CONFLICT)
            .contentType(MediaType.APPLICATION_JSON)
            .body("{\"message\":\"No stock record for HED-CUP.\"}"));
    purchase(offerId)
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.message").value("No stock record for HED-CUP."));
    assertThat(orderNumbers()).isEqualTo(before);
  }

  @Test
  void inventoryConflictWithoutAMessageUsesTheFallback() throws Exception {
    Set<String> before = orderNumbers();
    UUID offerId = UUID.randomUUID();
    stubOffer(offerId, "ACTIVE", UUID.randomUUID(), "HED-CUP", "Cup");
    DownstreamServers.server("inventoryClient")
        .expect(requestTo("http://inventory.test/api/reservations"))
        .andRespond(withStatus(HttpStatus.CONFLICT).contentType(MediaType.APPLICATION_JSON).body("{\"status\":409}"));
    purchase(offerId)
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.message").value("Inventory could not reserve this offer."));
    assertThat(orderNumbers()).isEqualTo(before);
  }

  @Test
  void inventoryConnectionDropDeletesTheOrder() throws Exception {
    Set<String> before = orderNumbers();
    UUID offerId = UUID.randomUUID();
    stubOffer(offerId, "ACTIVE", UUID.randomUUID(), "HED-CUP", "Cup");
    DownstreamServers.server("inventoryClient")
        .expect(requestTo("http://inventory.test/api/reservations"))
        .andRespond(withException(new ConnectException("down")));
    purchase(offerId)
        .andExpect(status().isBadGateway())
        .andExpect(jsonPath("$.message").value("Inventory service is unreachable."));
    assertThat(orderNumbers()).isEqualTo(before);
  }

  private ResultActions purchase(UUID offerId) throws Exception {
    return mvc.perform(post("/api/orders")
        .header("Authorization", ShopperTokens.bearer(UUID.randomUUID(), "Ada Lovelace", "ada@example.test"))
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"offerId\":\"" + offerId + "\"}"));
  }

  private void stubOffer(UUID offerId, String status, UUID productId, String sku, String name) {
    String body = "{\"id\":\"" + offerId + "\",\"name\":\"Morning\",\"status\":\"" + status
        + "\",\"offerPrice\":139.00,\"products\":[{\"id\":\"" + productId + "\",\"sku\":\"" + sku
        + "\",\"name\":\"" + name + "\",\"unitPrice\":48.00}]}";
    DownstreamServers.server("offersClient")
        .expect(requestTo("http://offers.test/api/offers/" + offerId))
        .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));
  }

  private Set<String> orderNumbers() throws Exception {
    JsonNode rows = json.readTree(mvc.perform(get("/api/orders")).andExpect(status().isOk())
        .andReturn().getResponse().getContentAsString());
    Set<String> numbers = new HashSet<>();
    rows.forEach(row -> numbers.add(row.get("orderNumber").asText()));
    return numbers;
  }
}
