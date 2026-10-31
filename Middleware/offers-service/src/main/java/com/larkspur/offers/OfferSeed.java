package com.larkspur.offers;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
class OfferSeed implements CommandLineRunner {
  private final OfferRepository offers;

  OfferSeed(OfferRepository offers) {
    this.offers = offers;
  }

  @Override
  public void run(String... args) {
    if (offers.count() > 0) {
      return;
    }
    LocalDate today = LocalDate.now(ZoneOffset.UTC);
    Instant now = Instant.now();
    save(1, "FIRSTLIGHT", "First Light", "Carafe, espresso cups, and a linen runner for the morning table.", "139.00",
        OfferStatus.ACTIVE, today.minusDays(20), null, List.of(product(1), product(6), product(2)), now.minus(6, ChronoUnit.DAYS));
    save(2, "DESKHOURS", "Desk Hours", "A brass task lamp paired with a pocket notebook.", "142.00",
        OfferStatus.ACTIVE, today.minusDays(15), null, List.of(product(3), product(7)), now.minus(5, ChronoUnit.DAYS));
    save(3, "HOSTING", "Sunday Hosting", "An oak board and a wool throw for a long lunch.", "132.00",
        OfferStatus.ACTIVE, today.minusDays(12), null, List.of(product(4), product(5)), now.minus(4, ChronoUnit.DAYS));
    save(4, "KITCHEN", "Kitchen Pair", "Espresso cups and a ribbed glass carafe.", "99.00",
        OfferStatus.ACTIVE, today.minusDays(10), null, List.of(product(6), product(8)), now.minus(3, ChronoUnit.DAYS));
    save(5, "DINNER", "After Dinner", "Candles, a runner, and the waist apron. Still being styled.", "92.00",
        OfferStatus.DRAFT, today.plusDays(7), null, List.of(product(9), product(2), product(10)), now.minus(2, ChronoUnit.DAYS));
    save(6, "READING", "Reading Corner", "Lamp and candles. This set closed last month.", "148.00",
        OfferStatus.EXPIRED, today.minusDays(40), today.minusDays(5), List.of(product(3), product(9)), now.minus(1, ChronoUnit.DAYS));
  }

  private void save(int sequence, String code, String name, String description, String price, OfferStatus status,
      LocalDate startsOn, LocalDate endsOn, List<UUID> productIds, Instant createdAt) {
    Offer offer = new Offer();
    offer.id = UUID.fromString("20000000-0000-4000-8000-0000000000" + String.format("%02d", sequence));
    offer.code = code;
    offer.name = name;
    offer.description = description;
    offer.offerPrice = new BigDecimal(price);
    offer.status = status;
    offer.startsOn = startsOn;
    offer.endsOn = endsOn;
    offer.productIds = productIds;
    offer.createdAt = createdAt;
    offer.updatedAt = createdAt;
    offers.save(offer);
  }

  private static UUID product(int sequence) {
    return UUID.fromString("10000000-0000-4000-8000-0000000000" + String.format("%02d", sequence));
  }
}
