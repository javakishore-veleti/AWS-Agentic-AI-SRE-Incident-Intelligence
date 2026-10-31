# APP-002 Commerce Integration Tests Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Prove signup, signin, offer search, checkout, inventory reservation, and order history, plus every error status the five Java services already return, each in that service's own Maven test phase.

**Architecture:** `Middleware/services-test-common` is a plain jar on the reactor. Each service depends on it at test scope and runs MockMvc against H2. `DownstreamClientOverrides` replaces an existing `RestClient` bean with one built from `MockRestServiceServer.bindTo(RestClient.builder())`. Offers tests start embedded Redis. No APP-002 test starts Kafka. Production `RestClient` `@Bean` methods stay unchanged.

**Tech Stack:** Java 21, Spring Boot 3.4.5, JUnit and MockMvc from `spring-boot-starter-test`, H2 `MODE=PostgreSQL`, jjwt 0.12.6, `com.github.codemonstur:embedded-redis` 1.4.4, `spring-kafka-test` from the Spring Boot 3.4.5 BOM.

**Spec:** `docs/superpowers/specs/2026-10-31-app-002-commerce-integration-tests-design.md`

## Global Constraints

- Work only in `.worktrees/issue-2-app-002` on branch `issue/2-app-002-commerce-integration-tests`.
- `services-test-common` is a plain jar. It does not declare `spring-boot-maven-plugin`. It does not depend on the five services.
- Each service depends on `services-test-common` with Maven `test` scope. Maven does not export the dependencies of a test-scoped artifact, so each service pom also declares `spring-boot-starter-test` and `h2` at test scope. Offers also declares `embedded-redis` at test scope. Those scopes keep the libraries out of the runtime image.
- Parent `dependencyManagement` pins `com.github.codemonstur:embedded-redis` to `1.4.4`. `spring-kafka-test` has no version tag; the Spring Boot 3.4.5 BOM supplies it.
- H2 URL is `jdbc:h2:mem:commerce;MODE=PostgreSQL;DB_CLOSE_DELAY=-1`. `spring.jpa.hibernate.ddl-auto` is `update`. `fault.response-delay-ms` is `0`. `observability.logs.exporter` is `console`. `observability.traces.exporter` is `none`.
- The test JWT secret is `commerce-test-jwt-hmac-secret-key-32b`. It is at least 32 bytes. It lives in `services-test-common/src/main/resources/commerce-test.properties` because Maven packages main resources into the jar that test-scoped dependents see. Do not copy `local-dev-sre-incident-intelligence-jwt-secret-key` into this plan, the spec, or a new document.
- Production `RestClient` methods stay as they are. The test replaces a bean only when its name already exists. The names are `offersClient`, `inventoryClient`, `catalogRestClient`, and `inventoryRestClient`.
- `services-test-common` has no test class of its own. Surefire for this issue runs `CustomerAccessTest`, `ProductApiTest`, `OfferApiTest`, `PurchaseHistoryTest`, `ReservationTest`, the advice tests beside them, and the existing `ObservabilityEnvironmentPostProcessorTest`.
- Advice-only cases call the package-visible advice directly and do not start a Spring context.
- Each test creates its own email, SKU, and offer code. The duplicate-email, duplicate-SKU, and duplicate-code tests are the ones that reuse a key.
- WireMock is not a dependency. No test starts the other services, the portals, the Compose stack, or the Kafka broker.
- Do not edit `SUPERPOWERS_GitIssues_Progress.md` from this worktree. That checkout's copy is behind `docs/progress-command-rows`.
- From `Middleware`, `mvn test` exits 0. This plan does not claim that command has already been run for the new tests.

## File structure

- `Middleware/pom.xml` — add the module and the embedded-redis version pin.
- `Middleware/services-test-common/pom.xml` — compile the harness. No repackage plugin.
- `Middleware/services-test-common/src/main/resources/commerce-test.properties` — H2, JWT, fault delay, console logs, traces off, stub base URLs.
- `Middleware/services-test-common/src/main/java/com/larkspur/testing/CommerceServiceTest.java` — MockMvc and H2 annotation.
- `Middleware/services-test-common/src/main/java/com/larkspur/testing/DownstreamInitializer.java` — registers the bean-definition replacement before the production clients are created.
- `Middleware/services-test-common/src/main/java/com/larkspur/testing/DownstreamClientOverrides.java` — replaces a named `RestClient` bean only when that name exists.
- `Middleware/services-test-common/src/main/java/com/larkspur/testing/DownstreamServers.java` — holds the `MockRestServiceServer` for a bean name.
- `Middleware/services-test-common/src/main/java/com/larkspur/testing/Datasets.java` — shopper, product, offer, and stock records. No import from a service jar.
- `Middleware/services-test-common/src/main/java/com/larkspur/testing/ShopperTokens.java` — signs a Bearer token with the test secret.
- `Middleware/services-test-common/src/main/java/com/larkspur/testing/EmbeddedRedis.java` — random-port Redis. Offers tests call `start()`.
- `Middleware/services-test-common/src/main/java/com/larkspur/testing/EmbeddedKafkaSupport.java` — can start an in-memory broker. No APP-002 test calls it.
- `Middleware/customers-service/pom.xml` plus `src/test/java/com/larkspur/customers/CustomerAccessTest.java` and `CustomerAdviceTest.java`.
- `Middleware/product-catalog-service/pom.xml` plus `src/test/java/com/larkspur/catalog/ProductApiTest.java` and `CatalogAdviceTest.java`.
- `Middleware/offers-service/pom.xml` plus `src/test/java/com/larkspur/offers/OfferApiTest.java` and `OfferAdviceTest.java`.
- `Middleware/orders-service/pom.xml` plus `src/test/java/com/larkspur/orders/PurchaseHistoryTest.java` and `OrderAdviceTest.java`.
- `Middleware/inventory-service/pom.xml` plus `src/test/java/com/larkspur/inventory/ReservationTest.java` and `InventoryAdviceTest.java`.

---

### Task 1: Shared test jar

**Files:**
- Modify: `Middleware/pom.xml`
- Create: `Middleware/services-test-common/pom.xml`
- Create: `Middleware/services-test-common/src/main/resources/commerce-test.properties`
- Create: `Middleware/services-test-common/src/main/java/com/larkspur/testing/CommerceServiceTest.java`
- Create: `Middleware/services-test-common/src/main/java/com/larkspur/testing/DownstreamInitializer.java`
- Create: `Middleware/services-test-common/src/main/java/com/larkspur/testing/DownstreamClientOverrides.java`
- Create: `Middleware/services-test-common/src/main/java/com/larkspur/testing/DownstreamServers.java`
- Create: `Middleware/services-test-common/src/main/java/com/larkspur/testing/Datasets.java`
- Create: `Middleware/services-test-common/src/main/java/com/larkspur/testing/ShopperTokens.java`
- Create: `Middleware/services-test-common/src/main/java/com/larkspur/testing/EmbeddedRedis.java`
- Create: `Middleware/services-test-common/src/main/java/com/larkspur/testing/EmbeddedKafkaSupport.java`

**Interfaces:**
- Consumes: parent compiler release 21 and the Spring Boot 3.4.5 BOM already imported by `Middleware/pom.xml`.
- Produces: `@CommerceServiceTest`, `DownstreamServers.server(String name)`, `DownstreamServers.reset()`, `Datasets.signup`, `Datasets.customer`, `Datasets.product`, `Datasets.offer`, `Datasets.stock`, `ShopperTokens.bearer(UUID customerId, String name, String email)`, `EmbeddedRedis.start()`, `EmbeddedRedis.port()`, `EmbeddedKafkaSupport.start()`.

- [ ] **Step 1: Add the module and the version pin**

