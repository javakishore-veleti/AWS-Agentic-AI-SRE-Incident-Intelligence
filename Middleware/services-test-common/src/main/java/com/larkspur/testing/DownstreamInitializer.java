package com.larkspur.testing;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

public final class DownstreamInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
  @Override
  public void initialize(ConfigurableApplicationContext context) {
    context.addBeanFactoryPostProcessor(new DownstreamClientOverrides());
  }
}
