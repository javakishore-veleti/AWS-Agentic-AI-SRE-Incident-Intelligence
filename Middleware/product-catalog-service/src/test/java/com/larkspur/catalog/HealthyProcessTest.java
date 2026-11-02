package com.larkspur.catalog;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.larkspur.testing.CommerceServiceTest;
import com.larkspur.testing.ShopperTokens;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@CommerceServiceTest
class HealthyProcessTest {
  private static final String SOURCE = "inc-003-fault";

  @Autowired MockMvc mvc;
  @Autowired ConfigurableEnvironment environment;

  @Test
  void apiFailsWhileHealthStaysUpThenTheApiRecovers() throws Exception {
    environment.getPropertySources().addFirst(new MapPropertySource(SOURCE, Map.of("fault.fail-requests", "true")));
    try {
      mvc.perform(get("/api/products").header("Authorization", ShopperTokens.admin()))
          .andExpect(status().isInternalServerError())
          .andExpect(jsonPath("$.message").value("Catalog cannot complete this request."));
      mvc.perform(get("/actuator/health"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.status").value("UP"));
      environment.getPropertySources().remove(SOURCE);
      mvc.perform(get("/api/products").header("Authorization", ShopperTokens.admin()))
          .andExpect(status().isOk())
          .andExpect(content().contentTypeCompatibleWith("application/json"));
    } finally {
      environment.getPropertySources().remove(SOURCE);
    }
  }
}