In `Middleware/pom.xml`, add `<module>services-test-common</module>` immediately after `<module>service-observability</module>`.

Inside `dependencyManagement`, after the logstash dependency, add:

```xml
<dependency>
  <groupId>com.github.codemonstur</groupId>
  <artifactId>embedded-redis</artifactId>
  <version>1.4.4</version>
</dependency>
```

- [ ] **Step 2: Create the plain jar pom**

Create `Middleware/services-test-common/pom.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0">
  <modelVersion>4.0.0</modelVersion>
  <parent>
    <groupId>com.larkspur</groupId>
    <artifactId>middleware</artifactId>
    <version>0.1.0</version>
  </parent>
  <artifactId>services-test-common</artifactId>
  <name>services-test-common</name>

  <dependencies>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-test</artifactId>
    </dependency>
    <dependency>
      <groupId>com.h2database</groupId>
      <artifactId>h2</artifactId>
    </dependency>
    <dependency>
      <groupId>io.jsonwebtoken</groupId>
      <artifactId>jjwt-api</artifactId>
    </dependency>
    <dependency>
      <groupId>io.jsonwebtoken</groupId>
      <artifactId>jjwt-impl</artifactId>
    </dependency>
    <dependency>
      <groupId>io.jsonwebtoken</groupId>
      <artifactId>jjwt-jackson</artifactId>
    </dependency>
    <dependency>
      <groupId>com.github.codemonstur</groupId>
      <artifactId>embedded-redis</artifactId>
    </dependency>
    <dependency>
      <groupId>org.springframework.kafka</groupId>
      <artifactId>spring-kafka-test</artifactId>
    </dependency>
  </dependencies>
</project>
```

Offers already depends on `spring-boot-starter-data-redis`, so `OfferApiTest` can inject that service's `StringRedisTemplate`.

- [ ] **Step 3: Create the test properties**

Create `Middleware/services-test-common/src/main/resources/commerce-test.properties`:

```properties
spring.datasource.url=jdbc:h2:mem:commerce;MODE=PostgreSQL;DB_CLOSE_DELAY=-1
spring.datasource.driver-class-name=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=
spring.jpa.hibernate.ddl-auto=update
security.jwt.secret=commerce-test-jwt-hmac-secret-key-32b
fault.response-delay-ms=0
observability.logs.exporter=console
observability.traces.exporter=none
clients.offers-base-url=http://offers.test
clients.inventory-base-url=http://inventory.test
clients.catalog-base-url=http://catalog.test
```

- [ ] **Step 4: Create the annotation and the client replacement**

Create `CommerceServiceTest.java`:

```java
package com.larkspur.testing;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:commerce-test.properties")
@ContextConfiguration(initializers = DownstreamInitializer.class)
public @interface CommerceServiceTest {
}
```

Create `DownstreamInitializer.java`:

```java
package com.larkspur.testing;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

public final class DownstreamInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
  @Override
  public void initialize(ConfigurableApplicationContext context) {
    context.addBeanFactoryPostProcessor(new DownstreamClientOverrides());
  }
}
```

Create `DownstreamClientOverrides.java`:

```java
package com.larkspur.testing;

import java.util.List;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.web.client.RestClient;

public final class DownstreamClientOverrides implements BeanDefinitionRegistryPostProcessor {
  public static final List<String> NAMES = List.of(
      "offersClient", "inventoryClient", "catalogRestClient", "inventoryRestClient");

  @Override
  public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) {
    for (String name : NAMES) {
      if (!registry.containsBeanDefinition(name)) {
        continue;
      }
      registry.removeBeanDefinition(name);
      RootBeanDefinition definition = new RootBeanDefinition(RestClient.class);
      definition.setTargetType(RestClient.class);
      definition.setScope(BeanDefinition.SCOPE_SINGLETON);
      definition.setInstanceSupplier(() -> DownstreamServers.bind(name));
      registry.registerBeanDefinition(name, definition);
    }
  }

  @Override
  public void postProcessBeanFactory(org.springframework.beans.factory.config.ConfigurableListableBeanFactory beanFactory) {
  }
}
```

Create `DownstreamServers.java`:

```java
package com.larkspur.testing;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

public final class DownstreamServers {
  private static final Map<String, MockRestServiceServer> SERVERS = new ConcurrentHashMap<>();

  private DownstreamServers() {
  }

  public static RestClient bind(String name) {
    String base = switch (name) {
      case "offersClient" -> "http://offers.test";
      case "catalogRestClient" -> "http://catalog.test";
      case "inventoryClient", "inventoryRestClient" -> "http://inventory.test";
      default -> throw new IllegalArgumentException(name);
    };
    RestClient.Builder builder = RestClient.builder()
        .baseUrl(base)
        .requestFactory(new BufferingClientHttpRequestFactory(new SimpleClientHttpRequestFactory()));
    MockRestServiceServer server = MockRestServiceServer.bindTo(builder).ignoreExpectOrder(true).build();
    SERVERS.put(name, server);
    return builder.build();
  }

  public static MockRestServiceServer server(String name) {
    MockRestServiceServer server = SERVERS.get(name);
    if (server == null) {
      throw new IllegalStateException("No mock server for " + name);
    }
    return server;
  }

  public static void reset() {
    SERVERS.values().forEach(MockRestServiceServer::reset);
  }
}
```

`BufferingClientHttpRequestFactory` lets a test match the request body and still gives `RestClient` a body to read. `bindTo(RestClient.builder())` is the call the spec requires; the builder also receives the stub base URL so the existing relative `uri(...)` calls resolve.

- [ ] **Step 5: Create datasets, tokens, Redis, and Kafka support**

Create `Datasets.java`:

```java
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
```

Create `ShopperTokens.java`:

```java
package com.larkspur.testing;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

public final class ShopperTokens {
  public static final String SECRET = "commerce-test-jwt-hmac-secret-key-32b";

  private ShopperTokens() {
  }

  public static String bearer(UUID customerId, String name, String email) {
    SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
    Instant now = Instant.now();
    String token = Jwts.builder()
        .subject(customerId.toString())
        .claim("email", email)
        .claim("name", name)
        .issuedAt(Date.from(now))
        .expiration(Date.from(now.plus(Duration.ofHours(1))))
        .signWith(key)
        .compact();
    return "Bearer " + token;
  }
}
```

Create `EmbeddedRedis.java`:

```java
package com.larkspur.testing;

import java.net.ServerSocket;
import redis.embedded.RedisServer;
import redis.embedded.core.ExecutableProvider;

public final class EmbeddedRedis {
  private static RedisServer server;
  private static int port;

  private EmbeddedRedis() {
  }

  public static synchronized void start() {
    if (server != null) {
      return;
    }
    port = freePort();
    server = server(port, false);
    try {
      server.start();
    } catch (RuntimeException first) {
      port = freePort();
      server = server(port, true);
      server.start();
    }
  }

  public static synchronized int port() {
    if (server == null) {
      throw new IllegalStateException("Redis has not been started");
    }
    return port;
  }

  public static synchronized void stop() {
    if (server != null) {
      server.stop();
      server = null;
    }
  }

  private static RedisServer server(int port, boolean unsignedMacBinary) {
    var builder = RedisServer.newRedisServer().port(port).setting("bind 127.0.0.1").setting("daemonize no");
    if (unsignedMacBinary) {
      builder.executableProvider(ExecutableProvider.REDIS_7_2_MACOSX_14_SONOMA_HANKCP);
    }
    return builder.build();
  }

  private static int freePort() {
    try (ServerSocket socket = new ServerSocket(0)) {
      return socket.getLocalPort();
    } catch (java.io.IOException ex) {
      throw new IllegalStateException("No free port", ex);
    }
  }
}
```

The second start is the library's macOS binary that does not need Homebrew OpenSSL. Use it only when the default binary fails.

