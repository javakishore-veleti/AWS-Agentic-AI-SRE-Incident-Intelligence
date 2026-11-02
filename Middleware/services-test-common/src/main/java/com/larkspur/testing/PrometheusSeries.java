package com.larkspur.testing;

import static org.assertj.core.api.Assertions.assertThat;

public final class PrometheusSeries {
  private PrometheusSeries() {
  }

  public static void assertCommerceSeries(String body) {
    assertThat(body).contains("http_server_requests_seconds_bucket");
    assertThat(body).containsPattern("status=\"[45][0-9][0-9]\"");
    assertThat(body).contains("hikaricp_connections");
  }
}
