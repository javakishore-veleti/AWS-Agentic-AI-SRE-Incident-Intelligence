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
import java.util.Optional;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataIntegrityViolationException;
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
import org.springframework.transaction.support.TransactionTemplate;
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

  java.util.Optional<CustomerOrder> findByCustomerIdAndIdempotencyKey(UUID customerId, String idempotencyKey);

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

record PurchaseResult(HttpStatus status, OrderResponse order) {}

record PendingReservation(UUID id, String orderNumber, List<ReserveLine> lines) {}

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

record ReleaseRequest(String orderRef) {}

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
  OrderResponse one(
      @RequestHeader(name = "Authorization", required = false) String authorization,
      @PathVariable UUID id) {
    return orders.get(authorization, id);
  }

  @PostMapping
  ResponseEntity<OrderResponse> purchase(
      @RequestHeader(name = "Authorization", required = false) String authorization,
      @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey,
      @Valid @RequestBody PurchaseRequest request) {
    PurchaseResult result = orders.purchase(authorization, idempotencyKey, request.offerId());
    return ResponseEntity.status(result.status()).body(result.order());
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
    orders.cancel(authorization, id);
    return ResponseEntity.noContent().build();
  }
}

@Service
class OrderService {
  private final OrderRepository orders;
  private final RestClient offers;
  private final RestClient inventory;
  private final ShopperTokens tokens;
  private final TransactionTemplate transactions;
  private final OrderCancellationRepository cancellations;

  OrderService(
      OrderRepository orders,
      @Qualifier("offersClient") RestClient offers,
      @Qualifier("inventoryClient") RestClient inventory,
      ShopperTokens tokens,
      TransactionTemplate transactions,
      OrderCancellationRepository cancellations) {
    this.orders = orders;
    this.offers = offers;
    this.inventory = inventory;
    this.tokens = tokens;
    this.transactions = transactions;
    this.cancellations = cancellations;
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
  OrderResponse get(String authorization, UUID id) {
    Claims claims = tokens.reader(authorization);
    CustomerOrder order = load(id);
    boolean admin = "ADMIN".equals(claims.get("role"));
    if (!admin && !order.customerId.equals(UUID.fromString(claims.getSubject()))) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found.");
    }
    return toResponse(order);
  }