Create `EmbeddedKafkaSupport.java`:

```java
package com.larkspur.testing;

import org.springframework.kafka.test.EmbeddedKafkaKraftBroker;

public final class EmbeddedKafkaSupport {
  private EmbeddedKafkaKraftBroker broker;

  public void start() {
    broker = new EmbeddedKafkaKraftBroker(1, 1);
    try {
      broker.afterPropertiesSet();
    } catch (Exception ex) {
      throw new IllegalStateException("Kafka did not start", ex);
    }
  }

  public String bootstrapServers() {
    return broker.getBrokersAsString();
  }

  public void stop() {
    if (broker != null) {
      broker.destroy();
      broker = null;
    }
  }
}
```

No APP-002 test calls `start()`.

- [ ] **Step 6: Compile the jar**

Run from `Middleware`:

```bash
mvn -pl services-test-common -am test
```

Expected: `BUILD SUCCESS`. Surefire reports no tests for `services-test-common`.

If `ExecutableProvider.REDIS_7_2_MACOSX_14_SONOMA_HANKCP` does not exist on 1.4.4, use the constant of that name that the jar actually contains, and keep the default-binary-then-retry behavior. If `EmbeddedKafkaKraftBroker` is not in the BOM's `spring-kafka-test`, use the embedded broker class that jar contains. Do not add a test that calls `start()`.

- [ ] **Step 7: Commit**

```bash
git add Middleware/pom.xml Middleware/services-test-common
git commit -m "Add the shared commerce test jar."
```

---

### Task 2: Customer access

**Files:**
- Modify: `Middleware/customers-service/pom.xml`
- Create: `Middleware/customers-service/src/test/java/com/larkspur/customers/CustomerAccessTest.java`
- Create: `Middleware/customers-service/src/test/java/com/larkspur/customers/CustomerAdviceTest.java`
- Test: `mvn -pl customers-service -am -Dtest=CustomerAccessTest,CustomerAdviceTest test`

**Interfaces:**
- Consumes: `@CommerceServiceTest` and `Datasets` from Task 1. `CustomersApplication`, `POST /api/auth/signup`, `POST /api/auth/signin`, `GET /api/auth/me`, `POST /api/customers`, `GET /api/customers/{id}`. Package-visible `ApiErrors.unexpected(Exception)`.
- Produces: a customers test phase that covers signup, signin, me, and the customers rows in the spec error table.

- [ ] **Step 1: Write the failing tests**

Add this dependency to `customers-service/pom.xml` only after Step 2 has failed. Step 1 creates the two test files and does not edit the pom yet.

Create `CustomerAccessTest.java`:

```java
package com.larkspur.customers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.larkspur.testing.CommerceServiceTest;
import com.larkspur.testing.Datasets;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@CommerceServiceTest
class CustomerAccessTest {
  @Autowired MockMvc mvc;
  @Autowired ObjectMapper json;

  @Test
  void signupSigninAndMe() throws Exception {
    String email = "ada-" + UUID.randomUUID() + "@example.test";
    MvcResult signup = mvc.perform(post("/api/auth/signup")
            .contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsBytes(Datasets.signup("Ada Lovelace", email, "password12"))))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.customer.email").value(email))
        .andExpect(jsonPath("$.token").isNotEmpty())
        .andReturn();
    String token = json.readTree(signup.getResponse().getContentAsString()).get("token").asText();

    mvc.perform(post("/api/auth/signin")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"" + email + "\",\"password\":\"password12\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.customer.email").value(email));

    mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.email").value(email));
  }

  @Test
  void wrongPasswordIs401() throws Exception {
    String email = "wrong-" + UUID.randomUUID() + "@example.test";
    signup(email);
    mvc.perform(post("/api/auth/signin")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"" + email + "\",\"password\":\"not-the-password\"}"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.message").value("Email or password is incorrect."));
  }

  @Test
  void suspendedAccountIs403() throws Exception {
    String email = "suspended-" + UUID.randomUUID() + "@example.test";
    mvc.perform(post("/api/customers")
            .contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsBytes(Datasets.customer("Grace Hopper", email, "password12", "SUSPENDED"))))
        .andExpect(status().isCreated());
    mvc.perform(post("/api/auth/signin")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"" + email + "\",\"password\":\"password12\"}"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.message").value("This account is suspended."));
  }

  @Test
  void missingAndInvalidTokenAre401() throws Exception {
    mvc.perform(get("/api/auth/me"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.message").value("Sign in to continue."));
    mvc.perform(get("/api/auth/me").header("Authorization", "Bearer not-a-token"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.message").value("Sign in to continue."));
  }

  @Test
  void unknownCustomerIs404() throws Exception {
    mvc.perform(get("/api/customers/" + UUID.randomUUID()))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").value("Customer not found."));
  }

  @Test
  void duplicateEmailIs409() throws Exception {
    String email = "dup-" + UUID.randomUUID() + "@example.test";
    signup(email);
    mvc.perform(post("/api/auth/signup")
            .contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsBytes(Datasets.signup("Second Ada", email, "password12"))))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.message").value("An account with this email already exists."));
  }

  @Test
  void shortSignupPasswordIs400() throws Exception {
    mvc.perform(post("/api/auth/signup")
            .contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsBytes(Datasets.signup("Ada Lovelace", "short-" + UUID.randomUUID() + "@example.test", "short"))))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("password size must be between 8 and 80"));
  }

  @Test
  void shortCustomerPasswordIs400() throws Exception {
    mvc.perform(post("/api/customers")
            .contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsBytes(Datasets.customer("Ada Lovelace", "desk-" + UUID.randomUUID() + "@example.test", "short", "ACTIVE"))))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("password must be at least 8 characters."));
  }

  private void signup(String email) throws Exception {
    mvc.perform(post("/api/auth/signup")
            .contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsBytes(Datasets.signup("Ada Lovelace", email, "password12"))))
        .andExpect(status().isCreated());
  }
}
```

Create `CustomerAdviceTest.java`:

```java
package com.larkspur.customers;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class CustomerAdviceTest {
  private final ApiErrors advice = new ApiErrors();

  @Test
  void unexpectedIs500() {
    ResponseEntity<Map<String, String>> response = advice.unexpected(new IllegalStateException("boom"));
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    assertThat(response.getBody()).containsEntry("message", "Customers could not complete that request.");
  }
}
```

- [ ] **Step 2: Run the tests and verify they fail**

```bash
mvn -pl customers-service -am -Dtest=CustomerAccessTest,CustomerAdviceTest test
```

Expected: compilation failure because `org.junit.jupiter.api` and `com.larkspur.testing` are not on the customers test classpath.

- [ ] **Step 3: Add the test dependencies**

Append these dependencies inside `Middleware/customers-service/pom.xml` `<dependencies>`:

```xml
<dependency>
  <groupId>com.larkspur</groupId>
  <artifactId>services-test-common</artifactId>
  <version>${project.version}</version>
  <scope>test</scope>
</dependency>
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-test</artifactId>
  <scope>test</scope>
</dependency>
<dependency>
  <groupId>com.h2database</groupId>
  <artifactId>h2</artifactId>
  <scope>test</scope>
</dependency>
```

Do not change `CustomerApi`, `CustomersApplication`, or `application.yml`.

- [ ] **Step 4: Run the tests and verify they pass**

```bash
mvn -pl customers-service -am -Dtest=CustomerAccessTest,CustomerAdviceTest test
```

Expected: `BUILD SUCCESS`, both classes, failures 0.

- [ ] **Step 5: Commit**

```bash
git add Middleware/customers-service/pom.xml Middleware/customers-service/src/test
git commit -m "Test customer signup, signin, and the customers error statuses."
```

---

### Task 3: Catalog products

