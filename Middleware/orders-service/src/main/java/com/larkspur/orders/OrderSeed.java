package com.larkspur.orders;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
class OrderSeed implements CommandLineRunner {
  private final OrderRepository orders;

  OrderSeed(OrderRepository orders) {
    this.orders = orders;
  }

  @Override
  public void run(String... args) {
    if (orders.count() > 0) {
      return;
    }
    Instant now = Instant.now();
    save(1, "HD-24021", 1, "Amira Shah", "amira.shah@heddle.example", 1, "First Light", "139.00",
        OrderStatus.PLACED, now.minus(1, ChronoUnit.DAYS), List.of(
            line(1, "HED-CARAFE", "Stoneware Pour-Over Carafe", "48.00"),
            line(6, "HED-CUP", "Ceramic Espresso Pair", "72.00"),
            line(2, "HED-RUNNER", "Linen Table Runner", "36.00")));
    save(2, "HD-24022", 2, "Jonah Ellis", "jonah.ellis@heddle.example", 2, "Desk Hours", "142.00",
        OrderStatus.CONFIRMED, now.minus(3, ChronoUnit.DAYS), List.of(
            line(3, "HED-LAMP", "Brass Task Lamp", "128.00"),
            line(7, "HED-NOTE", "Leather Pocket Notebook", "28.00")));
    save(3, "HD-24023", 3, "Priya Raman", "priya.raman@heddle.example", 3, "Sunday Hosting", "132.00",
        OrderStatus.FULFILLED, now.minus(8, ChronoUnit.DAYS), List.of(
            line(4, "HED-BOARD", "Oak Serving Board", "54.00"),
            line(5, "HED-THROW", "Wool Window Throw", "96.00")));
    save(4, "HD-24024", 4, "Luis Ortega", "luis.ortega@heddle.example", 4, "Kitchen Pair", "99.00",
        OrderStatus.CANCELLED, now.minus(12, ChronoUnit.DAYS), List.of(
            line(6, "HED-CUP", "Ceramic Espresso Pair", "72.00"),
            line(8, "HED-GLASS", "Ribbed Glass Carafe", "42.00")));
    save(5, "HD-24025", 5, "Helen Cho", "helen.cho@heddle.example", 1, "First Light", "139.00",
        OrderStatus.PLACED, now.minus(20, ChronoUnit.DAYS), List.of(
            line(1, "HED-CARAFE", "Stoneware Pour-Over Carafe", "48.00"),
            line(6, "HED-CUP", "Ceramic Espresso Pair", "72.00"),
            line(2, "HED-RUNNER", "Linen Table Runner", "36.00")));
    save(6, "HD-24026", 6, "Marco Bianchi", "marco.bianchi@heddle.example", 2, "Desk Hours", "142.00",
        OrderStatus.CONFIRMED, now.minus(2, ChronoUnit.DAYS), List.of(
            line(3, "HED-LAMP", "Brass Task Lamp", "128.00"),
            line(7, "HED-NOTE", "Leather Pocket Notebook", "28.00")));
  }

  private void save(int sequence, String number, int customer, String name, String email, int offer, String offerName,
      String total, OrderStatus status, Instant placedAt, List<OrderLine> lines) {
    CustomerOrder order = new CustomerOrder();
    order.id = UUID.fromString("40000000-0000-4000-8000-0000000000" + String.format("%02d", sequence));
    order.orderNumber = number;
    order.customerId = UUID.fromString("30000000-0000-4000-8000-0000000000" + String.format("%02d", customer));
    order.customerName = name;
    order.customerEmail = email;
    order.offerId = UUID.fromString("20000000-0000-4000-8000-0000000000" + String.format("%02d", offer));
    order.offerName = offerName;
    order.totalAmount = new BigDecimal(total);
    order.status = status;
    order.note = "";
    order.lines = lines;
    order.placedAt = placedAt;
    order.createdAt = placedAt;
    order.updatedAt = placedAt;
    orders.save(order);
  }

  private static OrderLine line(int product, String sku, String name, String price) {
    OrderLine line = new OrderLine();
    line.productId = UUID.fromString("10000000-0000-4000-8000-0000000000" + String.format("%02d", product));
    line.sku = sku;
    line.productName = name;
    line.quantity = 1;
    line.unitPrice = new BigDecimal(price);
    return line;
  }
}
