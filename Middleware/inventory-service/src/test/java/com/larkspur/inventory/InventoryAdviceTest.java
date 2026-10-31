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