**Files:**
- Modify: `Middleware/product-catalog-service/pom.xml`
- Create: `Middleware/product-catalog-service/src/test/java/com/larkspur/catalog/ProductApiTest.java`
- Create: `Middleware/product-catalog-service/src/test/java/com/larkspur/catalog/CatalogAdviceTest.java`
- Test: `mvn -pl product-catalog-service -am -Dtest=ProductApiTest,CatalogAdviceTest test`

**Interfaces:**
- Consumes: `@CommerceServiceTest`, `Datasets`, `DownstreamServers.server("inventoryRestClient")`. `POST /api/products`, `GET /api/products/{id}`, `PUT /api/products/{id}`, `DELETE /api/products/{id}`. `InventoryClient.sync` calls `PUT /api/stock/{sku}`. Package-visible `ApiErrors.badValue()` and `ApiErrors.unexpected(Exception)`.
- Produces: catalog create, read, update, delete, validation, the stock-sync call, and the catalog rows in the spec error table.

- [ ] **Step 1: Write the failing tests**

Create `ProductApiTest.java`:

```java
package com.larkspur.catalog;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.larkspur.testing.CommerceServiceTest;
import com.larkspur.testing.Datasets;
import com.larkspur.testing.DownstreamServers;
import java.net.ConnectException;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@CommerceServiceTest
class ProductApiTest {
  @Autowired MockMvc mvc;
  @Autowired ObjectMapper json;

  @BeforeEach
  void resetDownstream() {
    DownstreamServers.reset();
  }

  @Test
  void createReadUpdateAndDelete() throws Exception {
    String sku = "SKU-" + UUID.randomUUID();
    MockRestServiceServer inventory = DownstreamServers.server("inventoryRestClient");
    inventory.expect(requestTo("http://inventory.test/api/stock/" + sku))
        .andExpect(method(HttpMethod.PUT))
        .andRespond(withSuccess());
    MvcResult created = mvc.perform(post("/api/products")
            .contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsBytes(Datasets.product(sku, "Stoneware Carafe"))))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.sku").value(sku))
        .andReturn();
    inventory.verify();
    String id = json.readTree(created.getResponse().getContentAsString()).get("id").asText();

    mvc.perform(get("/api/products/" + id))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("Stoneware Carafe"));

    inventory.reset();
    inventory.expect(requestTo("http://inventory.test/api/stock/" + sku))
        .andExpect(method(HttpMethod.PUT))
        .andRespond(withSuccess());
    mvc.perform(put("/api/products/" + id)
            .contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsBytes(Datasets.product(sku, "Updated Carafe"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("Updated Carafe"));

    mvc.perform(delete("/api/products/" + id))
        .andExpect(status().isNoContent());
    mvc.perform(get("/api/products/" + id))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").value("Product not found."));
  }

  @Test
  void unknownProductIs404() throws Exception {
    mvc.perform(get("/api/products/" + UUID.randomUUID()))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").value("Product not found."));
  }

  @Test
  void duplicateSkuIs409() throws Exception {
    String sku = "DUP-" + UUID.randomUUID();
    MockRestServiceServer inventory = DownstreamServers.server("inventoryRestClient");
    inventory.expect(requestTo("http://inventory.test/api/stock/" + sku)).andRespond(withSuccess());
    mvc.perform(post("/api/products")
            .contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsBytes(Datasets.product(sku, "First"))))
        .andExpect(status().isCreated());
    inventory.reset();
    inventory.expect(requestTo("http://inventory.test/api/stock/" + sku)).andRespond(withSuccess());
    mvc.perform(post("/api/products")
            .contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsBytes(Datasets.product(sku, "Second"))))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.message").value("A product with that SKU already exists."));
  }

  @Test
  void blankSkuIs400() throws Exception {
    mvc.perform(post("/api/products")
            .contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsBytes(Datasets.product("  ", "Carafe"))))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("sku must not be blank"));
  }

  @Test
  void oversizedSkuIs400() throws Exception {
    mvc.perform(post("/api/products")
            .contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsBytes(Datasets.product("S".repeat(41), "Carafe"))))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("sku size must be between 0 and 40"));
  }

  @Test
  void inventoryFailureStillCreatesTheProduct() throws Exception {
    String sku = "SYNC-" + UUID.randomUUID().toString().substring(0, 8);
    DownstreamServers.server("inventoryRestClient")
        .expect(requestTo("http://inventory.test/api/stock/" + sku))
        .andRespond(withException(new ConnectException("down")));
    mvc.perform(post("/api/products")
            .contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsBytes(Datasets.product(sku, "Carafe"))))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.sku").value(sku));
  }
}
```

Create `CatalogAdviceTest.java`:

```java
package com.larkspur.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class CatalogAdviceTest {
  private final ApiErrors advice = new ApiErrors();

  @Test
  void invalidValueIs400() {
    ResponseEntity<Map<String, String>> response = advice.badValue();
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody()).containsEntry("message", "A field has an invalid value.");
  }

  @Test
  void unexpectedIs500() {
    ResponseEntity<Map<String, String>> response = advice.unexpected(new IllegalStateException("boom"));
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    assertThat(response.getBody()).containsEntry("message", "Product catalog could not complete that request.");
  }
}
```

- [ ] **Step 2: Run the tests and verify they fail**

```bash
mvn -pl product-catalog-service -am -Dtest=ProductApiTest,CatalogAdviceTest test
```

Expected: compilation failure because the catalog test classpath has no JUnit and no `com.larkspur.testing`.

- [ ] **Step 3: Add the test dependencies**

Append to `Middleware/product-catalog-service/pom.xml` `<dependencies>`:

```xml
<dependency>
  <groupId>com.larkspur</groupId>
  <artifactId>services-test-common</artifactId>
  <version>${project.version}</version>
  <scope>test</scope>
</dependency>
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-test</artifactId>
  <scope>test</scope>
</dependency>
<dependency>
  <groupId>com.h2database</groupId>
  <artifactId>h2</artifactId>
  <scope>test</scope>
</dependency>
```

Do not change `CatalogApplication`, `InventoryClient`, or `ProductApi`.

- [ ] **Step 4: Run the tests and verify they pass**

```bash
mvn -pl product-catalog-service -am -Dtest=ProductApiTest,CatalogAdviceTest test
```

Expected: `BUILD SUCCESS`. The catalog seed's stock calls during startup are swallowed by `InventoryClient.sync`. `@BeforeEach` resets the mock server before each test.

- [ ] **Step 5: Commit**

```bash
git add Middleware/product-catalog-service/pom.xml Middleware/product-catalog-service/src/test
git commit -m "Test catalog products and the inventory stock-sync call."
```

---

### Task 4: Offers, catalog lookup, and Redis

**Files:**
- Modify: `Middleware/offers-service/pom.xml`
- Create: `Middleware/offers-service/src/test/java/com/larkspur/offers/OfferApiTest.java`
- Create: `Middleware/offers-service/src/test/java/com/larkspur/offers/OfferAdviceTest.java`
- Test: `mvn -pl offers-service -am -Dtest=OfferApiTest,OfferAdviceTest test`

**Interfaces:**
- Consumes: `EmbeddedRedis.start()`, `EmbeddedRedis.port()`, `DownstreamServers.server("catalogRestClient")`, `Datasets.offer`. `POST /api/offers`, `PUT /api/offers/{id}`, `GET /api/offers/{id}`, `GET /api/offers`, `GET /api/offers/active`. Cache key `catalog:product:{id}`, TTL 60 seconds. Package-visible `ApiErrors.badValue()` and `ApiErrors.unexpected(Exception)`.
- Produces: offer create and read, the latest-100 list, the active filter, one catalog lookup cached in Redis, and the offers rows in the spec error table.

- [ ] **Step 1: Write the failing tests**

