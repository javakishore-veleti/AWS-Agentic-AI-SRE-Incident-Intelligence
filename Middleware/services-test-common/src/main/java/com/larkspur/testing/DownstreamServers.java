package com.larkspur.testing;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.test.web.client.MockRestServiceServer;
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
    MockRestServiceServer server = MockRestServiceServer.bindTo(builder).ignoreExpectOrder(true).build();
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
}
