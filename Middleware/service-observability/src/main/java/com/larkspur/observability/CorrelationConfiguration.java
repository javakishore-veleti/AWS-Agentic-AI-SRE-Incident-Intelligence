package com.larkspur.observability;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;

@AutoConfiguration
class CorrelationConfiguration {
  @Bean
  FilterRegistrationBean<CorrelationFilter> correlationFilter(
      @Value("${spring.application.name:service}") String service) {
    FilterRegistrationBean<CorrelationFilter> registration = new FilterRegistrationBean<>(new CorrelationFilter(service));
    registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
    return registration;
  }
}
