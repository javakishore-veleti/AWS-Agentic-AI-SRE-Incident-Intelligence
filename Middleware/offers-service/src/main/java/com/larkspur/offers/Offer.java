package com.larkspur.offers;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
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
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

enum OfferStatus {
  DRAFT,
  ACTIVE,
  EXPIRED
}

@Entity
@Table(name = "offers")
class Offer {
  @Id
  UUID id;

  @Column(nullable = false, unique = true, length = 40)
  String code;

  @Column(nullable = false, length = 140)
  String name;

  @Column(length = 2000)
  String description;

  @Column(nullable = false, precision = 12, scale = 2)
  BigDecimal offerPrice;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  OfferStatus status;

  LocalDate startsOn;

  LocalDate endsOn;

  @ElementCollection
  @CollectionTable(name = "offer_products", joinColumns = @JoinColumn(name = "offer_id"))
  @Column(name = "product_id")
  List<UUID> productIds = new ArrayList<>();

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