  PurchaseResult purchase(String authorization, String idempotencyKey, UUID offerId) {
    Claims claims = tokens.parse(authorization);
    String key = idempotencyKey == null ? "" : idempotencyKey.trim();
    if (key.isEmpty() || key.length() > 80) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Send an Idempotency-Key header.");
    }
    UUID customerId = UUID.fromString(claims.getSubject());
    Optional<CustomerOrder> existing = orders.findByCustomerIdAndIdempotencyKey(customerId, key);
    if (existing.isPresent()) {
      return resume(existing.get());
    }
    RemoteOffer offer = loadOffer(offerId);
    if (!"ACTIVE".equals(offer.status())) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "This offer is not available to purchase.");
    }
    if (offer.products() == null || offer.products().isEmpty()) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "This offer does not include any products.");
    }
    CustomerOrder order = new CustomerOrder();
    order.orderNumber = "HD-" + Long.toString(System.currentTimeMillis(), 36).toUpperCase();
    order.customerId = customerId;
    order.customerName = String.valueOf(claims.get("name"));
    order.customerEmail = String.valueOf(claims.get("email"));
    order.offerId = offer.id();
    order.offerName = offer.name();
    order.totalAmount = offer.offerPrice();
    order.status = OrderStatus.RESERVING;
    order.idempotencyKey = key;
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
    PendingReservation saved;
    try {
      saved = transactions.execute(status -> remember(orders.saveAndFlush(order)));
    } catch (DataIntegrityViolationException ex) {
      return resume(orders.findByCustomerIdAndIdempotencyKey(customerId, key)
          .orElseThrow(() -> new ResponseStatusException(
              HttpStatus.CONFLICT, "This purchase is already in progress. Retry with the same Idempotency-Key.")));
    }
    return reserve(saved);
  }

  private PurchaseResult resume(CustomerOrder order) {
    OrderStatus state = transactions.execute(status -> load(order.id).status);
    if (state == OrderStatus.RESERVING) {
      return reserve(transactions.execute(status -> remember(load(order.id))));
    }
    if (state == OrderStatus.RESERVATION_FAILED) {
      String message = transactions.execute(status -> {
        String stored = load(order.id).failureMessage;
        return stored == null || stored.isBlank() ? "Inventory could not reserve this offer." : stored;
      });
      throw new ResponseStatusException(HttpStatus.CONFLICT, message);
    }
    return transactions.execute(status -> new PurchaseResult(HttpStatus.OK, toResponse(load(order.id))));
  }

  private PurchaseResult reserve(PendingReservation saved) {
    try {
      inventory.post()
          .uri("/api/reservations")
          .body(new ReserveRequest(saved.orderNumber(), saved.lines()))
          .retrieve()
          .toBodilessEntity();
    } catch (RestClientResponseException ex) {
      if (ex.getStatusCode().value() == 409) {
        String message = "Inventory could not reserve this offer.";
        if (ex.getResponseBodyAsString().contains("message")) {
          message = readMessage(ex.getResponseBodyAsString());
        }
        rememberFailure(saved.id(), message);
        throw new ResponseStatusException(HttpStatus.CONFLICT, message);
      }
      throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Inventory service is unreachable.");
    } catch (RestClientException ex) {
      throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Inventory service is unreachable.");
    }
    return transactions.execute(status -> {
      CustomerOrder current = load(saved.id());
      if (current.status == OrderStatus.RESERVING) {
        current.status = OrderStatus.PLACED;
        current.failureMessage = null;
        orders.save(current);
        return new PurchaseResult(HttpStatus.CREATED, toResponse(current));
      }
      return new PurchaseResult(HttpStatus.OK, toResponse(current));
    });
  }

  private PendingReservation remember(CustomerOrder order) {
    List<ReserveLine> lines = order.lines.stream().map(line -> new ReserveLine(line.sku, line.quantity)).toList();
    return new PendingReservation(order.id, order.orderNumber, lines);
  }

  private void rememberFailure(UUID id, String message) {
    String stored = message.length() > 500 ? message.substring(0, 500) : message;
    transactions.executeWithoutResult(status -> {
      CustomerOrder order = load(id);
      order.status = OrderStatus.RESERVATION_FAILED;
      order.failureMessage = stored;
      orders.save(order);
    });
  }

  @Transactional
  OrderResponse update(UUID id, OrderUpdateRequest request) {
    CustomerOrder order = load(id);
    order.status = request.status();
    order.note = request.note() == null ? "" : request.note().trim();
    return toResponse(orders.save(order));
  }

  void cancel(String authorization, UUID id) {
    Claims claims = tokens.reader(authorization);
    tokens.requireAdmin(authorization);
    CustomerOrder order = transactions.execute(status -> load(id));
    if (order.status == OrderStatus.CANCELLED) {
      return;
    }
    try {
      inventory.post()
          .uri("/api/reservations/release")
          .body(new ReleaseRequest(order.orderNumber))
          .retrieve()
          .toBodilessEntity();
    } catch (RestClientException ex) {
      throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Inventory service is unreachable.");
    }
    String actor = claims.getSubject();
    String email = claims.get("email") == null ? "" : String.valueOf(claims.get("email"));
    transactions.executeWithoutResult(status -> {
      CustomerOrder current = load(id);
      if (current.status == OrderStatus.CANCELLED || cancellations.findByOrderId(current.id).isPresent()) {
        return;
      }
      OrderCancellation audit = new OrderCancellation();
      audit.orderId = current.id;
      audit.orderNumber = current.orderNumber;
      audit.previousStatus = current.status;
      audit.actorId = UUID.fromString(actor);
      audit.actorEmail = email;
      cancellations.save(audit);
      current.status = OrderStatus.CANCELLED;
      orders.save(current);
    });
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

  Claims reader(String header) {
    return parseDesk(header);
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
