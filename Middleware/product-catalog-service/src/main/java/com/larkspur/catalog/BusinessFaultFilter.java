package com.larkspur.catalog;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
class BusinessFaultFilter extends OncePerRequestFilter {
  private final Environment environment;

  BusinessFaultFilter(Environment environment) {
    this.environment = environment;
  }

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    boolean fail = environment.getProperty("fault.fail-requests", Boolean.class, Boolean.FALSE);
    if (fail && request.getRequestURI().startsWith("/api/")) {
      response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
      response.setContentType(MediaType.APPLICATION_JSON_VALUE);
      response.getWriter().write("{\"message\":\"Catalog cannot complete this request.\"}");
      return;
    }
    chain.doFilter(request, response);
  }
}
