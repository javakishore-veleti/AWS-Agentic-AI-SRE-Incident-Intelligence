package com.larkspur.catalog;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

record ProductRequest(
    @NotBlank @Size(max = 40) String sku,
    @NotBlank @Size(max = 140) String name,
    @Size(max = 2000) String description,
    @NotBlank @Size(max = 80) String category,
    @NotNull @DecimalMin(value = "0.0", inclusive = true) BigDecimal unitPrice,
    @NotNull ProductStatus status) {}

record ProductResponse(
    UUID id,
    String sku,
    String name,
    String description,
    String category,
    BigDecimal unitPrice,
    ProductStatus status,
    Instant createdAt,
    Instant updatedAt) {}

@RestController
@RequestMapping("/api/products")
class ProductController {
  private final ProductService products;

  ProductController(ProductService products) {
    this.products = products;
  }

  @GetMapping
  List<ProductResponse> latest(
      @RequestParam(required = false) String name,
      @RequestParam(required = false) String sku,
      @RequestParam(required = false) String category,
      @RequestParam(required = false) String status) {
    return products.latest(name, sku, category, status);
  }

  @GetMapping("/lookup")
  List<ProductResponse> lookup(@RequestParam(name = "ids", defaultValue = "") String ids) {
    if (ids.isBlank()) {
      return List.of();
    }
    List<UUID> parsed = Arrays.stream(ids.split(","))
        .map(String::trim)
        .filter(value -> !value.isEmpty())
        .map(UUID::fromString)
        .toList();
    return products.findAll(parsed);
  }

  @GetMapping("/{id}")
  ProductResponse one(@PathVariable UUID id) {
    return products.get(id);
  }

  @PostMapping
  ResponseEntity<ProductResponse> create(@Valid @RequestBody ProductRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(products.create(request));
  }

  @PutMapping("/{id}")
  ProductResponse update(@PathVariable UUID id, @Valid @RequestBody ProductRequest request) {
    return products.update(id, request);
  }

  @DeleteMapping("/{id}")
  ResponseEntity<Void> delete(@PathVariable UUID id) {
    products.delete(id);
    return ResponseEntity.noContent().build();
  }
}

class MissingProductException extends ResponseStatusException {
  MissingProductException() {
    super(HttpStatus.NOT_FOUND, "Product not found.");
  }
}
