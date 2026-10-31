package com.larkspur.testing;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.RequestExpectationManager;
import org.springframework.test.web.client.RequestMatcher;
import org.springframework.test.web.client.ResponseActions;
import org.springframework.test.web.client.UnorderedRequestExpectationManager;
import org.springframework.web.client.RestClient;

public final class DownstreamServers {
  private static final Map<String, MockRestServiceServer> SERVERS = new ConcurrentHashMap<>();

  private DownstreamServers() {
  }

  public static RestClient bind(String name) {
    String base = switch (name) {
      case "offersClient" -> "http://offers.test";
      case "catalogRestClient" -> "http://catalog.test";
      case "inventoryClient", "inventoryRestClient" -> "http://inventory.test";
      default -> throw new IllegalArgumentException(name);
    };
    RestClient.Builder builder = RestClient.builder()
        .baseUrl(base)
        .requestFactory(new BufferingClientHttpRequestFactory(new SimpleClientHttpRequestFactory()));
    MockRestServiceServer server = MockRestServiceServer.bindTo(builder)
        .build(new RuntimeFailureExpectationManager());
    SERVERS.put(name, server);
    return builder.build();
  }

  public static MockRestServiceServer server(String name) {
    MockRestServiceServer server = SERVERS.get(name);
    if (server == null) {
      throw new IllegalStateException("No mock server for " + name);
    }
    return server;
  }

  public static void reset() {
    SERVERS.values().forEach(MockRestServiceServer::reset);
  }

  private static final class RuntimeFailureExpectationManager implements RequestExpectationManager {
    private final RequestExpectationManager delegate = new UnorderedRequestExpectationManager();

    @Override
    public ResponseActions expectRequest(ExpectedCount count, RequestMatcher matcher) {
      return delegate.expectRequest(count, matcher);
    }

    @Override
    public void verify() {
      delegate.verify();
    }

    @Override
    public void verify(Duration timeout) {
      delegate.verify(timeout);
    }

    @Override
    public void reset() {
      delegate.reset();
    }

    @Override
    public ClientHttpResponse validateRequest(ClientHttpRequest request) throws IOException {
      try {
        return delegate.validateRequest(request);
      } catch (AssertionError error) {
        throw new IOException(error.getMessage(), error);
      }
    }
  }
}
