package com.larkspur.orders;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

interface OrderRepository extends JpaRepository<CustomerOrder, UUID> {
  List<CustomerOrder> findTop100ByOrderByPlacedAtDesc();

  List<CustomerOrder> findTop100ByCustomerIdOrderByPlacedAtDesc(UUID customerId);

  @Query("""
      select o from CustomerOrder o
      where (:customer is null or lower(o.customerName) like lower(concat('%', :customer, '%')))
        and (:email is null or lower(o.customerEmail) like lower(concat('%', :email, '%')))
        and (:offer is null or lower(o.offerName) like lower(concat('%', :offer, '%')))
        and (:status is null or o.status = :status)
        and (:from is null or o.placedAt >= :from)
        and (:toExclusive is null or o.placedAt < :toExclusive)
      order by o.placedAt desc
      """)
  List<CustomerOrder> search(
      @Param("customer") String customer,
      @Param("email") String email,
      @Param("offer") String offer,
      @Param("status") OrderStatus status,
      @Param("from") Instant from,
      @Param("toExclusive") Instant toExclusive,
      Pageable pageable);
}

record PurchaseRequest(@NotNull UUID offerId) {}

record OrderUpdateRequest(@NotNull OrderStatus status, String note) {}

record LineResponse(UUID productId, String sku, String productName, int quantity, BigDecimal unitPrice) {}

record OrderResponse(
    UUID id,
    String orderNumber,
    UUID customerId,
    String customerName,
    String customerEmail,
    UUID offerId,
    String offerName,
    BigDecimal totalAmount,
    OrderStatus status,
    String note,
    List<LineResponse> lines,
    Instant placedAt,
    Instant updatedAt) {}

record RemoteProduct(UUID id, String sku, String name, BigDecimal unitPrice) {}

record RemoteOffer(UUID id, String name, String status, BigDecimal offerPrice, List<RemoteProduct> products) {}

record ReserveLine(String sku, int qty) {}

record ReserveRequest(String orderRef, List<ReserveLine> lines) {}

@RestController
@RequestMapping("/api/orders")
class OrderController {
  private final OrderService orders;

  OrderController(OrderService orders) {
    this.orders = orders;
  }

  @GetMapping
  List<OrderResponse> latest(
      @RequestHeader(name = "Authorization", required = false) String authorization,
      @RequestParam(required = false) String customer,
      @RequestParam(required = false) String email,
      @RequestParam(required = false) String offer,
      @RequestParam(required = false) String status,
      @RequestParam(required = false) String from,
      @RequestParam(required = false) String to) {
    orders.requireDesk(authorization);
    return orders.latest(customer, email, offer, status, from, to);
  }

  @GetMapping("/mine")
  List<OrderResponse> mine(@RequestHeader(name = "Authorization", required = false) String authorization) {
    return orders.mine(authorization);
  }

  @GetMapping("/{id}")
  OrderResponse one(@PathVariable UUID id) {
    return orders.get(id);
  }

  @PostMapping
  ResponseEntity<OrderResponse> purchase(
      @RequestHeader(name = "Authorization", required = false) String authorization,
      @Valid @RequestBody PurchaseRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(orders.purchase(authorization, request.offerId()));
  }

  @PutMapping("/{id}")
  OrderResponse update(
      @RequestHeader(name = "Authorization", required = false) String authorization,
      @PathVariable UUID id,
      @Valid @RequestBody OrderUpdateRequest request) {
    orders.requireDesk(authorization);
    return orders.update(id, request);
  }

  @DeleteMapping("/{id}")
  ResponseEntity<Void> delete(
      @RequestHeader(name = "Authorization", required = false) String authorization,
      @PathVariable UUID id) {
    orders.requireDesk(authorization);
    orders.delete(id);
    return ResponseEntity.noContent().build();
  }
}

@Service
class OrderService {
  private final OrderRepository orders;
  private final RestClient offers;
  private final RestClient inventory;
  private final ShopperTokens tokens;

  OrderService(
      OrderRepository orders,
      @Qualifier("offersClient") RestClient offers,
      @Qualifier("inventoryClient") RestClient inventory,
      ShopperTokens tokens) {
    this.orders = orders;
    this.offers = offers;
    this.inventory = inventory;
    this.tokens = tokens;
  }

  void requireDesk(String authorization) {
    tokens.requireAdmin(authorization);
  }

  @Transactional(readOnly = true)
  List<OrderResponse> latest(String customer, String email, String offer, String status, String from, String to) {
    return orders.search(
            text(customer),
            text(email),
            text(offer),
            status(status),
            startOfDay(from),
            startOfNextDay(to),
            PageRequest.of(0, 100))
        .stream()
        .map(OrderService::toResponse)
        .toList();
  }

  private static String text(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return value.trim();
  }

