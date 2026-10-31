package com.larkspur.offers;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.larkspur.testing.CommerceServiceTest;
import com.larkspur.testing.Datasets;
import com.larkspur.testing.DownstreamServers;
import com.larkspur.testing.EmbeddedRedis;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@CommerceServiceTest
class OfferQueryTest {
  @DynamicPropertySource
  static void redis(DynamicPropertyRegistry registry) {
    EmbeddedRedis.start();
    registry.add("spring.data.redis.host", () -> "127.0.0.1");
    registry.add("spring.data.redis.port", EmbeddedRedis::port);
  }

  @Autowired MockMvc mvc;
  @Autowired ObjectMapper json;

  @BeforeEach
  void resetDownstream() {
    DownstreamServers.reset();
  }

  @Test
  void listReturnsTheCreatedOfferAndActiveFiltersByUtcWindow() throws Exception {
    LocalDate today = LocalDate.now(ZoneOffset.UTC);
    UUID keptId = UUID.randomUUID();
    UUID droppedId = UUID.randomUUID();
    stubLookup(keptId);
    MvcResult kept = mvc.perform(post("/api/offers")
            .contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsBytes(Datasets.offer(
                "ACT-" + UUID.randomUUID().toString().substring(0, 8),
                "Open", "ACTIVE", today, today, List.of(keptId)))))
        .andExpect(status().isCreated())
        .andReturn();
    DownstreamServers.reset();
    stubLookup(droppedId);
    MvcResult dropped = mvc.perform(post("/api/offers")
            .contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsBytes(Datasets.offer(
                "END-" + UUID.randomUUID().toString().substring(0, 8),
                "Closed", "ACTIVE", today.minusDays(10), today.minusDays(1), List.of(droppedId)))))
        .andExpect(status().isCreated())
        .andReturn();
    String keptOffer = json.readTree(kept.getResponse().getContentAsString()).get("id").asText();
    String droppedOffer = json.readTree(dropped.getResponse().getContentAsString()).get("id").asText();

    mvc.perform(get("/api/offers"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[?(@.id == '" + keptOffer + "')]").exists());
    mvc.perform(get("/api/offers/active"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[?(@.id == '" + keptOffer + "')]").exists())
        .andExpect(jsonPath("$[?(@.id == '" + droppedOffer + "')]").doesNotExist());
  }

  @Test
  void unknownOfferIs404() throws Exception {
    mvc.perform(get("/api/offers/" + UUID.randomUUID()))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").value("Offer not found."));
  }

  private void stubLookup(UUID productId) {
    DownstreamServers.server("catalogRestClient")
        .expect(requestTo(org.hamcrest.Matchers.containsString("/api/products/lookup")))
        .andRespond(withSuccess(productJson(productId, "HED-" + productId.toString().substring(0, 8), "Carafe"), MediaType.APPLICATION_JSON));
  }

  private static String productJson(UUID id, String sku, String name) {
    return "[{\"id\":\"" + id + "\",\"sku\":\"" + sku + "\",\"name\":\"" + name + "\",\"category\":\"Home\",\"unitPrice\":48.00}]";
  }
}
