package com.larkspur.observability;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

class CorrelationFilter extends OncePerRequestFilter {
  static final String HEADER = "X-Correlation-Id";
  private static final Pattern SAFE = Pattern.compile("[A-Za-z0-9._-]{1,80}");

  private final String service;

  CorrelationFilter(String service) {
    this.service = service;
  }

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    String incoming = request.getHeader(HEADER);
    String id = incoming != null && SAFE.matcher(incoming).matches() ? incoming : UUID.randomUUID().toString();
    MDC.put("correlationId", id);
    MDC.put("service", service);
    response.setHeader(HEADER, id);
    try {
      chain.doFilter(request, response);
    } finally {
      MDC.remove("correlationId");
      MDC.remove("service");
    }
  }
}
