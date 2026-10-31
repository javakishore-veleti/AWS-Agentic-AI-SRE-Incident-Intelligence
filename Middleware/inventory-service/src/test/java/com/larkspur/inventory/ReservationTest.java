package com.larkspur.inventory;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
class ReservationTest {
  @Autowired MockMvc mvc;
  @Autowired ObjectMapper json;

  @Test
  void upsertUppercasesSkuAndDefaultsQuantity() throws Exception {
    String sku = "ab-" + UUID.randomUUID().toString().substring(0, 8);
    mvc.perform(put("/api/stock/" + sku)
            .contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsBytes(Datasets.stock("Carafe", null))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.sku").value(sku.toUpperCase()))
        .andExpect(jsonPath("$.availableQty").value(100))
        .andExpect(jsonPath("$.reservedQty").value(0));
  }

  @Test
  void reservationMovesQuantity() throws Exception {
    String sku = "MOVE-" + UUID.randomUUID().toString().substring(0, 8);
    mvc.perform(put("/api/stock/" + sku)
            .contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsBytes(Datasets.stock("Carafe", 5))))
        .andExpect(status().isOk());
    mvc.perform(post("/api/reservations")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"orderRef\":\"HD-1\",\"lines\":[{\"sku\":\"" + sku.toLowerCase() + "\",\"qty\":2}]}"))
        .andExpect(status().isNoContent());
    mvc.perform(get("/api/stock"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[?(@.sku == '" + sku.toUpperCase() + "')].availableQty").value(org.hamcrest.Matchers.hasItem(3)))
        .andExpect(jsonPath("$[?(@.sku == '" + sku.toUpperCase() + "')].reservedQty").value(org.hamcrest.Matchers.hasItem(2)));
  }
}
