package com.larkspur.offers;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

record ProductSummary(UUID id, String sku, String name, String category, BigDecimal unitPrice) {}

record OfferRequest(
    @NotBlank @Size(max = 40) String code,
    @NotBlank @Size(max = 140) String name,
    @Size(max = 2000) String description,
    @NotNull @DecimalMin("0.01") BigDecimal offerPrice,
    @NotNull OfferStatus status,
    LocalDate startsOn,
    LocalDate endsOn,
    @NotEmpty List<UUID> productIds) {}

record OfferResponse(
    UUID id,
    String code,
    String name,
    String description,
    BigDecimal offerPrice,
    OfferStatus status,
    LocalDate startsOn,
    LocalDate endsOn,
    List<UUID> productIds,
    List<ProductSummary> products,
    Instant createdAt,
    Instant updatedAt) {}

@RestController
@RequestMapping("/api/offers")
class OfferController {
  private final OfferService offers;
  private final AdminGate desk;

  OfferController(OfferService offers, AdminGate desk) {
    this.offers = offers;
    this.desk = desk;
  }

  @GetMapping
  List<OfferResponse> latest(
      @RequestHeader(name = "Authorization", required = false) String authorization,
      @RequestParam(required = false) String name,
      @RequestParam(required = false) String code,
      @RequestParam(required = false) String product,
      @RequestParam(required = false) String status) {
    desk.require(authorization);
    return offers.latest(name, code, product, status);
  }

  @GetMapping("/active")
  List<OfferResponse> active() {
    return offers.active();
  }

  @GetMapping("/{id}")
  OfferResponse one(@PathVariable UUID id) {
    return offers.get(id);
  }

  @PostMapping
  ResponseEntity<OfferResponse> create(
      @RequestHeader(name = "Authorization", required = false) String authorization,
      @Valid @RequestBody OfferRequest request) {
    desk.require(authorization);
    return ResponseEntity.status(HttpStatus.CREATED).body(offers.create(request));
  }

  @PutMapping("/{id}")
  OfferResponse update(
      @RequestHeader(name = "Authorization", required = false) String authorization,
      @PathVariable UUID id,
      @Valid @RequestBody OfferRequest request) {
    desk.require(authorization);
    return offers.update(id, request);
  }

  @DeleteMapping("/{id}")
  ResponseEntity<Void> delete(
      @RequestHeader(name = "Authorization", required = false) String authorization,
      @PathVariable UUID id) {
    desk.require(authorization);
    offers.delete(id);
    return ResponseEntity.noContent().build();
  }
}

@Service
class OfferService {
  private static final Logger log = LoggerFactory.getLogger(OfferService.class);
  private final OfferRepository offers;
  private final CatalogClient catalog;

  OfferService(OfferRepository offers, CatalogClient catalog) {
    this.offers = offers;
    this.catalog = catalog;
  }

  @Transactional(readOnly = true)
  List<OfferResponse> latest() {
    return latest(null, null, null, null);
  }

  @Transactional(readOnly = true)
  List<OfferResponse> latest(String name, String code, String product, String status) {
    String productText = text(product);
    List<Offer> rows = offers.search(text(name), text(code), status(status), PageRequest.of(0, productText == null ? 100 : 1000));
    List<OfferResponse> mapped = mapAll(rows);
    if (productText == null) {
      return mapped;
    }
    String needle = productText.toLowerCase();
    return mapped.stream()
        .filter(offer -> offer.products().stream().anyMatch(item ->
            item.name().toLowerCase().contains(needle) || item.sku().toLowerCase().contains(needle)))
        .limit(100)
        .toList();
  }

  private static String text(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return value.trim();
  }

