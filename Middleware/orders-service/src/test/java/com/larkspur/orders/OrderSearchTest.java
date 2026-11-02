package com.larkspur.orders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.larkspur.testing.CommerceServiceTest;
import com.larkspur.testing.ShopperTokens;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

@CommerceServiceTest
class OrderSearchTest {
  @Autowired MockMvc mvc;
  @Autowired OrderRepository orders;

  @Test
  void olderMatchIsReturnedWhenTheUnfilteredLatest100OmitsIt() throws Exception {
    orders.save(order("Older Match", Instant.parse("2020-06-15T12:00:00Z")));
    for (int index = 0; index < 100; index++) {
      orders.save(order("Newer " + index, Instant.now()));
    }

    mvc.perform(get("/api/orders").header("Authorization", ShopperTokens.admin()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[?(@.customerName == 'Older Match')]").isEmpty());

    mvc.perform(get("/api/orders").header("Authorization", ShopperTokens.admin()).param("customer", "older match").param("from", "2020-06-15").param("to", "2020-06-15"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].customerName").value("Older Match"))
        .andExpect(jsonPath("$.length()").value(1));
  }

  @Test
  void unknownDateIs400() throws Exception {
    mvc.perform(get("/api/orders").header("Authorization", ShopperTokens.admin()).param("from", "15-06-2020"))
        .andExpect(status().isBadRequest());
  }

  private static CustomerOrder order(String customerName, Instant placedAt) {
    CustomerOrder order = new CustomerOrder();
    order.orderNumber = "ORD-" + UUID.randomUUID().toString().substring(0, 8);
    order.customerId = UUID.randomUUID();
    order.customerName = customerName;
    order.customerEmail = UUID.randomUUID() + "@example.test";
    order.offerId = UUID.randomUUID();
    order.offerName = "Weekend Set";
    order.totalAmount = new BigDecimal("139.00");
    order.status = OrderStatus.PLACED;
    order.lines = new ArrayList<>();
    order.placedAt = placedAt;
    order.createdAt = placedAt;
    order.updatedAt = placedAt;
    return order;
  }
}
