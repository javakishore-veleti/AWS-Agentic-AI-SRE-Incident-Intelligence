package com.larkspur.orders;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

@Entity
@Table(name = "order_cancellations")
class OrderCancellation {
  @Id
  UUID id;

  @Column(nullable = false, unique = true)
  UUID orderId;

  @Column(nullable = false, length = 40)
  String orderNumber;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  OrderStatus previousStatus;

  @Column(nullable = false)
  UUID actorId;

  @Column(nullable = false, length = 180)
  String actorEmail;

  @Column(nullable = false)
  Instant cancelledAt;

  @PrePersist
  void onCreate() {
    if (id == null) {
      id = UUID.randomUUID();
    }
    if (cancelledAt == null) {
      cancelledAt = Instant.now();
    }
  }
}

interface OrderCancellationRepository extends JpaRepository<OrderCancellation, UUID> {
  Optional<OrderCancellation> findByOrderId(UUID orderId);
}
