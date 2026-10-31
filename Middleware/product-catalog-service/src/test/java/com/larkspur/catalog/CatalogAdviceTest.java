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