  private static OrderStatus status(String value) {
    String text = text(value);
    if (text == null) {
      return null;
    }
    try {
      return OrderStatus.valueOf(text);
    } catch (IllegalArgumentException exception) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown status.");
    }
  }

  private static Instant startOfDay(String day) {
    LocalDate date = date(day);
    if (date == null) {
      return null;
    }
    return date.atStartOfDay(ZoneOffset.UTC).toInstant();
  }

  private static Instant startOfNextDay(String day) {
    LocalDate date = date(day);
    if (date == null) {
      return null;
    }
    return date.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
  }

  private static LocalDate date(String day) {
    String text = text(day);
    if (text == null) {
      return null;
    }
    try {
      return LocalDate.parse(text);
    } catch (DateTimeParseException exception) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown date.");
    }
  }

  @Transactional(readOnly = true)
  List<OrderResponse> mine(String authorization) {
    UUID customerId = UUID.fromString(tokens.parse(authorization).getSubject());
    return orders.findTop100ByCustomerIdOrderByPlacedAtDesc(customerId).stream().map(OrderService::toResponse).toList();
  }

  @Transactional(readOnly = true)
  OrderResponse get(UUID id) {
    return toResponse(load(id));
  }

  @Transactional
  OrderResponse purchase(String authorization, UUID offerId) {
    Claims claims = tokens.parse(authorization);
    RemoteOffer offer = loadOffer(offerId);
    if (!"ACTIVE".equals(offer.status())) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "This offer is not available to purchase.");
    }
    if (offer.products() == null || offer.products().isEmpty()) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "This offer does not include any products.");
    }
    CustomerOrder order = new CustomerOrder();
    order.orderNumber = "HD-" + Long.toString(System.currentTimeMillis(), 36).toUpperCase();
    order.customerId = UUID.fromString(claims.getSubject());
    order.customerName = String.valueOf(claims.get("name"));
    order.customerEmail = String.valueOf(claims.get("email"));
    order.offerId = offer.id();
    order.offerName = offer.name();
    order.totalAmount = offer.offerPrice();
    order.status = OrderStatus.PLACED;
    order.note = "";
    order.lines = new ArrayList<>();
    for (RemoteProduct product : offer.products()) {
      OrderLine line = new OrderLine();
      line.productId = product.id();
      line.sku = product.sku();
      line.productName = product.name();
      line.quantity = 1;
      line.unitPrice = product.unitPrice();
      order.lines.add(line);
    }
    CustomerOrder saved = orders.saveAndFlush(order);
    try {
      inventory.post()
          .uri("/api/reservations")
          .body(new ReserveRequest(
              saved.orderNumber,
              saved.lines.stream().map(line -> new ReserveLine(line.sku, line.quantity)).toList()))
          .retrieve()
          .toBodilessEntity();
    } catch (RestClientResponseException ex) {
      orders.delete(saved);
      String message = "Inventory could not reserve this offer.";
      if (ex.getStatusCode().value() == 409) {
        message = ex.getResponseBodyAsString().contains("message")
            ? readMessage(ex.getResponseBodyAsString())
            : message;
      }
      throw new ResponseStatusException(HttpStatus.CONFLICT, message);
    } catch (RestClientException ex) {
      orders.delete(saved);
      throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Inventory service is unreachable.");
    }
    return toResponse(saved);
  }

  @Transactional
  OrderResponse update(UUID id, OrderUpdateRequest request) {
    CustomerOrder order = load(id);
    order.status = request.status();
    order.note = request.note() == null ? "" : request.note().trim();
    return toResponse(orders.save(order));
  }

  @Transactional
  void delete(UUID id) {
    if (!orders.existsById(id)) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found.");
    }
    orders.deleteById(id);
  }

  private RemoteOffer loadOffer(UUID offerId) {
    try {
      RemoteOffer offer = offers.get().uri("/api/offers/{id}", offerId).retrieve().body(RemoteOffer.class);
      if (offer == null) {
        throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Offers service returned an empty response.");
      }
      return offer;
    } catch (RestClientResponseException ex) {
      if (ex.getStatusCode().value() == 404) {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Offer not found.");
      }
      throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Offers service rejected the lookup.");
    } catch (RestClientException ex) {
      throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Offers service is unreachable.");
    }
  }

  private CustomerOrder load(UUID id) {
    return orders.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found."));
  }

  private static String readMessage(String body) {
    int marker = body.indexOf("\"message\"");
    if (marker < 0) {
      return "Inventory could not reserve this offer.";
    }
    int start = body.indexOf('"', marker + 9);
    int end = body.indexOf('"', start + 1);
    if (start < 0 || end < 0) {
      return "Inventory could not reserve this offer.";
    }
    return body.substring(start + 1, end);
  }

  static OrderResponse toResponse(CustomerOrder order) {
    return new OrderResponse(
        order.id,
        order.orderNumber,
        order.customerId,
        order.customerName,
        order.customerEmail,
        order.offerId,
        order.offerName,
        order.totalAmount,
        order.status,
        order.note,
        order.lines.stream()
            .map(line -> new LineResponse(line.productId, line.sku, line.productName, line.quantity, line.unitPrice))
            .toList(),
        order.placedAt,
        order.updatedAt);
  }
}

@Component
class ShopperTokens {
  private final SecretKey key;

  ShopperTokens(@Value("${security.jwt.secret}") String secret) {
    this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
  }

  void requireAdmin(String header) {
    Object role = parseDesk(header).get("role");
    if (!"ADMIN".equals(role)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "An administrator must approve this request.");
    }
  }

  private Claims parseDesk(String header) {
    if (header == null || !header.startsWith("Bearer ")) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sign in to continue.");
    }
    try {
      return Jwts.parser().verifyWith(key).build().parseSignedClaims(header.substring(7)).getPayload();
    } catch (JwtException ex) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sign in to continue.");
    }
  }

  Claims parse(String header) {
    if (header == null || !header.startsWith("Bearer ")) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sign in to purchase an offer.");
    }
    try {
      return Jwts.parser().verifyWith(key).build().parseSignedClaims(header.substring(7)).getPayload();
    } catch (JwtException ex) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sign in to purchase an offer.");
    }
  }
}
