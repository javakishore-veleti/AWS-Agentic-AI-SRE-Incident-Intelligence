package com.larkspur.testing;

import java.util.Map;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.MapPropertySource;

public final class DownstreamInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
  @Override
  public void initialize(ConfigurableApplicationContext context) {
    context.getEnvironment().getPropertySources().addFirst(new MapPropertySource(
        "commerce-test-traces",
        Map.of(
            "management.tracing.enabled", "true",
            "management.tracing.sampling.probability", "1.0",
            "management.otlp.tracing.export.enabled", "true",
            "management.otlp.tracing.endpoint", "http://127.0.0.1:9/v1/traces")));
    context.addBeanFactoryPostProcessor(new DownstreamClientOverrides());
  }
}
