package com.larkspur.observability;

import java.util.HashMap;
import java.util.Map;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

/**
 * Maps observability exporter names onto Actuator, Micrometer, and Logback.
 * Runs after application config is loaded. Cloud exporter names fail startup
 * until that deployment adds them.
 */
public class ObservabilityEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {
  static final String SOURCE = "observability-exporters";

  @Override
  public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
    String metrics = environment.getProperty("observability.metrics.exporter", "prometheus");
    String logs = environment.getProperty("observability.logs.exporter", "console");
    String traces = environment.getProperty("observability.traces.exporter", "none");
    Map<String, Object> mapped = new HashMap<>();

    switch (metrics) {
      case "prometheus" -> mapped.put("management.prometheus.metrics.export.enabled", "true");
      case "none" -> mapped.put("management.prometheus.metrics.export.enabled", "false");
      case "cloudwatch", "azure-monitor" -> throw unimplemented("metrics", metrics);
      default -> throw unknown("metrics", metrics);
    }

    switch (logs) {
      case "console" -> {
        // Console appender is always attached. The elk profile is what adds Logstash.
      }
      case "logstash" -> environment.addActiveProfile("elk");
      case "cloudwatch", "azure-monitor" -> throw unimplemented("logs", logs);
      default -> throw unknown("logs", logs);
    }

    switch (traces) {
      case "none" -> {
        mapped.put("management.tracing.enabled", "false");
        mapped.put("management.otlp.tracing.export.enabled", "false");
      }
      case "otlp" -> {
        String endpoint = environment.getProperty(
            "observability.traces.otlp-endpoint",
            "http://127.0.0.1:4318/v1/traces");
        mapped.put("management.tracing.enabled", "true");
        mapped.put("management.tracing.sampling.probability", "1.0");
        mapped.put("management.otlp.tracing.export.enabled", "true");
        mapped.put("management.otlp.tracing.endpoint", endpoint);
      }
      case "xray", "azure-monitor" -> throw unimplemented("traces", traces);
      default -> throw unknown("traces", traces);
    }

    environment.getPropertySources().addFirst(new MapPropertySource(SOURCE, mapped));
  }

  @Override
  public int getOrder() {
    return Ordered.LOWEST_PRECEDENCE;
  }

  private static IllegalStateException unimplemented(String signal, String exporter) {
    return new IllegalStateException(
        "observability." + signal + ".exporter=" + exporter
            + " is reserved for a later AWS or Azure deployment and is not wired. "
            + allowed());
  }

  private static IllegalStateException unknown(String signal, String exporter) {
    return new IllegalStateException(
        "observability." + signal + ".exporter=" + exporter + " is not a known exporter. " + allowed());
  }

  private static String allowed() {
    return "Use metrics prometheus or none, logs console or logstash, and traces none or otlp.";
  }
}