Create `OfferApiTest.java`:

```java
package com.larkspur.offers;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.larkspur.testing.CommerceServiceTest;
import com.larkspur.testing.Datasets;
import com.larkspur.testing.DownstreamServers;
import com.larkspur.testing.EmbeddedRedis;
import java.net.ConnectException;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@CommerceServiceTest
class OfferApiTest {
  @DynamicPropertySource
  static void redis(DynamicPropertyRegistry registry) {
    EmbeddedRedis.start();
    registry.add("spring.data.redis.host", () -> "127.0.0.1");
    registry.add("spring.data.redis.port", EmbeddedRedis::port);
  }

  @Autowired MockMvc mvc;
  @Autowired ObjectMapper json;
  @Autowired StringRedisTemplate redisTemplate;

  @BeforeEach
  void resetDownstream() {
    DownstreamServers.reset();
  }

  @Test
  void createReadsCatalogOnceAndTheNextReadUsesRedis() throws Exception {
    UUID productId = UUID.randomUUID();
    String code = "ONE-" + UUID.randomUUID().toString().substring(0, 8);
    MockRestServiceServer catalog = DownstreamServers.server("catalogRestClient");
    catalog.expect(requestTo(org.hamcrest.Matchers.containsString("/api/products/lookup")))
        .andRespond(withSuccess(productJson(productId, "HED-ONE", "Carafe"), MediaType.APPLICATION_JSON));

    MvcResult created = mvc.perform(post("/api/offers")
            .contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsBytes(Datasets.offer(code, "Morning", "ACTIVE", null, null, List.of(productId)))))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.products[0].sku").value("HED-ONE"))
        .andReturn();
    String id = json.readTree(created.getResponse().getContentAsString()).get("id").asText();
    catalog.verify();

    Long ttl = redisTemplate.getExpire("catalog:product:" + productId);
    org.assertj.core.api.Assertions.assertThat(ttl).isBetween(1L, 60L);

    mvc.perform(get("/api/offers/" + id))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.products[0].id").value(productId.toString()));
    catalog.verify();
  }

  @Test
  void listReturnsTheCreatedOfferAndActiveFiltersByUtcWindow() throws Exception {
    LocalDate today = LocalDate.now(ZoneOffset.UTC);
    UUID keptId = UUID.randomUUID();
    UUID droppedId = UUID.randomUUID();
    stubLookup(keptId);
    MvcResult kept = mvc.perform(post("/api/offers")
            .contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsBytes(Datasets.offer(
                "ACT-" + UUID.randomUUID().toString().substring(0, 8),
                "Open", "ACTIVE", today, today, List.of(keptId)))))
        .andExpect(status().isCreated())
        .andReturn();
    stubLookup(droppedId);
    MvcResult dropped = mvc.perform(post("/api/offers")
            .contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsBytes(Datasets.offer(
                "END-" + UUID.randomUUID().toString().substring(0, 8),
                "Closed", "ACTIVE", today.minusDays(10), today.minusDays(1), List.of(droppedId)))))
        .andExpect(status().isCreated())
        .andReturn();
    String keptOffer = json.readTree(kept.getResponse().getContentAsString()).get("id").asText();
    String droppedOffer = json.readTree(dropped.getResponse().getContentAsString()).get("id").asText();

    mvc.perform(get("/api/offers"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[?(@.id == '" + keptOffer + "')]").exists());
    mvc.perform(get("/api/offers/active"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[?(@.id == '" + keptOffer + "')]").exists())
        .andExpect(jsonPath("$[?(@.id == '" + droppedOffer + "')]").doesNotExist());
  }

  @Test
  void unknownOfferIs404() throws Exception {
    mvc.perform(get("/api/offers/" + UUID.randomUUID()))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").value("Offer not found."));
  }

  @Test
  void endBeforeStartIs400() throws Exception {
    LocalDate today = LocalDate.now(ZoneOffset.UTC);
    mvc.perform(post("/api/offers")
            .contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsBytes(Datasets.offer(
                "DATE-" + UUID.randomUUID().toString().substring(0, 8),
                "Backwards", "ACTIVE", today, today.minusDays(1), List.of(UUID.randomUUID())))))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("The end date is before the start date."));
  }

  @Test
  void repeatedProductIs400() throws Exception {
    UUID productId = UUID.randomUUID();
    mvc.perform(post("/api/offers")
            .contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsBytes(Datasets.offer(
                "REP-" + UUID.randomUUID().toString().substring(0, 8),
                "Repeat", "ACTIVE", null, null, List.of(productId, productId)))))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("Choose each product once."));
  }

  @Test
  void missingCatalogProductIs400() throws Exception {
    UUID productId = UUID.randomUUID();
    DownstreamServers.server("catalogRestClient")
        .expect(requestTo(org.hamcrest.Matchers.containsString("/api/products/lookup")))
        .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));
    mvc.perform(post("/api/offers")
            .contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsBytes(Datasets.offer(
                "MISS-" + UUID.randomUUID().toString().substring(0, 8),
                "Missing", "ACTIVE", null, null, List.of(productId)))))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("One or more products are not in the catalog."));
  }

  @Test
  void catalogFailureOnCreateAndUpdateIs502() throws Exception {
    UUID first = UUID.randomUUID();
    UUID second = UUID.randomUUID();
    stubLookup(first);
    MvcResult created = mvc.perform(post("/api/offers")
            .contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsBytes(Datasets.offer(
                "UP-" + UUID.randomUUID().toString().substring(0, 8),
                "Editable", "ACTIVE", null, null, List.of(first)))))
        .andExpect(status().isCreated())
        .andReturn();
    String id = json.readTree(created.getResponse().getContentAsString()).get("id").asText();

    DownstreamServers.reset();
    DownstreamServers.server("catalogRestClient")
        .expect(requestTo(org.hamcrest.Matchers.containsString("/api/products/lookup")))
        .andRespond(withException(new ConnectException("down")));
    mvc.perform(post("/api/offers")
            .contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsBytes(Datasets.offer(
                "DOWN-" + UUID.randomUUID().toString().substring(0, 8),
                "Unreachable", "ACTIVE", null, null, List.of(second)))))
        .andExpect(status().isBadGateway())
        .andExpect(jsonPath("$.message").value("Product catalog is unreachable."));

    DownstreamServers.reset();
    DownstreamServers.server("catalogRestClient")
        .expect(requestTo(org.hamcrest.Matchers.containsString("/api/products/lookup")))
        .andRespond(withException(new ConnectException("down")));
    mvc.perform(put("/api/offers/" + id)
            .contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsBytes(Datasets.offer(
                "UP2-" + UUID.randomUUID().toString().substring(0, 8),
                "Edited", "ACTIVE", null, null, List.of(second)))))
        .andExpect(status().isBadGateway())
        .andExpect(jsonPath("$.message").value("Product catalog is unreachable."));
  }

  @Test
  void catalogFailureOnListReturnsEmptySummaries() throws Exception {
    Set<String> cached = redisTemplate.keys("catalog:product:*");
    if (cached != null && !cached.isEmpty()) {
      redisTemplate.delete(cached);
    }
    DownstreamServers.server("catalogRestClient")
        .expect(requestTo(org.hamcrest.Matchers.containsString("/api/products/lookup")))
        .andRespond(withException(new ConnectException("down")));
    mvc.perform(get("/api/offers"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").exists())
        .andExpect(jsonPath("$[0].products").isEmpty());
  }

  @Test
  void duplicateCodeIs409() throws Exception {
    String code = "DUP-" + UUID.randomUUID().toString().substring(0, 8);
    UUID productId = UUID.randomUUID();
    stubLookup(productId);
    mvc.perform(post("/api/offers")
            .contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsBytes(Datasets.offer(code, "First", "ACTIVE", null, null, List.of(productId)))))
        .andExpect(status().isCreated());
    DownstreamServers.reset();
    stubLookup(productId);
    mvc.perform(post("/api/offers")
            .contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsBytes(Datasets.offer(code, "Second", "ACTIVE", null, null, List.of(productId)))))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.message").value("An offer with that code already exists."));
  }

  private void stubLookup(UUID productId) {
    DownstreamServers.server("catalogRestClient")
        .expect(requestTo(org.hamcrest.Matchers.containsString("/api/products/lookup")))
        .andRespond(withSuccess(productJson(productId, "HED-" + productId.toString().substring(0, 8), "Carafe"), MediaType.APPLICATION_JSON));
  }

  private static String productJson(UUID id, String sku, String name) {
    return "[{\"id\":\"" + id + "\",\"sku\":\"" + sku + "\",\"name\":\"" + name + "\",\"category\":\"Home\",\"unitPrice\":48.00}]";
  }
}
```

