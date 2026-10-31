package com.larkspur.offers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.larkspur.testing.CommerceServiceTest;
import com.larkspur.testing.EmbeddedRedis;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@CommerceServiceTest
class OfferSearchTest {
  @DynamicPropertySource
  static void redis(DynamicPropertyRegistry registry) {
    EmbeddedRedis.start();
    registry.add("spring.data.redis.host", () -> "127.0.0.1");
    registry.add("spring.data.redis.port", EmbeddedRedis::port);
  }

  @Autowired MockMvc mvc;
  @Autowired OfferRepository offers;

  @Test
  void olderMatchIsReturnedWhenTheUnfilteredLatest100OmitsIt() throws Exception {
    offers.save(offer("OLD-MATCH", "Older Match", Instant.parse("2020-01-01T00:00:00Z")));
    for (int index = 0; index < 100; index++) {
      offers.save(offer("NEW-" + index, "Newer " + index, Instant.now()));
    }

    mvc.perform(get("/api/offers"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[?(@.name == 'Older Match')]").isEmpty());

    mvc.perform(get("/api/offers").param("code", "old-match"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].name").value("Older Match"))
        .andExpect(jsonPath("$.length()").value(1));
  }

  private static Offer offer(String code, String name, Instant createdAt) {
    Offer offer = new Offer();
    offer.code = (code + "-" + UUID.randomUUID().toString().substring(0, 8)).toUpperCase();
    offer.name = name;
    offer.description = "A test offer.";
    offer.offerPrice = new BigDecimal("139.00");
    offer.status = OfferStatus.ACTIVE;
    offer.productIds = new ArrayList<>();
    offer.createdAt = createdAt;
    offer.updatedAt = createdAt;
    return offer;
  }
}
