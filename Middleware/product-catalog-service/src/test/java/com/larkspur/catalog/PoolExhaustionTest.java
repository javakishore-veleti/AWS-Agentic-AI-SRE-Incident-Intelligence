package com.larkspur.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.larkspur.catalog.CatalogSpanCapture.Spans;
import com.larkspur.testing.CommerceServiceTest;
import com.larkspur.testing.ShopperTokens;
import io.opentelemetry.sdk.trace.data.SpanData;
import java.sql.Connection;
import java.util.List;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

@CommerceServiceTest
class PoolExhaustionTest {
  @Autowired MockMvc mvc;
  @Autowired DataSource dataSource;
  @Autowired Spans spans;

  @Test
  void exhaustedPoolFailsThenTheNextRequestSucceeds() throws Exception {
    spans.finished.clear();
    try (Connection held = dataSource.getConnection()) {
      assertThat(held).isNotNull();
      mvc.perform(get("/api/products").header("Authorization", ShopperTokens.admin()))
          .andExpect(status().is5xxServerError());
    }
    String series = mvc.perform(get("/actuator/prometheus"))
        .andExpect(status().isOk())
        .andReturn()
        .getResponse()
        .getContentAsString();
    assertThat(series).contains("hikaricp_connections_timeout");
    List<SpanData> finished = List.copyOf(spans.finished);
    assertThat(finished).isNotEmpty();
    assertThat(finished.stream()
        .mapToLong(span -> span.getEndEpochNanos() - span.getStartEpochNanos())
        .max()
        .orElse(0L)).isGreaterThan(300_000_000L);

    spans.finished.clear();
    mvc.perform(get("/api/products").header("Authorization", ShopperTokens.admin()))
        .andExpect(status().isOk());
  }
}
