package com.larkspur.catalog;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.larkspur.testing.CommerceServiceTest;
import com.larkspur.testing.ShopperTokens;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

@CommerceServiceTest
class ProductSearchTest {
  @Autowired MockMvc mvc;
  @Autowired ProductRepository products;

  @Test
  void olderMatchIsReturnedWhenTheUnfilteredLatest100OmitsIt() throws Exception {
    products.save(product("OLD-MATCH", "Older Match", Instant.parse("2020-01-01T00:00:00Z")));
    for (int index = 0; index < 100; index++) {
      products.save(product("NEW-" + index, "Newer " + index, Instant.now()));
    }

    mvc.perform(get("/api/products").header("Authorization", ShopperTokens.admin()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[?(@.name == 'Older Match')]").isEmpty());

    mvc.perform(get("/api/products").header("Authorization", ShopperTokens.admin()).param("name", "older match"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].name").value("Older Match"))
        .andExpect(jsonPath("$.length()").value(1));
  }

  @Test
  void unknownStatusIs400() throws Exception {
    mvc.perform(get("/api/products").header("Authorization", ShopperTokens.admin()).param("status", "NOPE"))
        .andExpect(status().isBadRequest());
  }

  private static Product product(String sku, String name, Instant createdAt) {
    Product product = new Product();
    product.sku = sku + "-" + UUID.randomUUID().toString().substring(0, 8);
    product.name = name;
    product.description = "A test product.";
    product.category = "Home";
    product.unitPrice = new BigDecimal("48.00");
    product.status = ProductStatus.ACTIVE;
    product.createdAt = createdAt;
    product.updatedAt = createdAt;
    return product;
  }
}
