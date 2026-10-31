package com.larkspur.inventory;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "stock_items")
class StockItem {
  @Id
  @Column(length = 40)
  String sku;

  @Column(nullable = false, length = 140)
  String productName;

  @Column(nullable = false)
  int availableQty;

  @Column(nullable = false)
  int reservedQty;

  @Column(nullable = false)
  Instant updatedAt;
}
