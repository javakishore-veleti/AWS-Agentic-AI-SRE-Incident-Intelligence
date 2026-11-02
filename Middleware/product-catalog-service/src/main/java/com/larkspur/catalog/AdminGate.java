package com.larkspur.catalog;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
class AdminGate {
  private final SecretKey key;

  AdminGate(@Value("${security.jwt.secret}") String secret) {
    this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
  }

  void require(String header) {
    if (header == null || !header.startsWith("Bearer ")) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sign in to continue.");
    }
    Claims claims;
    try {
      claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(header.substring(7)).getPayload();
    } catch (JwtException ex) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sign in to continue.");
    }
    if (!"ADMIN".equals(claims.get("role"))) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "An administrator must approve this request.");
    }
  }
}
