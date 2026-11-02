package com.larkspur.customers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.larkspur.testing.CommerceServiceTest;
import com.larkspur.testing.ShopperTokens;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

@CommerceServiceTest
class CustomerSearchTest {
  @Autowired MockMvc mvc;
  @Autowired CustomerRepository customers;

  @Test
  void olderMatchIsReturnedWhenTheUnfilteredLatest100OmitsIt() throws Exception {
    customers.save(customer("Older Match", "older-match@example.test", Instant.parse("2020-01-01T00:00:00Z")));
    for (int index = 0; index < 100; index++) {
      customers.save(customer("Newer " + index, "newer-" + index + "-" + UUID.randomUUID() + "@example.test", Instant.now()));
    }

    mvc.perform(get("/api/customers").header("Authorization", ShopperTokens.admin()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[?(@.fullName == 'Older Match')]").isEmpty());

    mvc.perform(get("/api/customers").header("Authorization", ShopperTokens.admin()).param("email", "older-match@"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].fullName").value("Older Match"))
        .andExpect(jsonPath("$.length()").value(1));
  }

  private static Customer customer(String name, String email, Instant createdAt) {
    Customer customer = new Customer();
    customer.fullName = name;
    customer.email = email;
    customer.passwordHash = "test-hash";
    customer.phone = "555-0100";
    customer.status = CustomerStatus.ACTIVE;
    customer.createdAt = createdAt;
    customer.updatedAt = createdAt;
    return customer;
  }
}
