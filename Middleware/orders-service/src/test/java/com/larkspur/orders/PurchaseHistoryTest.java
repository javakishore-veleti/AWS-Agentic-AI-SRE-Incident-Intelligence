package com.larkspur.orders;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withNoContent;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.larkspur.testing.CommerceServiceTest;
import com.larkspur.testing.DownstreamServers;
import com.larkspur.testing.ShopperTokens;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@CommerceServiceTest
class PurchaseHistoryTest {
  @Autowired MockMvc mvc;
  @Autowired ObjectMapper json;

  @BeforeEach
  void resetDownstream() {
    DownstreamServers.reset();
  }

  @Test
  void purchasePlacesAnOrderAndMineReturnsItForThatCustomer() throws Exception {
    UUID customerId = UUID.randomUUID();
    UUID otherId = UUID.randomUUID();
    UUID offerId = UUID.randomUUID();
    UUID productId = UUID.randomUUID();
    String token = ShopperTokens.bearer(customerId, "Ada Lovelace", "ada@example.test");
    stubOffer(offerId, "ACTIVE", productId, "HED-CARAFE", "Carafe");
    DownstreamServers.server("inventoryClient")
        .expect(requestTo("http://inventory.test/api/reservations"))
        .andExpect(method(HttpMethod.POST))
        .andExpect(content().string(org.hamcrest.Matchers.containsString("HD-")))
        .andExpect(content().string(org.hamcrest.Matchers.containsString("\"qty\":1")))
        .andRespond(withNoContent());

    mvc.perform(post("/api/orders")
            .header("Authorization", token)
            .header("Idempotency-Key", UUID.randomUUID().toString())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"offerId\":\"" + offerId + "\"}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.orderNumber").value(org.hamcrest.Matchers.startsWith("HD-")))
        .andExpect(jsonPath("$.status").value("PLACED"))
        .andExpect(jsonPath("$.customerId").value(customerId.toString()))
        .andExpect(jsonPath("$.customerName").value("Ada Lovelace"))
        .andExpect(jsonPath("$.customerEmail").value("ada@example.test"))
        .andExpect(jsonPath("$.totalAmount").value(139.00))
        .andExpect(jsonPath("$.lines[0].quantity").value(1))
        .andExpect(jsonPath("$.lines[0].sku").value("HED-CARAFE"));

    mvc.perform(get("/api/orders/mine").header("Authorization", token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[?(@.customerId == '" + customerId + "')]").exists());
    mvc.perform(get("/api/orders/mine").header("Authorization", ShopperTokens.bearer(otherId, "Other", "other@example.test")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[?(@.customerId == '" + otherId + "')]").doesNotExist());
  }

  @Test
  void missingAndInvalidTokenAre401() throws Exception {
    mvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content("{\"offerId\":\"" + UUID.randomUUID() + "\"}"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.message").value("Sign in to purchase an offer."));
    mvc.perform(post("/api/orders")
            .header("Authorization", "Bearer not-a-token")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"offerId\":\"" + UUID.randomUUID() + "\"}"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.message").value("Sign in to purchase an offer."));
  }

  private void stubOffer(UUID offerId, String status, UUID productId, String sku, String name) {
    String body = "{\"id\":\"" + offerId + "\",\"name\":\"Morning\",\"status\":\"" + status
        + "\",\"offerPrice\":139.00,\"products\":[{\"id\":\"" + productId + "\",\"sku\":\"" + sku
        + "\",\"name\":\"" + name + "\",\"unitPrice\":48.00}]}";
    DownstreamServers.server("offersClient")
        .expect(requestTo("http://offers.test/api/offers/" + offerId))
        .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));
  }
}
