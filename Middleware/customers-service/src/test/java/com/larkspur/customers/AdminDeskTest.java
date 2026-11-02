package com.larkspur.customers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.larkspur.testing.CommerceServiceTest;
import com.larkspur.testing.Datasets;
import com.larkspur.testing.ShopperTokens;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@CommerceServiceTest
class AdminDeskTest {
  @Autowired MockMvc mvc;
  @Autowired ObjectMapper json;
  @Autowired CustomerRepository customers;

  @Test
  void missingTokenIs401() throws Exception {
    mvc.perform(get("/api/customers"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.message").value("Sign in to continue."));
  }

  @Test
  void customerTokenIs403() throws Exception {
    mvc.perform(get("/api/customers").header("Authorization", ShopperTokens.bearer(UUID.randomUUID(), "Ada", "ada@example.test")))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.message").value("An administrator must approve this request."));
  }

  @Test
  void signupRoleIsCustomer() throws Exception {
    String email = "role-" + UUID.randomUUID() + "@example.test";
    mvc.perform(post("/api/auth/signup")
            .contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsBytes(Datasets.signup("Ada Lovelace", email, "password12"))))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.customer.role").value("CUSTOMER"));
  }

  @Test
  void seedCreatesAnAdmin() {
    Customer desk = customers.findByEmail("desk@heddle.example").orElseThrow();
    assertThat(desk.role).isEqualTo(AccountRole.ADMIN);
  }
}
