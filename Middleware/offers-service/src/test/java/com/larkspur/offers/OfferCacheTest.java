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
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@CommerceServiceTest
class OfferCacheTest {
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
  void createReadsCatalogOnceAndTheNextReadUsesRedis() throws Exception {
    UUID productId = UUID.randomUUID();
    String code = "ONE-" + UUID.randomUUID().toString().substring(0, 8);
    MockRestServiceServer catalog = DownstreamServers.server("catalogRestClient");
    catalog.expect(requestTo(org.hamcrest.Matchers.containsString("/api/products/lookup")))
        .andRespond(withSuccess(productJson(productId, "HED-ONE", "Carafe"), MediaType.APPLICATION_JSON));

    MvcResult created = mvc.perform(post("/api/offers")
            .contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsBytes(Datasets.offer(code, "Morning", "ACTIVE", null, null, List.of(productId)))))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.products[0].sku").value("HED-ONE"))
        .andReturn();
    String id = json.readTree(created.getResponse().getContentAsString()).get("id").asText();
    catalog.verify();

    Long ttl = redisTemplate.getExpire("catalog:product:" + productId);
    org.assertj.core.api.Assertions.assertThat(ttl).isBetween(1L, 60L);

    mvc.perform(get("/api/offers/" + id))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.products[0].id").value(productId.toString()));
    catalog.verify();
  }

  private static String productJson(UUID id, String sku, String name) {
    return "[{\"id\":\"" + id + "\",\"sku\":\"" + sku + "\",\"name\":\"" + name + "\",\"category\":\"Home\",\"unitPrice\":48.00}]";
  }
}
