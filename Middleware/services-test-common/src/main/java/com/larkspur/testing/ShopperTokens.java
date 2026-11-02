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
    return token(customerId, name, email, "CUSTOMER");
  }

  public static String admin() {
    return token(UUID.fromString("00000000-0000-4000-8000-000000000099"), "Heddle Desk", "desk@heddle.example", "ADMIN");
  }

  private static String token(UUID customerId, String name, String email, String role) {
    SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
    Instant now = Instant.now();
    String token = Jwts.builder()
        .subject(customerId.toString())
        .claim("email", email)
        .claim("name", name)
        .claim("role", role)
        .issuedAt(Date.from(now))
        .expiration(Date.from(now.plus(Duration.ofHours(1))))
        .signWith(key)
        .compact();
    return "Bearer " + token;
  }
}