`GET /api/offers` after the seed has rows, so the list-failure test asserts the first row's `products` array is empty. The seed writes offers without calling the catalog. The update uses a product id that was not cached by create, so the catalog stub is actually called.

Create `OfferAdviceTest.java`:

```java
package com.larkspur.offers;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class OfferAdviceTest {
  private final ApiErrors advice = new ApiErrors();

  @Test
  void invalidValueIs400() {
    ResponseEntity<Map<String, String>> response = advice.badValue();
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody()).containsEntry("message", "A field has an invalid value.");
  }

  @Test
  void unexpectedIs500() {
    ResponseEntity<Map<String, String>> response = advice.unexpected(new IllegalStateException("boom"));
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    assertThat(response.getBody()).containsEntry("message", "Offers could not complete that request.");
  }
}
```

- [ ] **Step 2: Run the tests and verify they fail**

```bash
mvn -pl offers-service -am -Dtest=OfferApiTest,OfferAdviceTest test
```

Expected: compilation failure because the offers test classpath has no `com.larkspur.testing`.

- [ ] **Step 3: Add the test dependencies**

Append to `Middleware/offers-service/pom.xml` `<dependencies>`:

```xml
<dependency>
  <groupId>com.larkspur</groupId>
  <artifactId>services-test-common</artifactId>
  <version>${project.version}</version>
  <scope>test</scope>
</dependency>
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-test</artifactId>
  <scope>test</scope>
</dependency>
<dependency>
  <groupId>com.h2database</groupId>
  <artifactId>h2</artifactId>
  <scope>test</scope>
</dependency>
<dependency>
  <groupId>com.github.codemonstur</groupId>
  <artifactId>embedded-redis</artifactId>
  <scope>test</scope>
</dependency>
```

Do not change `OffersApplication`, `CatalogClient`, or `OfferApi`.

- [ ] **Step 4: Run the tests and verify they pass**

```bash
mvn -pl offers-service -am -Dtest=OfferApiTest,OfferAdviceTest test
```

Expected: `BUILD SUCCESS`. Redis is the embedded server, not the Compose Redis.

- [ ] **Step 5: Commit**

```bash
git add Middleware/offers-service/pom.xml Middleware/offers-service/src/test
git commit -m "Test offer lookup, the Redis catalog cache, and offer error statuses."
```

---

### Task 5: Purchase history

**Files:**
- Modify: `Middleware/orders-service/pom.xml`
- Create: `Middleware/orders-service/src/test/java/com/larkspur/orders/PurchaseHistoryTest.java`
- Create: `Middleware/orders-service/src/test/java/com/larkspur/orders/OrderAdviceTest.java`
- Test: `mvn -pl orders-service -am -Dtest=PurchaseHistoryTest,OrderAdviceTest test`

**Interfaces:**
- Consumes: `ShopperTokens.bearer(UUID, String, String)`, `DownstreamServers.server("offersClient")`, `DownstreamServers.server("inventoryClient")`. `POST /api/orders` body `{ "offerId" }`, `GET /api/orders/mine`, `GET /api/orders`, `GET /api/orders/{id}`. Package-visible `ApiErrors.badValue()` and `ApiErrors.unexpected(Exception)`.
- Produces: an `HD-` `PLACED` order whose `totalAmount` is `offerPrice`, quantity-1 lines, a reservation whose `orderRef` is the order number, mine scoped to the token subject, and the orders rows in the spec error table.

- [ ] **Step 1: Write the failing tests**

Create `PurchaseHistoryTest.java`:

