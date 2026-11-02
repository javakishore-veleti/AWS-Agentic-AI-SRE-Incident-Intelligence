package com.larkspur.inventory;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

record StockResponse(String sku, String productName, int availableQty, int reservedQty, Instant updatedAt) {}

record StockUpsert(@NotBlank String productName, Integer availableQty) {}

record ReserveLine(@NotBlank String sku, @Min(1) int qty) {}

record ReserveRequest(@NotBlank String orderRef, @NotEmpty List<@Valid ReserveLine> lines) {}

record ReleaseRequest(@NotBlank String orderRef) {}

@Entity
@Table(name = "reservations")
class HeldReservation {
  @Id
  @Column(length = 40)
  String orderRef;

  @Column(nullable = false, length = 1000)
  String fingerprint;

  @Column(nullable = false)
  Instant createdAt;
}

interface ReservationRepository extends JpaRepository<HeldReservation, String> {}

@RestController
@RequestMapping("/api")
class InventoryController {
  private final InventoryService inventory;

  InventoryController(InventoryService inventory) {
    this.inventory = inventory;
  }

  @GetMapping("/stock")
  List<StockResponse> latest() {
    return inventory.latest();
  }

  @PutMapping("/stock/{sku}")
  StockResponse upsert(@PathVariable String sku, @Valid @RequestBody StockUpsert request) {
    return inventory.upsert(sku, request);
  }

  @PostMapping("/reservations")
  ResponseEntity<Void> reserve(@Valid @RequestBody ReserveRequest request) {
    inventory.reserve(request);
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/reservations/release")
  ResponseEntity<Void> release(@Valid @RequestBody ReleaseRequest request) {
    inventory.release(request.orderRef());
    return ResponseEntity.noContent().build();
  }
}

@Service
class InventoryService {
  private final StockRepository stock;
  private final ReservationRepository reservations;
  private final long delayMs;

  InventoryService(
      StockRepository stock,
      ReservationRepository reservations,
      @Value("${fault.response-delay-ms:0}") long delayMs) {
    this.stock = stock;
    this.reservations = reservations;
    this.delayMs = delayMs;
  }

  List<StockResponse> latest() {
    return stock.findAll().stream()
        .sorted((left, right) -> right.updatedAt.compareTo(left.updatedAt))
        .limit(100)
        .map(InventoryService::toResponse)
        .toList();
  }

  @Transactional
  StockResponse upsert(String sku, StockUpsert request) {
    String key = sku.trim().toUpperCase();
    StockItem item = stock.findById(key).orElseGet(() -> {
      StockItem created = new StockItem();
      created.sku = key;
      created.availableQty = request.availableQty() == null ? 100 : request.availableQty();
      created.reservedQty = 0;
      return created;
    });
    item.productName = request.productName().trim();
    item.updatedAt = Instant.now();
    return toResponse(stock.save(item));
  }

  @Transactional
  void reserve(ReserveRequest request) {
    pause();
    String fingerprint = fingerprint(request);
    Optional<HeldReservation> held = reservations.findById(request.orderRef());
    if (held.isPresent()) {
      if (!held.get().fingerprint.equals(fingerprint)) {
        throw new ResponseStatusException(HttpStatus.CONFLICT, "This order already reserved different stock.");
      }
      return;
    }
    for (ReserveLine line : request.lines()) {
      String sku = line.sku().trim().toUpperCase();
      StockItem item = stock.findForUpdate(sku)
          .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "No stock record for " + sku + "."));
      if (item.availableQty < line.qty()) {
        throw new ResponseStatusException(HttpStatus.CONFLICT, item.productName + " does not have enough stock.");
      }
      item.availableQty -= line.qty();
      item.reservedQty += line.qty();
      item.updatedAt = Instant.now();
    }
    HeldReservation created = new HeldReservation();
    created.orderRef = request.orderRef();
    created.fingerprint = fingerprint;
    created.createdAt = Instant.now();
    reservations.save(created);
  }

  @Transactional
  void release(String orderRef) {
    Optional<HeldReservation> held = reservations.findById(orderRef);
    if (held.isEmpty()) {
      return;
    }
    for (String part : held.get().fingerprint.split("\\|")) {
      int colon = part.lastIndexOf(':');
      if (colon <= 0) {
        continue;
      }
      String sku = part.substring(0, colon);
      int qty = Integer.parseInt(part.substring(colon + 1));
      StockItem item = stock.findForUpdate(sku)
          .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "No stock record for " + sku + "."));
      item.availableQty += qty;
      item.reservedQty = Math.max(0, item.reservedQty - qty);
      item.updatedAt = Instant.now();
    }
    reservations.delete(held.get());
  }

  private static String fingerprint(ReserveRequest request) {
    return request.lines().stream()
        .map(line -> line.sku().trim().toUpperCase() + ":" + line.qty())
        .sorted()
        .reduce((left, right) -> left + "|" + right)
        .orElse("");
  }

  private void pause() {
    if (delayMs <= 0) {
      return;
    }
    try {
      Thread.sleep(delayMs);
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
    }
  }

  private static StockResponse toResponse(StockItem item) {
    return new StockResponse(item.sku, item.productName, item.availableQty, item.reservedQty, item.updatedAt);
  }
}
