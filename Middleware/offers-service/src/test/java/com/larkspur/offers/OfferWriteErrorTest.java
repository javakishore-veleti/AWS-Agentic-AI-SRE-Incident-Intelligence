package com.larkspur.offers;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.larkspur.testing.CommerceServiceTest;
import com.larkspur.testing.Datasets;
import com.larkspur.testing.DownstreamServers;
import com.larkspur.testing.EmbeddedRedis;
import java.net.ConnectException;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@CommerceServiceTest
class OfferWriteErrorTest {
  @DynamicPropertySource
  static void redis(DynamicPropertyRegistry registry) {
    EmbeddedRedis.start();
    registry.add("spring.data.redis.host", () -> "127.0.0.1");
    registry.add("spring.data.redis.port", EmbeddedRedis::port);
  }

  @Autowired MockMvc mvc;
  @Autowired ObjectMapper json;
  @Autowired StringRedisTemplate redisTemplate;

  @BeforeEach
  void resetDownstream() {
    DownstreamServers.reset();
  }

  @Test
  void endBeforeStartIs400() throws Exception {
    LocalDate today = LocalDate.now(ZoneOffset.UTC);
    mvc.perform(post("/api/offers")
            .contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsBytes(Datasets.offer(
                "DATE-" + UUID.randomUUID().toString().substring(0, 8),
                "Backwards", "ACTIVE", today, today.minusDays(1), List.of(UUID.randomUUID())))))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("The end date is before the start date."));
  }

  @Test
  void repeatedProductIs400() throws Exception {
    UUID productId = UUID.randomUUID();
    mvc.perform(post("/api/offers")
            .contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsBytes(Datasets.offer(
                "REP-" + UUID.randomUUID().toString().substring(0, 8),
                "Repeat", "ACTIVE", null, null, List.of(productId, productId)))))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("Choose each product once."));
  }

  @Test
  void missingCatalogProductIs400() throws Exception {
    UUID productId = UUID.randomUUID();
    DownstreamServers.server("catalogRestClient")
        .expect(requestTo(org.hamcrest.Matchers.containsString("/api/products/lookup")))
        .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));
    mvc.perform(post("/api/offers")
            .contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsBytes(Datasets.offer(
                "MISS-" + UUID.randomUUID().toString().substring(0, 8),
                "Missing", "ACTIVE", null, null, List.of(productId)))))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("One or more products are not in the catalog."));
  }

  @Test
  void catalogFailureOnCreateAndUpdateIs502() throws Exception {
    UUID first = UUID.randomUUID();
    UUID second = UUID.randomUUID();
    stubLookup(first);
    MvcResult created = mvc.perform(post("/api/offers")
            .contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsBytes(Datasets.offer(
                "UP-" + UUID.randomUUID().toString().substring(0, 8),
                "Editable", "ACTIVE", null, null, List.of(first)))))
        .andExpect(status().isCreated())
        .andReturn();
    String id = json.readTree(created.getResponse().getContentAsString()).get("id").asText();

    DownstreamServers.reset();
    DownstreamServers.server("catalogRestClient")
        .expect(requestTo(org.hamcrest.Matchers.containsString("/api/products/lookup")))
        .andRespond(withException(new ConnectException("down")));
    mvc.perform(post("/api/offers")
            .contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsBytes(Datasets.offer(
                "DOWN-" + UUID.randomUUID().toString().substring(0, 8),
                "Unreachable", "ACTIVE", null, null, List.of(second)))))
        .andExpect(status().isBadGateway())
        .andExpect(jsonPath("$.message").value("Product catalog is unreachable."));

    DownstreamServers.reset();
    DownstreamServers.server("catalogRestClient")
        .expect(requestTo(org.hamcrest.Matchers.containsString("/api/products/lookup")))
        .andRespond(withException(new ConnectException("down")));
    mvc.perform(put("/api/offers/" + id)
            .contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsBytes(Datasets.offer(
                "UP2-" + UUID.randomUUID().toString().substring(0, 8),
                "Edited", "ACTIVE", null, null, List.of(second)))))
        .andExpect(status().isBadGateway())
        .andExpect(jsonPath("$.message").value("Product catalog is unreachable."));
  }

  @Test
  void catalogFailureOnListReturnsEmptySummaries() throws Exception {
    Set<String> cached = redisTemplate.keys("catalog:product:*");
    if (cached != null && !cached.isEmpty()) {
      redisTemplate.delete(cached);
    }
    DownstreamServers.server("catalogRestClient")
        .expect(requestTo(org.hamcrest.Matchers.containsString("/api/products/lookup")))
        .andRespond(withException(new ConnectException("down")));
    mvc.perform(get("/api/offers"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").exists())
        .andExpect(jsonPath("$[0].products").isEmpty());
  }

  @Test
  void duplicateCodeIs409() throws Exception {
    String code = "DUP-" + UUID.randomUUID().toString().substring(0, 8);
    UUID productId = UUID.randomUUID();
    stubLookup(productId);
    mvc.perform(post("/api/offers")
            .contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsBytes(Datasets.offer(code, "First", "ACTIVE", null, null, List.of(productId)))))
        .andExpect(status().isCreated());
    DownstreamServers.reset();
    stubLookup(productId);
    mvc.perform(post("/api/offers")
            .contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsBytes(Datasets.offer(code, "Second", "ACTIVE", null, null, List.of(productId)))))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.message").value("An offer with that code already exists."));
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
