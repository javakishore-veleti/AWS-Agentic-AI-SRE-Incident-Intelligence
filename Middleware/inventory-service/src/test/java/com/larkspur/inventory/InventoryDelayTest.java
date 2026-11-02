package com.larkspur.inventory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.larkspur.testing.CommerceServiceTest;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@CommerceServiceTest
class InventoryDelayTest {
  private static final String SOURCE = "inc-002-delay";

  @Autowired MockMvc mvc;
  @Autowired ConfigurableEnvironment environment;

  @Test
  void reservationWaitsForTheDelayAndTheNextReservationDoesNot() throws Exception {
    environment.getPropertySources().addFirst(new MapPropertySource(SOURCE, Map.of("fault.response-delay-ms", "300")));
    try {
      long slow = reserve();
      environment.getPropertySources().remove(SOURCE);
      long fast = reserve();
      assertThat(slow).isGreaterThanOrEqualTo(250);
      assertThat(fast).isLessThan(150);
    } finally {
      environment.getPropertySources().remove(SOURCE);
    }
  }

  private long reserve() throws Exception {
    String sku = "DLY-" + UUID.randomUUID().toString().substring(0, 8);
    mvc.perform(put("/api/stock/" + sku)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"productName\":\"Carafe\",\"availableQty\":5}"))
        .andExpect(status().isOk());
    long started = System.nanoTime();
    mvc.perform(post("/api/reservations")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"orderRef\":\"" + UUID.randomUUID() + "\",\"lines\":[{\"sku\":\"" + sku + "\",\"qty\":1}]}"))
        .andExpect(status().isNoContent());
    return (System.nanoTime() - started) / 1_000_000L;
  }
}
