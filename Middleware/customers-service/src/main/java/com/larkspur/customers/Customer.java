package com.larkspur.customers;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

enum CustomerStatus {
  ACTIVE,
  SUSPENDED
}

enum AccountRole {
  CUSTOMER,
  ADMIN
}

@Entity
@Table(name = "customers")
class Customer {
  @Id
  UUID id;

  @Column(nullable = false, length = 140)
  String fullName;

  @Column(nullable = false, unique = true, length = 180)
  String email;

  @Column(nullable = false, length = 100)
  String passwordHash;

  @Column(length = 40)
  String phone;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  CustomerStatus status;

  @Enumerated(EnumType.STRING)
  @Column(length = 20)
  AccountRole role;

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
    if (role == null) {
      role = AccountRole.CUSTOMER;
    }
  }

  @PreUpdate
  void onUpdate() {
    updatedAt = Instant.now();
  }
}
