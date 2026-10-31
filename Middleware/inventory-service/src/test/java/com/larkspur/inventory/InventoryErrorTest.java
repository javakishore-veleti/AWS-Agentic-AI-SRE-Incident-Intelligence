package com.larkspur.inventory;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.larkspur.testing.CommerceServiceTest;
import com.larkspur.testing.Datasets;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@CommerceServiceTest
class InventoryErrorTest {
  @Autowired MockMvc mvc;
  @Autowired ObjectMapper json;

  @Test
  void missingSkuIs409() throws Exception {
    String sku = ("MISSING-" + UUID.randomUUID().toString().substring(0, 8)).toUpperCase();
    mvc.perform(post("/api/reservations")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"orderRef\":\"HD-2\",\"lines\":[{\"sku\":\"" + sku + "\",\"qty\":1}]}"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.message").value("No stock record for " + sku + "."));
  }

  @Test
  void insufficientStockIs409() throws Exception {
    String sku = ("LOW-" + UUID.randomUUID().toString().substring(0, 8)).toUpperCase();
    mvc.perform(put("/api/stock/" + sku)
            .contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsBytes(Datasets.stock("Linen Runner", 1))))
        .andExpect(status().isOk());
    mvc.perform(post("/api/reservations")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"orderRef\":\"HD-3\",\"lines\":[{\"sku\":\"" + sku + "\",\"qty\":2}]}"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.message").value("Linen Runner does not have enough stock."));
  }

  @Test
  void blankProductNameIs400() throws Exception {
    mvc.perform(put("/api/stock/NAME-" + UUID.randomUUID().toString().substring(0, 8))
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"productName\":\" \"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("productName must not be blank"));
  }

  @Test
  void blankSkuIs400() throws Exception {
    mvc.perform(post("/api/reservations")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"orderRef\":\"HD-4\",\"lines\":[{\"sku\":\" \",\"qty\":1}]}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("lines[0].sku must not be blank"));
  }

  @Test
  void quantityBelowOneIs400() throws Exception {
    mvc.perform(post("/api/reservations")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"orderRef\":\"HD-5\",\"lines\":[{\"sku\":\"CUP\",\"qty\":0}]}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("lines[0].qty must be greater than or equal to 1"));
  }

  @Test
  void emptyLinesAre400() throws Exception {
    mvc.perform(post("/api/reservations")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"orderRef\":\"HD-6\",\"lines\":[]}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("lines must not be empty"));
  }
}