```java
package com.larkspur.orders;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withNoContent;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.larkspur.testing.CommerceServiceTest;
import com.larkspur.testing.DownstreamServers;
import com.larkspur.testing.ShopperTokens;
import java.net.ConnectException;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.MockMvc;

@CommerceServiceTest
class PurchaseHistoryTest {
  @Autowired MockMvc mvc;
  @Autowired ObjectMapper json;

  @BeforeEach
  void resetDownstream() {
    DownstreamServers.reset();
  }

  @Test
  void purchasePlacesAnOrderAndMineReturnsItForThatCustomer() throws Exception {
    UUID customerId = UUID.randomUUID();
    UUID otherId = UUID.randomUUID();
    UUID offerId = UUID.randomUUID();
    UUID productId = UUID.randomUUID();
    String token = ShopperTokens.bearer(customerId, "Ada Lovelace", "ada@example.test");
    stubOffer(offerId, "ACTIVE", productId, "HED-CARAFE", "Carafe");
    DownstreamServers.server("inventoryClient")
        .expect(requestTo("http://inventory.test/api/reservations"))
        .andExpect(method(HttpMethod.POST))
        .andExpect(content().string(org.hamcrest.Matchers.containsString("HD-")))
        .andExpect(content().string(org.hamcrest.Matchers.containsString("\"qty\":1")))
        .andRespond(withNoContent());

    mvc.perform(post("/api/orders")
            .header("Authorization", token)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"offerId\":\"" + offerId + "\"}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.orderNumber").value(org.hamcrest.Matchers.startsWith("HD-")))
        .andExpect(jsonPath("$.status").value("PLACED"))
        .andExpect(jsonPath("$.customerId").value(customerId.toString()))
        .andExpect(jsonPath("$.customerName").value("Ada Lovelace"))
        .andExpect(jsonPath("$.customerEmail").value("ada@example.test"))
        .andExpect(jsonPath("$.totalAmount").value(139.00))
        .andExpect(jsonPath("$.lines[0].quantity").value(1))
        .andExpect(jsonPath("$.lines[0].sku").value("HED-CARAFE"));

    mvc.perform(get("/api/orders/mine").header("Authorization", token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[?(@.customerId == '" + customerId + "')]").exists());
    mvc.perform(get("/api/orders/mine").header("Authorization", ShopperTokens.bearer(otherId, "Other", "other@example.test")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[?(@.customerId == '" + otherId + "')]").doesNotExist());
  }

  @Test
  void missingAndInvalidTokenAre401() throws Exception {
    mvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content("{\"offerId\":\"" + UUID.randomUUID() + "\"}"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.message").value("Sign in to purchase an offer."));
    mvc.perform(post("/api/orders")
            .header("Authorization", "Bearer not-a-token")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"offerId\":\"" + UUID.randomUUID() + "\"}"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.message").value("Sign in to purchase an offer."));
  }

  @Test
  void missingOfferIs404() throws Exception {
    UUID offerId = UUID.randomUUID();
    DownstreamServers.server("offersClient")
        .expect(requestTo("http://offers.test/api/offers/" + offerId))
        .andRespond(withStatus(HttpStatus.NOT_FOUND));
    purchase(offerId)
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").value("Offer not found."));
  }

  @Test
  void offersHttpErrorIs502() throws Exception {
    UUID offerId = UUID.randomUUID();
    DownstreamServers.server("offersClient")
        .expect(requestTo("http://offers.test/api/offers/" + offerId))
        .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));
    purchase(offerId)
        .andExpect(status().isBadGateway())
        .andExpect(jsonPath("$.message").value("Offers service rejected the lookup."));
  }

  @Test
  void emptyOfferBodyIs502() throws Exception {
    UUID offerId = UUID.randomUUID();
    DownstreamServers.server("offersClient")
        .expect(requestTo("http://offers.test/api/offers/" + offerId))
        .andRespond(withSuccess("", MediaType.APPLICATION_JSON));
    purchase(offerId)
        .andExpect(status().isBadGateway())
        .andExpect(jsonPath("$.message").value("Offers service returned an empty response."));
  }

  @Test
  void offersConnectionDropIs502() throws Exception {
    UUID offerId = UUID.randomUUID();
    DownstreamServers.server("offersClient")
        .expect(requestTo("http://offers.test/api/offers/" + offerId))
        .andRespond(withException(new ConnectException("down")));
    purchase(offerId)
        .andExpect(status().isBadGateway())
        .andExpect(jsonPath("$.message").value("Offers service is unreachable."));
  }

  @Test
  void inactiveOfferSavesNoRow() throws Exception {
    Set<String> before = orderNumbers();
    UUID offerId = UUID.randomUUID();
    stubOffer(offerId, "EXPIRED", UUID.randomUUID(), "HED-OLD", "Old");
    purchase(offerId)
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.message").value("This offer is not available to purchase."));
    org.assertj.core.api.Assertions.assertThat(orderNumbers()).isEqualTo(before);
  }

  @Test
  void offerWithoutProductsSavesNoRow() throws Exception {
    Set<String> before = orderNumbers();
    UUID offerId = UUID.randomUUID();
    DownstreamServers.server("offersClient")
        .expect(requestTo("http://offers.test/api/offers/" + offerId))
        .andRespond(withSuccess(
            "{\"id\":\"" + offerId + "\",\"name\":\"Empty\",\"status\":\"ACTIVE\",\"offerPrice\":139.00,\"products\":[]}",
            MediaType.APPLICATION_JSON));
    purchase(offerId)
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.message").value("This offer does not include any products."));
    org.assertj.core.api.Assertions.assertThat(orderNumbers()).isEqualTo(before);
  }

  @Test
  void inventoryConflictDeletesTheOrderAndUsesTheBodyMessage() throws Exception {
    Set<String> before = orderNumbers();
    UUID offerId = UUID.randomUUID();
    stubOffer(offerId, "ACTIVE", UUID.randomUUID(), "HED-CUP", "Cup");
    DownstreamServers.server("inventoryClient")
        .expect(requestTo("http://inventory.test/api/reservations"))
        .andRespond(withStatus(HttpStatus.CONFLICT)
            .contentType(MediaType.APPLICATION_JSON)
            .body("{\"message\":\"No stock record for HED-CUP.\"}"));
    purchase(offerId)
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.message").value("No stock record for HED-CUP."));
    org.assertj.core.api.Assertions.assertThat(orderNumbers()).isEqualTo(before);
  }

  @Test
  void inventoryConflictWithoutAMessageUsesTheFallback() throws Exception {
    Set<String> before = orderNumbers();
    UUID offerId = UUID.randomUUID();
    stubOffer(offerId, "ACTIVE", UUID.randomUUID(), "HED-CUP", "Cup");
    DownstreamServers.server("inventoryClient")
        .expect(requestTo("http://inventory.test/api/reservations"))
        .andRespond(withStatus(HttpStatus.CONFLICT).contentType(MediaType.APPLICATION_JSON).body("{\"status\":409}"));
    purchase(offerId)
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.message").value("Inventory could not reserve this offer."));
    org.assertj.core.api.Assertions.assertThat(orderNumbers()).isEqualTo(before);
  }

  @Test
  void inventoryConnectionDropDeletesTheOrder() throws Exception {
    Set<String> before = orderNumbers();
    UUID offerId = UUID.randomUUID();
    stubOffer(offerId, "ACTIVE", UUID.randomUUID(), "HED-CUP", "Cup");
    DownstreamServers.server("inventoryClient")
        .expect(requestTo("http://inventory.test/api/reservations"))
        .andRespond(withException(new ConnectException("down")));
    purchase(offerId)
        .andExpect(status().isBadGateway())
        .andExpect(jsonPath("$.message").value("Inventory service is unreachable."));
    org.assertj.core.api.Assertions.assertThat(orderNumbers()).isEqualTo(before);
  }

  @Test
  void unknownOrderIs404() throws Exception {
    mvc.perform(get("/api/orders/" + UUID.randomUUID()))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").value("Order not found."));
  }

  private org.springframework.test.web.servlet.ResultActions purchase(UUID offerId) throws Exception {
    return mvc.perform(post("/api/orders")
        .header("Authorization", ShopperTokens.bearer(UUID.randomUUID(), "Ada Lovelace", "ada@example.test"))
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"offerId\":\"" + offerId + "\"}"));
  }

  private void stubOffer(UUID offerId, String status, UUID productId, String sku, String name) {
    String body = "{\"id\":\"" + offerId + "\",\"name\":\"Morning\",\"status\":\"" + status
        + "\",\"offerPrice\":139.00,\"products\":[{\"id\":\"" + productId + "\",\"sku\":\"" + sku
        + "\",\"name\":\"" + name + "\",\"unitPrice\":48.00}]}";
    DownstreamServers.server("offersClient")
        .expect(requestTo("http://offers.test/api/offers/" + offerId))
        .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));
  }

  private Set<String> orderNumbers() throws Exception {
    JsonNode rows = json.readTree(mvc.perform(get("/api/orders")).andExpect(status().isOk())
        .andReturn().getResponse().getContentAsString());
    Set<String> numbers = new HashSet<>();
    rows.forEach(row -> numbers.add(row.get("orderNumber").asText()));
    return numbers;
  }
}
```

The order number is generated inside the service, so a failed purchase is checked by comparing `GET /api/orders` order numbers before and after. An unchanged set means the new number is absent.

Create `OrderAdviceTest.java`:

```java
package com.larkspur.orders;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class OrderAdviceTest {
  private final ApiErrors advice = new ApiErrors();

  @Test
  void invalidValueIs400() {
    ResponseEntity<Map<String, String>> response = advice.badValue();
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody()).containsEntry("message", "A field has an invalid value.");
  }

  @Test
  void unexpectedIs500() {
    ResponseEntity<Map<String, String>> response = advice.unexpected(new IllegalStateException("boom"));
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    assertThat(response.getBody()).containsEntry("message", "Orders could not complete that request.");
  }
}
```

- [ ] **Step 2: Run the tests and verify they fail**

```bash
mvn -pl orders-service -am -Dtest=PurchaseHistoryTest,OrderAdviceTest test
```

Expected: compilation failure because the orders test classpath has no `com.larkspur.testing`.

- [ ] **Step 3: Add the test dependencies**

Append to `Middleware/orders-service/pom.xml` `<dependencies>`:

```xml
<dependency>
  <groupId>com.larkspur</groupId>
  <artifactId>services-test-common</artifactId>
  <version>${project.version}</version>
  <scope>test</scope>
</dependency>
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-test</artifactId>
  <scope>test</scope>
</dependency>
<dependency>
  <groupId>com.h2database</groupId>
  <artifactId>h2</artifactId>
  <scope>test</scope>
</dependency>
```

Do not change `OrdersApplication`, `OrderApi`, or `application.yml`.

- [ ] **Step 4: Run the tests and verify they pass**

```bash
mvn -pl orders-service -am -Dtest=PurchaseHistoryTest,OrderAdviceTest test
```

Expected: `BUILD SUCCESS`.

