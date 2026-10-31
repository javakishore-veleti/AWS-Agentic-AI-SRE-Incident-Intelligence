package com.larkspur.testing;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class Datasets {
  private Datasets() {
  }

  public record ShopperSignup(String fullName, String email, String phone, String password) {
  }

  public record CustomerDraft(String fullName, String email, String phone, String password, String status) {
  }

  public record ProductDraft(
      String sku, String name, String description, String category, BigDecimal unitPrice, String status) {
  }

  public record OfferDraft(
      String code,
      String name,
      String description,
      BigDecimal offerPrice,
      String status,
      LocalDate startsOn,
      LocalDate endsOn,
      List<UUID> productIds) {
  }

  public record StockDraft(String productName, Integer availableQty) {
  }

  public static ShopperSignup signup(String fullName, String email, String password) {
    return new ShopperSignup(fullName, email, "555-0100", password);
  }

  public static CustomerDraft customer(String fullName, String email, String password, String status) {
    return new CustomerDraft(fullName, email, "555-0100", password, status);
  }

  public static ProductDraft product(String sku, String name) {
    return new ProductDraft(sku, name, "A test product.", "Home", new BigDecimal("48.00"), "ACTIVE");
  }

  public static OfferDraft offer(String code, String name, String status, LocalDate startsOn, LocalDate endsOn, List<UUID> productIds) {
    return new OfferDraft(code, name, "A test offer.", new BigDecimal("139.00"), status, startsOn, endsOn, productIds);
  }

  public static StockDraft stock(String productName, Integer availableQty) {
    return new StockDraft(productName, availableQty);
  }
}
