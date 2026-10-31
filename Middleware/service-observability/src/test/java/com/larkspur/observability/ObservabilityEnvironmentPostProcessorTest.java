package com.larkspur.observability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

class ObservabilityEnvironmentPostProcessorTest {
  private final ObservabilityEnvironmentPostProcessor processor = new ObservabilityEnvironmentPostProcessor();

  @Test
  void otlpEnablesTracingAndLeavesPrometheusOn() {
    StandardEnvironment environment = environment(Map.of(
        "observability.metrics.exporter", "prometheus",
        "observability.logs.exporter", "console",
        "observability.traces.exporter", "otlp",
        "observability.traces.otlp-endpoint", "http://127.0.0.1:4318/v1/traces"));

    processor.postProcessEnvironment(environment, new SpringApplication());

    assertThat(environment.getProperty("management.prometheus.metrics.export.enabled")).isEqualTo("true");
    assertThat(environment.getProperty("management.tracing.enabled")).isEqualTo("true");
    assertThat(environment.getProperty("management.otlp.tracing.endpoint"))
        .isEqualTo("http://127.0.0.1:4318/v1/traces");
    assertThat(environment.getActiveProfiles()).doesNotContain("elk");
  }

  @Test
  void logstashAddsTheElkProfileAndNoneDisablesExport() {
    StandardEnvironment environment = environment(Map.of(
        "observability.metrics.exporter", "none",
        "observability.logs.exporter", "logstash",
        "observability.traces.exporter", "none"));

    processor.postProcessEnvironment(environment, new SpringApplication());

    assertThat(environment.getProperty("management.prometheus.metrics.export.enabled")).isEqualTo("false");
    assertThat(environment.getProperty("management.tracing.enabled")).isEqualTo("false");
    assertThat(environment.getActiveProfiles()).contains("elk");
  }

  @Test
  void cloudExportersFailStartup() {
    StandardEnvironment environment = environment(Map.of(
        "observability.metrics.exporter", "cloudwatch",
        "observability.logs.exporter", "console",
        "observability.traces.exporter", "none"));

    assertThatThrownBy(() -> processor.postProcessEnvironment(environment, new SpringApplication()))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("not wired");
  }

  private static StandardEnvironment environment(Map<String, String> values) {
    StandardEnvironment environment = new StandardEnvironment();
    environment.getPropertySources().addFirst(new MapPropertySource("test", Map.copyOf(values)));
    return environment;
  }
}
