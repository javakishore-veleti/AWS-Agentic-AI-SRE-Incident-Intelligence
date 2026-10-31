package com.larkspur.inventory;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
class InventorySeed implements CommandLineRunner {
  private final StockRepository stock;

  InventorySeed(StockRepository stock) {
    this.stock = stock;
  }

  @Override
  public void run(String... args) {
    if (stock.count() > 0) {
      return;
    }
    Instant now = Instant.now();
    save("HED-CARAFE", "Stoneware Pour-Over Carafe", now.minus(10, ChronoUnit.DAYS));
    save("HED-RUNNER", "Linen Table Runner", now.minus(9, ChronoUnit.DAYS));
    save("HED-LAMP", "Brass Task Lamp", now.minus(8, ChronoUnit.DAYS));
    save("HED-BOARD", "Oak Serving Board", now.minus(7, ChronoUnit.DAYS));
    save("HED-THROW", "Wool Window Throw", now.minus(6, ChronoUnit.DAYS));
    save("HED-CUP", "Ceramic Espresso Pair", now.minus(5, ChronoUnit.DAYS));
    save("HED-NOTE", "Leather Pocket Notebook", now.minus(4, ChronoUnit.DAYS));
    save("HED-GLASS", "Ribbed Glass Carafe", now.minus(3, ChronoUnit.DAYS));
    save("HED-CANDLE", "Cedarwood Candle Duo", now.minus(2, ChronoUnit.DAYS));
    save("HED-APRON", "Cotton Waist Apron", now.minus(1, ChronoUnit.DAYS));
  }

  private void save(String sku, String name, Instant updatedAt) {
    StockItem item = new StockItem();
    item.sku = sku;
    item.productName = name;
    item.availableQty = 100;
    item.reservedQty = 0;
    item.updatedAt = updatedAt;
    stock.save(item);
  }
}
