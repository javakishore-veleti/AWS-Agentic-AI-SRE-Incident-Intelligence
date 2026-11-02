package com.larkspur.orders;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
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
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@CommerceServiceTest
class OrderAvailabilityTest {
  @Autowired MockMvc mvc;
  @Autowired ObjectMapper json;

  @BeforeEach
  void resetDownstream() {
    DownstreamServers.reset();
  }

  @Test
  void inactiveOfferSavesNoRow() throws Exception {
    Set<String> before = orderNumbers();
    UUID offerId = UUID.randomUUID();
    stubOffer(offerId, "EXPIRED", UUID.randomUUID(), "HED-OLD", "Old");
    purchase(offerId)
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.message").value("This offer is not available to purchase."));
    org.assertj.core.api.Assertions.assertThat(orderNumbers()).isEqualTo(before);
  }

  @Test
  void offerWithoutProductsSavesNoRow() throws Exception {
    Set<String> before = orderNumbers();
    UUID offerId = UUID.randomUUID();
    DownstreamServers.server("offersClient")
        .expect(requestTo("http://offers.test/api/offers/" + offerId))
        .andRespond(withSuccess(
            "{\"id\":\"" + offerId + "\",\"name\":\"Empty\",\"status\":\"ACTIVE\",\"offerPrice\":139.00,\"products\":[]}",
            MediaType.APPLICATION_JSON));
    purchase(offerId)
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.message").value("This offer does not include any products."));
    org.assertj.core.api.Assertions.assertThat(orderNumbers()).isEqualTo(before);
  }

  @Test
  void unknownOrderIs404() throws Exception {
    mvc.perform(get("/api/orders/" + UUID.randomUUID()).header("Authorization", ShopperTokens.admin()))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").value("Order not found."));
  }

  private org.springframework.test.web.servlet.ResultActions purchase(UUID offerId) throws Exception {
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
    JsonNode rows = json.readTree(mvc.perform(get("/api/orders").header("Authorization", ShopperTokens.admin())).andExpect(status().isOk())
        .andReturn().getResponse().getContentAsString());
    Set<String> numbers = new HashSet<>();
    rows.forEach(row -> numbers.add(row.get("orderNumber").asText()));
    return numbers;
  }
}