- [ ] **Step 5: Commit**

```bash
git add Middleware/orders-service/pom.xml Middleware/orders-service/src/test
git commit -m "Test purchase, order history, and the orders error statuses."
```

---

### Task 6: Inventory reservation

**Files:**
- Modify: `Middleware/inventory-service/pom.xml`
- Create: `Middleware/inventory-service/src/test/java/com/larkspur/inventory/ReservationTest.java`
- Create: `Middleware/inventory-service/src/test/java/com/larkspur/inventory/InventoryAdviceTest.java`
- Test: `mvn -pl inventory-service -am -Dtest=ReservationTest,InventoryAdviceTest test`

**Interfaces:**
- Consumes: `@CommerceServiceTest` and `Datasets.stock`. `PUT /api/stock/{sku}`, `POST /api/reservations`, `GET /api/stock`. Package-visible `ApiErrors.conflict()` and `ApiErrors.unexpected(Exception)`.
- Produces: upper-case SKU storage, default available quantity 100, a reservation that moves quantity from available to reserved, and the inventory rows in the spec error table.

- [ ] **Step 1: Write the failing tests**

Create `ReservationTest.java`:

```java
package com.larkspur.inventory;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.larkspur.testing.CommerceServiceTest;
import com.larkspur.testing.Datasets;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@CommerceServiceTest
class ReservationTest {
  @Autowired MockMvc mvc;
  @Autowired ObjectMapper json;

  @Test
  void upsertUppercasesSkuAndDefaultsQuantity() throws Exception {
    String sku = "ab-" + UUID.randomUUID().toString().substring(0, 8);
    mvc.perform(put("/api/stock/" + sku)
            .contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsBytes(Datasets.stock("Carafe", null))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.sku").value(sku.toUpperCase()))
        .andExpect(jsonPath("$.availableQty").value(100))
        .andExpect(jsonPath("$.reservedQty").value(0));
  }

  @Test
  void reservationMovesQuantity() throws Exception {
    String sku = "MOVE-" + UUID.randomUUID().toString().substring(0, 8);
    mvc.perform(put("/api/stock/" + sku)
            .contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsBytes(Datasets.stock("Carafe", 5))))
        .andExpect(status().isOk());
    mvc.perform(post("/api/reservations")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"orderRef\":\"HD-1\",\"lines\":[{\"sku\":\"" + sku.toLowerCase() + "\",\"qty\":2}]}"))
        .andExpect(status().isNoContent());
    mvc.perform(get("/api/stock"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[?(@.sku == '" + sku.toUpperCase() + "')].availableQty").value(org.hamcrest.Matchers.hasItem(3)))
        .andExpect(jsonPath("$[?(@.sku == '" + sku.toUpperCase() + "')].reservedQty").value(org.hamcrest.Matchers.hasItem(2)));
  }

  @Test
  void missingSkuIs409() throws Exception {
    String sku = "MISSING-" + UUID.randomUUID().toString().substring(0, 8);
    mvc.perform(post("/api/reservations")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"orderRef\":\"HD-2\",\"lines\":[{\"sku\":\"" + sku + "\",\"qty\":1}]}"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.message").value("No stock record for " + sku + "."));
  }

  @Test
  void insufficientStockIs409() throws Exception {
    String sku = "LOW-" + UUID.randomUUID().toString().substring(0, 8);
    mvc.perform(put("/api/stock/" + sku)
            .contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsBytes(Datasets.stock("Linen Runner", 1))))
        .andExpect(status().isOk());
    mvc.perform(post("/api/reservations")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"orderRef\":\"HD-3\",\"lines\":[{\"sku\":\"" + sku + "\",\"qty\":2}]}"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.message").value("Linen Runner does not have enough stock."));
  }

  @Test
  void blankProductNameIs400() throws Exception {
    mvc.perform(put("/api/stock/NAME-" + UUID.randomUUID().toString().substring(0, 8))
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"productName\":\" \"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("productName must not be blank"));
  }

  @Test
  void blankSkuIs400() throws Exception {
    mvc.perform(post("/api/reservations")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"orderRef\":\"HD-4\",\"lines\":[{\"sku\":\" \",\"qty\":1}]}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("sku must not be blank"));
  }

  @Test
  void quantityBelowOneIs400() throws Exception {
    mvc.perform(post("/api/reservations")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"orderRef\":\"HD-5\",\"lines\":[{\"sku\":\"CUP\",\"qty\":0}]}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("qty must be greater than or equal to 1"));
  }

  @Test
  void emptyLinesAre400() throws Exception {
    mvc.perform(post("/api/reservations")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"orderRef\":\"HD-6\",\"lines\":[]}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("lines must not be empty"));
  }
}
```

Create `InventoryAdviceTest.java`:

```java
package com.larkspur.inventory;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class InventoryAdviceTest {
  private final ApiErrors advice = new ApiErrors();

  @Test
  void duplicateStockIs409() {
    ResponseEntity<Map<String, String>> response = advice.conflict();
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(response.getBody()).containsEntry("message", "That stock record already exists.");
  }

  @Test
  void unexpectedIs500() {
    ResponseEntity<Map<String, String>> response = advice.unexpected(new IllegalStateException("boom"));
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    assertThat(response.getBody()).containsEntry("message", "Inventory could not complete that request.");
  }
}
```

- [ ] **Step 2: Run the tests and verify they fail**

```bash
mvn -pl inventory-service -am -Dtest=ReservationTest,InventoryAdviceTest test
```

Expected: compilation failure because the inventory test classpath has no `com.larkspur.testing`.

- [ ] **Step 3: Add the test dependencies**

Append to `Middleware/inventory-service/pom.xml` `<dependencies>`:

```xml
<dependency>
  <groupId>com.larkspur</groupId>
  <artifactId>services-test-common</artifactId>
  <version>${project.version}</version>
  <scope>test</scope>
</dependency>
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-test</artifactId>
  <scope>test</scope>
</dependency>
<dependency>
  <groupId>com.h2database</groupId>
  <artifactId>h2</artifactId>
  <scope>test</scope>
</dependency>
```

Do not change `InventoryApplication`, `InventoryApi`, or `application.yml`.

- [ ] **Step 4: Run the tests and verify they pass**

```bash
mvn -pl inventory-service -am -Dtest=ReservationTest,InventoryAdviceTest test
```

Expected: `BUILD SUCCESS`. `fault.response-delay-ms` stays 0, so reservation does not sleep.

- [ ] **Step 5: Commit**

```bash
git add Middleware/inventory-service/pom.xml Middleware/inventory-service/src/test
git commit -m "Test stock upsert, reservation, and the inventory error statuses."
```

---

### Task 7: Reactor test run

**Files:**
- Test: `Middleware` reactor `mvn test`

**Interfaces:**
- Consumes: Tasks 1 through 6.
- Produces: the pass command from the spec. Surefire runs the five service test classes, their advice tests, and `ObservabilityEnvironmentPostProcessorTest`.

- [ ] **Step 1: Run the reactor tests**

From `Middleware`:

```bash
mvn test
```

Expected: exit 0. `services-test-common` has no tests. The five commerce modules run the classes from Tasks 2 through 6. `service-observability` runs `ObservabilityEnvironmentPostProcessorTest` with 3 tests.

- [ ] **Step 2: Record a failure before changing code**

If a test fails, the command output is the evidence. Fix the test or the test-jar code the output names. Do not change a production `@Bean` to expose a `RestClient.Builder`. Do not start Compose, Postgres, Redis, Kafka, Jaeger, or Logstash to make the run pass.

- [ ] **Step 3: Commit only if this run required a fix**

```bash
git add Middleware
git commit -m "Make the commerce service tests pass together."
```

Skip the commit when Step 1 already exited 0 and the tree is clean.
