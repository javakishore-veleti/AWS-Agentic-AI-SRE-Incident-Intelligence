package com.larkspur.catalog;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

enum ProductStatus {
  ACTIVE,
  DISCONTINUED
}

@Entity
@Table(name = "products")
class Product {
  @Id
  UUID id;

  @Column(nullable = false, unique = true, length = 40)
  String sku;

  @Column(nullable = false, length = 140)
  String name;

  @Column(length = 2000)
  String description;

  @Column(nullable = false, length = 80)
  String category;

  @Column(nullable = false, precision = 12, scale = 2)
  BigDecimal unitPrice;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  ProductStatus status;

  @Column(nullable = false)
  Instant createdAt;

  @Column(nullable = false)
  Instant updatedAt;

  @PrePersist
  void onCreate() {
    if (id == null) {
      id = UUID.randomUUID();
    }
    Instant now = Instant.now();
    if (createdAt == null) {
      createdAt = now;
    }
    if (updatedAt == null) {
      updatedAt = now;
    }
  }

  @PreUpdate
  void onUpdate() {
    updatedAt = Instant.now();
  }
}
