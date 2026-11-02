package com.larkspur.orders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.larkspur.testing.CommerceServiceTest;
import com.larkspur.testing.ShopperTokens;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

@CommerceServiceTest
class AdminDeskTest {
  @Autowired MockMvc mvc;

  @Test
  void missingTokenIs401() throws Exception {
    mvc.perform(get("/api/orders"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.message").value("Sign in to continue."));
  }

  @Test
  void customerTokenIs403() throws Exception {
    mvc.perform(get("/api/orders").header("Authorization", ShopperTokens.bearer(UUID.randomUUID(), "Ada", "ada@example.test")))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.message").value("An administrator must approve this request."));
  }

  @Test
  void adminCanList() throws Exception {
    mvc.perform(get("/api/orders").header("Authorization", ShopperTokens.admin()))
        .andExpect(status().isOk());
  }
}
