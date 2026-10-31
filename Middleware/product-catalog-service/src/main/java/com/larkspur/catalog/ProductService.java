package com.larkspur.catalog;

import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
class ProductService {
  private final ProductRepository products;
  private final InventoryClient inventory;

  ProductService(ProductRepository products, InventoryClient inventory) {
    this.products = products;
    this.inventory = inventory;
  }

  List<ProductResponse> latest(String name, String sku, String category, String status) {
    return products.search(text(name), text(sku), text(category), status(status), PageRequest.of(0, 100)).stream()
        .map(ProductService::toResponse)
        .toList();
  }

  private static String text(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return value.trim();
  }

  private static ProductStatus status(String value) {
    String text = text(value);
    if (text == null) {
      return null;
    }
    try {
      return ProductStatus.valueOf(text);
    } catch (IllegalArgumentException exception) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown status.");
    }
  }

  List<ProductResponse> findAll(List<UUID> ids) {
    return products.findByIdIn(ids).stream().map(ProductService::toResponse).toList();
  }

  ProductResponse get(UUID id) {
    return toResponse(load(id));
  }

  @Transactional
  ProductResponse create(ProductRequest request) {
    Product product = new Product();
    apply(product, request);
    Product saved = products.save(product);
    inventory.sync(saved.sku, saved.name);
    return toResponse(saved);
  }

  @Transactional
  ProductResponse update(UUID id, ProductRequest request) {
    Product product = load(id);
    apply(product, request);
    Product saved = products.save(product);
    inventory.sync(saved.sku, saved.name);
    return toResponse(saved);
  }

  @Transactional
  void delete(UUID id) {
    if (!products.existsById(id)) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found.");
    }
    products.deleteById(id);
  }

  private Product load(UUID id) {
    return products.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found."));
  }

  private static void apply(Product product, ProductRequest request) {
    product.sku = request.sku().trim().toUpperCase();
    product.name = request.name().trim();
    product.description = request.description() == null ? "" : request.description().trim();
    product.category = request.category().trim();
    product.unitPrice = request.unitPrice();
    product.status = request.status();
  }

  static ProductResponse toResponse(Product product) {
    return new ProductResponse(
        product.id,
        product.sku,
        product.name,
        product.description,
        product.category,
        product.unitPrice,
        product.status,
        product.createdAt,
        product.updatedAt);
  }
}
