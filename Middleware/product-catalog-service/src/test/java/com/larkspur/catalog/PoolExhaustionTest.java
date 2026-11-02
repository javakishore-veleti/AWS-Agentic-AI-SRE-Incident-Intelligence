package com.larkspur.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.larkspur.testing.CommerceServiceTest;
import com.larkspur.testing.ShopperTokens;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.SpanProcessor;
import io.opentelemetry.sdk.trace.data.SpanData;
import java.sql.Connection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@CommerceServiceTest
@TestPropertySource(properties = {
    "spring.datasource.hikari.maximum-pool-size=1",
    "spring.datasource.hikari.minimum-idle=0",
    "spring.datasource.hikari.connection-timeout=400"
})
@Import(PoolExhaustionTest.Capture.class)
class PoolExhaustionTest {
  @Autowired MockMvc mvc;
  @Autowired DataSource dataSource;
  @Autowired Capture.Spans spans;

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
    assertThat(finished).as(finished.stream().map(SpanData::getName).toList().toString()).isNotEmpty();
    assertThat(finished.stream()
        .mapToLong(span -> span.getEndEpochNanos() - span.getStartEpochNanos())
        .max()
        .orElse(0L)).isGreaterThan(300_000_000L);

    spans.finished.clear();
    mvc.perform(get("/api/products").header("Authorization", ShopperTokens.admin()))
        .andExpect(status().isOk());
  }

  @TestConfiguration
  static class Capture {
    @Bean
    Spans poolSpans() {
      return new Spans();
    }

    static final class Spans implements SpanProcessor {
      final List<SpanData> finished = new CopyOnWriteArrayList<>();

      @Override
      public void onStart(Context parentContext, ReadWriteSpan span) {
      }

      @Override
      public boolean isStartRequired() {
        return false;
      }

      @Override
      public void onEnd(ReadableSpan span) {
        finished.add(span.toSpanData());
      }

      @Override
      public boolean isEndRequired() {
        return true;
      }

      @Override
      public CompletableResultCode shutdown() {
        return CompletableResultCode.ofSuccess();
      }

      @Override
      public CompletableResultCode forceFlush() {
        return CompletableResultCode.ofSuccess();
      }
    }
  }
}
