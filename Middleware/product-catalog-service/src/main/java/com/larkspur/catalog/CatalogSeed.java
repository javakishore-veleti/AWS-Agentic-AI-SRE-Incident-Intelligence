package com.larkspur.catalog;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
class CatalogSeed implements CommandLineRunner {
  private final ProductRepository products;
  private final InventoryClient inventory;

  CatalogSeed(ProductRepository products, InventoryClient inventory) {
    this.products = products;
    this.inventory = inventory;
  }

  @Override
  public void run(String... args) {
    if (products.count() > 0) {
      return;
    }
    Instant now = Instant.now();
    save(id(1), "HED-CARAFE", "Stoneware Pour-Over Carafe", "A matte sand carafe for a slow morning.", "Home", "48.00", ProductStatus.ACTIVE, now.minus(10, ChronoUnit.DAYS));
    save(id(2), "HED-RUNNER", "Linen Table Runner", "Stonewashed linen, long enough for six.", "Home", "36.00", ProductStatus.ACTIVE, now.minus(9, ChronoUnit.DAYS));
    save(id(3), "HED-LAMP", "Brass Task Lamp", "A weighted lamp with a warm linen shade.", "Lighting", "128.00", ProductStatus.ACTIVE, now.minus(8, ChronoUnit.DAYS));
    save(id(4), "HED-BOARD", "Oak Serving Board", "Oil-finished oak with a juice groove.", "Kitchen", "54.00", ProductStatus.ACTIVE, now.minus(7, ChronoUnit.DAYS));
    save(id(5), "HED-THROW", "Wool Window Throw", "A dense wool throw in pine and cream.", "Textiles", "96.00", ProductStatus.ACTIVE, now.minus(6, ChronoUnit.DAYS));
    save(id(6), "HED-CUP", "Ceramic Espresso Pair", "Two cups with a speckled clay glaze.", "Kitchen", "72.00", ProductStatus.ACTIVE, now.minus(5, ChronoUnit.DAYS));
    save(id(7), "HED-NOTE", "Leather Pocket Notebook", "A refillable notebook that lies flat.", "Stationery", "28.00", ProductStatus.ACTIVE, now.minus(4, ChronoUnit.DAYS));
    save(id(8), "HED-GLASS", "Ribbed Glass Carafe", "Pressed glass for water at the table.", "Kitchen", "42.00", ProductStatus.ACTIVE, now.minus(3, ChronoUnit.DAYS));
    save(id(9), "HED-CANDLE", "Cedarwood Candle Duo", "Two short candles poured in ceramic.", "Home", "32.00", ProductStatus.ACTIVE, now.minus(2, ChronoUnit.DAYS));
    save(id(10), "HED-APRON", "Cotton Waist Apron", "A cross-back apron in washed clay cotton.", "Kitchen", "38.00", ProductStatus.DISCONTINUED, now.minus(1, ChronoUnit.DAYS));
  }

  private void save(UUID id, String sku, String name, String description, String category, String price, ProductStatus status, Instant createdAt) {
    Product product = new Product();
    product.id = id;
    product.sku = sku;
    product.name = name;
    product.description = description;
    product.category = category;
    product.unitPrice = new BigDecimal(price);
    product.status = status;
    product.createdAt = createdAt;
    product.updatedAt = createdAt;
    products.save(product);
    inventory.sync(sku, name);
  }

  static UUID id(int sequence) {
    return UUID.fromString("10000000-0000-4000-8000-0000000000" + String.format("%02d", sequence));
  }
}
