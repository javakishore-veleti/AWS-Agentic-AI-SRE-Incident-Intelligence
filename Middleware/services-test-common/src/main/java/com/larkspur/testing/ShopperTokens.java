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