  private static OfferStatus status(String value) {
    String text = text(value);
    if (text == null) {
      return null;
    }
    try {
      return OfferStatus.valueOf(text);
    } catch (IllegalArgumentException exception) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown status.");
    }
  }

  @Transactional(readOnly = true)
  List<OfferResponse> active() {
    LocalDate today = LocalDate.now(ZoneOffset.UTC);
    return latest().stream()
        .filter(offer -> offer.status() == OfferStatus.ACTIVE)
        .filter(offer -> offer.startsOn() == null || !offer.startsOn().isAfter(today))
        .filter(offer -> offer.endsOn() == null || !offer.endsOn().isBefore(today))
        .toList();
  }

  @Transactional(readOnly = true)
  OfferResponse get(UUID id) {
    return mapAll(List.of(load(id))).getFirst();
  }

  @Transactional
  OfferResponse create(OfferRequest request) {
    validate(request, true);
    Offer offer = new Offer();
    apply(offer, request);
    return mapAll(List.of(offers.save(offer))).getFirst();
  }

  @Transactional
  OfferResponse update(UUID id, OfferRequest request) {
    validate(request, true);
    Offer offer = load(id);
    apply(offer, request);
    return mapAll(List.of(offers.save(offer))).getFirst();
  }

  @Transactional
  void delete(UUID id) {
    if (!offers.existsById(id)) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Offer not found.");
    }
    offers.deleteById(id);
  }

  private void validate(OfferRequest request, boolean requireCatalog) {
    if (request.startsOn() != null && request.endsOn() != null && request.endsOn().isBefore(request.startsOn())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The end date is before the start date.");
    }
    List<UUID> distinct = request.productIds().stream().distinct().toList();
    if (distinct.size() != request.productIds().size()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choose each product once.");
    }
    if (!requireCatalog) {
      return;
    }
    List<ProductSummary> found = catalog.lookup(distinct, true);
    if (found.size() != distinct.size()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "One or more products are not in the catalog.");
    }
  }

  private Offer load(UUID id) {
    return offers.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Offer not found."));
  }

  private static void apply(Offer offer, OfferRequest request) {
    offer.code = request.code().trim().toUpperCase();
    offer.name = request.name().trim();
    offer.description = request.description() == null ? "" : request.description().trim();
    offer.offerPrice = request.offerPrice();
    offer.status = request.status();
    offer.startsOn = request.startsOn();
    offer.endsOn = request.endsOn();
    offer.productIds = new ArrayList<>(request.productIds());
  }

  private List<OfferResponse> mapAll(List<Offer> rows) {
    List<UUID> ids = rows.stream().flatMap(offer -> offer.productIds.stream()).distinct().toList();
    Map<UUID, ProductSummary> products = catalog.lookup(ids, false).stream()
        .collect(Collectors.toMap(ProductSummary::id, Function.identity(), (left, right) -> left, LinkedHashMap::new));
    return rows.stream().map(offer -> new OfferResponse(
        offer.id,
        offer.code,
        offer.name,
        offer.description,
        offer.offerPrice,
        offer.status,
        offer.startsOn,
        offer.endsOn,
        List.copyOf(offer.productIds),
        offer.productIds.stream().map(products::get).filter(item -> item != null).toList(),
        offer.createdAt,
        offer.updatedAt)).toList();
  }
}

@Service
class CatalogClient {
  private static final Logger log = LoggerFactory.getLogger(CatalogClient.class);
  private final RestClient catalog;
  private final StringRedisTemplate redis;
  private final ObjectMapper json;

  CatalogClient(RestClient catalogRestClient, StringRedisTemplate redis, ObjectMapper json) {
    this.catalog = catalogRestClient;
    this.redis = redis;
    this.json = json;
  }

  List<ProductSummary> lookup(List<UUID> ids, boolean required) {
    if (ids.isEmpty()) {
      return List.of();
    }
    List<ProductSummary> resolved = new ArrayList<>();
    List<UUID> missing = new ArrayList<>();
    for (UUID id : ids) {
      ProductSummary cached = readCache(id);
      if (cached == null) {
        missing.add(id);
      } else {
        resolved.add(cached);
      }
    }
    if (!missing.isEmpty()) {
      try {
        String joined = missing.stream().map(UUID::toString).collect(Collectors.joining(","));
        String body = catalog.get()
            .uri(builder -> builder.path("/api/products/lookup").queryParam("ids", joined).build())
            .retrieve()
            .body(String.class);
        List<ProductSummary> fetched = body == null
            ? List.of()
            : json.readValue(body, new TypeReference<List<ProductSummary>>() {});
        for (ProductSummary product : fetched) {
          writeCache(product);
          resolved.add(product);
        }
      } catch (RestClientException ex) {
        log.warn("Catalog lookup failed: {}", ex.getMessage());
        if (required) {
          throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Product catalog is unreachable.");
        }
      } catch (Exception ex) {
        log.warn("Catalog lookup could not be read: {}", ex.getMessage());
        if (required) {
          throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Product catalog is unreachable.");
        }
      }
    }
    return resolved;
  }

  private ProductSummary readCache(UUID id) {
    try {
      String raw = redis.opsForValue().get("catalog:product:" + id);
      return raw == null ? null : json.readValue(raw, ProductSummary.class);
    } catch (Exception ex) {
      return null;
    }
  }

  private void writeCache(ProductSummary product) {
    try {
      redis.opsForValue().set("catalog:product:" + product.id(), json.writeValueAsString(product), Duration.ofSeconds(60));
    } catch (Exception ex) {
      log.debug("Skipping catalog cache write: {}", ex.getMessage());
    }
  }
}
