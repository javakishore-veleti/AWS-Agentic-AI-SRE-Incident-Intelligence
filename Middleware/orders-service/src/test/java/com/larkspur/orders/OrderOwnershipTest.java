package com.larkspur.orders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.larkspur.testing.CommerceServiceTest;
import com.larkspur.testing.ShopperTokens;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@CommerceServiceTest
class OrderOwnershipTest {
  @Autowired MockMvc mvc;
  @Autowired OrderRepository orders;

  @Test
  void anotherCustomerCannotReadTheOrder() throws Exception {
    UUID owner = UUID.randomUUID();
    UUID other = UUID.randomUUID();
    CustomerOrder order = saved(owner);

    mvc.perform(get("/api/orders/" + order.id))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.message").value("Sign in to continue."));

    mvc.perform(get("/api/orders/" + order.id).header("Authorization", ShopperTokens.bearer(other, "Other", "other@example.test")))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").value("Order not found."));

    mvc.perform(get("/api/orders/" + order.id).header("Authorization", ShopperTokens.bearer(owner, "Ada", "ada@example.test")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(order.id.toString()));

    mvc.perform(get("/api/orders/" + order.id).header("Authorization", ShopperTokens.admin()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(order.id.toString()));

    mvc.perform(put("/api/orders/" + order.id)
            .header("Authorization", ShopperTokens.bearer(other, "Other", "other@example.test"))
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"status\":\"FULFILLED\",\"note\":\"no\"}"))
        .andExpect(status().isForbidden());
  }

  private CustomerOrder saved(UUID customerId) {
    CustomerOrder order = new CustomerOrder();
    order.orderNumber = "HD-OWN-" + UUID.randomUUID().toString().substring(0, 8);
    order.customerId = customerId;
    order.customerName = "Ada";
    order.customerEmail = "ada@example.test";
    order.offerId = UUID.randomUUID();
    order.offerName = "Morning";
    order.totalAmount = new BigDecimal("139.00");
    order.status = OrderStatus.PLACED;
    order.placedAt = Instant.parse("2026-11-01T12:00:00Z");
    order.updatedAt = order.placedAt;
    return orders.save(order);
  }
}
