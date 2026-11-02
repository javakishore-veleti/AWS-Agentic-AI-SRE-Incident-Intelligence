package com.larkspur.customers;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

interface CustomerRepository extends JpaRepository<Customer, UUID> {
  List<Customer> findTop100ByOrderByCreatedAtDesc();

  @Query("""
      select c from Customer c
      where (:name is null or lower(c.fullName) like lower(concat('%', :name, '%')))
        and (:email is null or lower(c.email) like lower(concat('%', :email, '%')))
        and (:status is null or c.status = :status)
      order by c.createdAt desc
      """)
  List<Customer> search(
      @Param("name") String name,
      @Param("email") String email,
      @Param("status") CustomerStatus status,
      Pageable pageable);

  java.util.Optional<Customer> findByEmail(String email);
}

record CustomerRequest(
    @NotBlank @Size(max = 140) String fullName,
    @NotBlank @Email @Size(max = 180) String email,
    @Size(max = 40) String phone,
    String password,
    @NotNull CustomerStatus status) {}

record SignupRequest(
    @NotBlank @Size(max = 140) String fullName,
    @NotBlank @Email @Size(max = 180) String email,
    @Size(max = 40) String phone,
    @NotBlank @Size(min = 8, max = 80) String password) {}

record SignInRequest(@NotBlank @Email String email, @NotBlank String password) {}

record CustomerResponse(
    UUID id,
    String fullName,
    String email,
    String phone,
    CustomerStatus status,
    AccountRole role,
    java.time.Instant createdAt,
    java.time.Instant updatedAt) {}

record AuthResponse(String token, CustomerResponse customer) {}

@RestController
@RequestMapping("/api")
class CustomerController {
  private final CustomerService customers;

  CustomerController(CustomerService customers) {
    this.customers = customers;
  }

  @GetMapping("/customers")
  List<CustomerResponse> latest(
      @RequestHeader(name = "Authorization", required = false) String authorization,
      @RequestParam(required = false) String name,
      @RequestParam(required = false) String email,
      @RequestParam(required = false) String status) {
    customers.requireDesk(authorization);
    return customers.latest(name, email, status);
  }

  @GetMapping("/customers/{id}")
  CustomerResponse one(
      @RequestHeader(name = "Authorization", required = false) String authorization,
      @PathVariable UUID id) {
    customers.requireDesk(authorization);
    return customers.get(id);
  }

  @PostMapping("/customers")
  ResponseEntity<CustomerResponse> create(
      @RequestHeader(name = "Authorization", required = false) String authorization,
      @Valid @RequestBody CustomerRequest request) {
    customers.requireDesk(authorization);
    return ResponseEntity.status(HttpStatus.CREATED).body(customers.create(request));
  }

  @PutMapping("/customers/{id}")
  CustomerResponse update(
      @RequestHeader(name = "Authorization", required = false) String authorization,
      @PathVariable UUID id,
      @Valid @RequestBody CustomerRequest request) {
    customers.requireDesk(authorization);
    return customers.update(id, request);
  }

  @DeleteMapping("/customers/{id}")
  ResponseEntity<Void> delete(
      @RequestHeader(name = "Authorization", required = false) String authorization,
      @PathVariable UUID id) {
    customers.requireDesk(authorization);
    customers.delete(id);
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/auth/signup")
  ResponseEntity<AuthResponse> signup(@Valid @RequestBody SignupRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(customers.signup(request));
  }

  @PostMapping("/auth/signin")
  AuthResponse signin(@Valid @RequestBody SignInRequest request) {
    return customers.signin(request);
  }

  @GetMapping("/auth/me")
  CustomerResponse me(@RequestHeader(name = "Authorization", required = false) String authorization) {
    return customers.me(authorization);
  }
}

@Service
class CustomerService {
  private final CustomerRepository customers;
  private final BCryptPasswordEncoder passwords;
  private final Tokens tokens;

  CustomerService(CustomerRepository customers, BCryptPasswordEncoder passwords, Tokens tokens) {
    this.customers = customers;
    this.passwords = passwords;
    this.tokens = tokens;
  }

  void requireDesk(String authorization) {
    tokens.requireAdmin(authorization);
  }

