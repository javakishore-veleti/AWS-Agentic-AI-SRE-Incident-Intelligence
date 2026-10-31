package com.larkspur.orders;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

enum OrderStatus {
  PLACED,
  CONFIRMED,
  FULFILLED,
  CANCELLED
}

@Embeddable
class OrderLine {
  UUID productId;
  @Column(length = 40)
  String sku;
  @Column(length = 140)
  String productName;
  int quantity;
  @Column(precision = 12, scale = 2)
  BigDecimal unitPrice;
}

@Entity
@Table(name = "customer_orders")
class CustomerOrder {
  @Id
  UUID id;

  @Column(nullable = false, unique = true, length = 40)
  String orderNumber;

  @Column(nullable = false)
  UUID customerId;

  @Column(nullable = false, length = 140)
  String customerName;

  @Column(nullable = false, length = 180)
  String customerEmail;

  @Column(nullable = false)
  UUID offerId;

  @Column(nullable = false, length = 140)
  String offerName;

  @Column(nullable = false, precision = 12, scale = 2)
  BigDecimal totalAmount;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  OrderStatus status;

  @Column(length = 2000)
  String note;

  @ElementCollection
  @CollectionTable(name = "order_lines", joinColumns = @JoinColumn(name = "order_id"))
  List<OrderLine> lines = new ArrayList<>();

  @Column(nullable = false)
  Instant placedAt;

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
    if (placedAt == null) {
      placedAt = now;
    }
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
