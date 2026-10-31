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