  List<CustomerResponse> latest(String name, String email, String status) {
    return customers.search(text(name), text(email), status(status), PageRequest.of(0, 100)).stream()
        .map(CustomerService::toResponse)
        .toList();
  }

  private static String text(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return value.trim();
  }

  private static CustomerStatus status(String value) {
    String text = text(value);
    if (text == null) {
      return null;
    }
    try {
      return CustomerStatus.valueOf(text);
    } catch (IllegalArgumentException exception) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown status.");
    }
  }

  CustomerResponse get(UUID id) {
    return toResponse(load(id));
  }

  @Transactional
  CustomerResponse create(CustomerRequest request) {
    requirePassword(request.password());
    Customer customer = new Customer();
    apply(customer, request, true);
    return toResponse(customers.save(customer));
  }

  @Transactional
  CustomerResponse update(UUID id, CustomerRequest request) {
    Customer customer = load(id);
    if (request.password() != null && !request.password().isBlank()) {
      requirePassword(request.password());
    }
    apply(customer, request, false);
    return toResponse(customers.save(customer));
  }

  @Transactional
  void delete(UUID id) {
    if (!customers.existsById(id)) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Customer not found.");
    }
    customers.deleteById(id);
  }

  @Transactional
  AuthResponse signup(SignupRequest request) {
    CustomerResponse customer = create(new CustomerRequest(
        request.fullName(), request.email(), request.phone(), request.password(), CustomerStatus.ACTIVE));
    Customer stored = customers.findById(customer.id()).orElseThrow();
    return new AuthResponse(tokens.issue(stored), customer);
  }

  AuthResponse signin(SignInRequest request) {
    Customer customer = customers.findByEmail(request.email().trim().toLowerCase())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Email or password is incorrect."));
    if (!passwords.matches(request.password(), customer.passwordHash)) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Email or password is incorrect.");
    }
    if (customer.status == CustomerStatus.SUSPENDED) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This account is suspended.");
    }
    return new AuthResponse(tokens.issue(customer), toResponse(customer));
  }

  CustomerResponse me(String authorization) {
    UUID id = UUID.fromString(tokens.parse(authorization).getSubject());
    return get(id);
  }

  private Customer load(UUID id) {
    return customers.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Customer not found."));
  }

  private void apply(Customer customer, CustomerRequest request, boolean creating) {
    customer.fullName = request.fullName().trim();
    customer.email = request.email().trim().toLowerCase();
    customer.phone = request.phone() == null ? "" : request.phone().trim();
    customer.status = request.status();
    if (creating || (request.password() != null && !request.password().isBlank())) {
      customer.passwordHash = passwords.encode(request.password());
    }
    if (creating && customer.role == null) {
      customer.role = AccountRole.CUSTOMER;
    }
  }

  private static void requirePassword(String password) {
    if (password == null || password.length() < 8) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "password must be at least 8 characters.");
    }
  }

  static CustomerResponse toResponse(Customer customer) {
    return new CustomerResponse(
        customer.id,
        customer.fullName,
        customer.email,
        customer.phone,
        customer.status,
        customer.role == null ? AccountRole.CUSTOMER : customer.role,
        customer.createdAt,
        customer.updatedAt);
  }
}

@Component
class Tokens {
  private final SecretKey key;

  Tokens(@Value("${security.jwt.secret}") String secret) {
    this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
  }

  String issue(Customer customer) {
    Instant now = Instant.now();
    return Jwts.builder()
        .subject(customer.id.toString())
        .claim("email", customer.email)
        .claim("name", customer.fullName)
        .claim("role", customer.role == null ? AccountRole.CUSTOMER.name() : customer.role.name())
        .issuedAt(Date.from(now))
        .expiration(Date.from(now.plus(Duration.ofHours(12))))
        .signWith(key)
        .compact();
  }

  void requireAdmin(String header) {
    Object role = parse(header).get("role");
    if (!"ADMIN".equals(role)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "An administrator must approve this request.");
    }
  }

  Claims parse(String header) {
    if (header == null || !header.startsWith("Bearer ")) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sign in to continue.");
    }
    try {
      return Jwts.parser().verifyWith(key).build().parseSignedClaims(header.substring(7)).getPayload();
    } catch (JwtException ex) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sign in to continue.");
    }
  }
}
