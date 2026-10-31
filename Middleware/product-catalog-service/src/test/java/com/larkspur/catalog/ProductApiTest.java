package com.larkspur.catalog;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.larkspur.testing.CommerceServiceTest;
import com.larkspur.testing.Datasets;
import com.larkspur.testing.DownstreamServers;
import java.net.ConnectException;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@CommerceServiceTest
class ProductApiTest {
  @Autowired MockMvc mvc;
  @Autowired ObjectMapper json;

  @BeforeEach
  void resetDownstream() {
    DownstreamServers.reset();
  }

  @Test
  void createReadUpdateAndDelete() throws Exception {
    String sku = ("SKU-" + UUID.randomUUID()).toUpperCase();
    MockRestServiceServer inventory = DownstreamServers.server("inventoryRestClient");
    inventory.expect(requestTo("http://inventory.test/api/stock/" + sku))
        .andExpect(method(HttpMethod.PUT))
        .andRespond(withSuccess());
    MvcResult created = mvc.perform(post("/api/products")
            .contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsBytes(Datasets.product(sku, "Stoneware Carafe"))))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.sku").value(sku))
        .andReturn();
    inventory.verify();
    String id = json.readTree(created.getResponse().getContentAsString()).get("id").asText();

    mvc.perform(get("/api/products/" + id))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("Stoneware Carafe"));

    inventory.reset();
    inventory.expect(requestTo("http://inventory.test/api/stock/" + sku))
        .andExpect(method(HttpMethod.PUT))
        .andRespond(withSuccess());
    mvc.perform(put("/api/products/" + id)
            .contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsBytes(Datasets.product(sku, "Updated Carafe"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("Updated Carafe"));

    mvc.perform(delete("/api/products/" + id))
        .andExpect(status().isNoContent());
    mvc.perform(get("/api/products/" + id))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").value("Product not found."));
  }

  @Test
  void unknownProductIs404() throws Exception {
    mvc.perform(get("/api/products/" + UUID.randomUUID()))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").value("Product not found."));
  }

  @Test
  void duplicateSkuIs409() throws Exception {
    String sku = ("DUP-" + UUID.randomUUID()).toUpperCase();
    MockRestServiceServer inventory = DownstreamServers.server("inventoryRestClient");
    inventory.expect(requestTo("http://inventory.test/api/stock/" + sku)).andRespond(withSuccess());
    mvc.perform(post("/api/products")
            .contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsBytes(Datasets.product(sku, "First"))))
        .andExpect(status().isCreated());
    inventory.reset();
    inventory.expect(requestTo("http://inventory.test/api/stock/" + sku)).andRespond(withSuccess());
    mvc.perform(post("/api/products")
            .contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsBytes(Datasets.product(sku, "Second"))))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.message").value("A product with that SKU already exists."));
  }

  @Test
  void blankSkuIs400() throws Exception {
    mvc.perform(post("/api/products")
            .contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsBytes(Datasets.product("  ", "Carafe"))))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("sku must not be blank"));
  }

  @Test
  void oversizedSkuIs400() throws Exception {
    mvc.perform(post("/api/products")
            .contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsBytes(Datasets.product("S".repeat(41), "Carafe"))))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("sku size must be between 0 and 40"));
  }

  @Test
  void inventoryFailureStillCreatesTheProduct() throws Exception {
    String sku = ("SYNC-" + UUID.randomUUID().toString().substring(0, 8)).toUpperCase();
    DownstreamServers.server("inventoryRestClient")
        .expect(requestTo("http://inventory.test/api/stock/" + sku))
        .andRespond(withException(new ConnectException("down")));
    mvc.perform(post("/api/products")
            .contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsBytes(Datasets.product(sku, "Carafe"))))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.sku").value(sku));
  }
}
