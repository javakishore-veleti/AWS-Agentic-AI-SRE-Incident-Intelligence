package com.larkspur.catalog;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
class InventoryClient {
  private static final Logger log = LoggerFactory.getLogger(InventoryClient.class);
  private final RestClient inventory;

  InventoryClient(RestClient inventoryClient) {
    this.inventory = inventoryClient;
  }

  void sync(String sku, String productName) {
    try {
      inventory.put()
          .uri("/api/stock/{sku}", sku)
          .body(new StockSync(productName))
          .retrieve()
          .toBodilessEntity();
    } catch (RuntimeException ex) {
      log.warn("Inventory was not updated for {}: {}", sku, ex.getMessage());
    }
  }

  record StockSync(String productName) {}
}
