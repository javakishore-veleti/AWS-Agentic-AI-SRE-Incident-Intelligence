package com.larkspur.orders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withNoContent;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.larkspur.orders.PurchaseSpanCapture.CapturingProcessor;
import com.larkspur.testing.CommerceServiceTest;
import com.larkspur.testing.DownstreamServers;
import com.larkspur.testing.ShopperTokens;
import io.opentelemetry.sdk.trace.data.SpanData;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@CommerceServiceTest
class PurchaseTraceTest {
  @Autowired MockMvc mvc;
  @Autowired CapturingProcessor spans;

  @BeforeEach
  void reset() {
    DownstreamServers.reset();
    spans.finished.clear();
  }

  @Test
  void purchaseLinksOffersAndInventoryOnOneTrace() throws Exception {
    UUID customerId = UUID.randomUUID();
    UUID offerId = UUID.randomUUID();
    String[] headers = new String[2];
    stubOffer(offerId, request -> headers[0] = request.getHeaders().getFirst("traceparent"));
    DownstreamServers.server("inventoryClient")
        .expect(requestTo("http://inventory.test/api/reservations"))
        .andExpect(method(HttpMethod.POST))
        .andExpect(request -> headers[1] = request.getHeaders().getFirst("traceparent"))
        .andRespond(withNoContent());

    mvc.perform(post("/api/orders")
            .header("Authorization", ShopperTokens.bearer(customerId, "Ada Lovelace", "ada@example.test"))
            .header("Idempotency-Key", UUID.randomUUID().toString())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"offerId\":\"" + offerId + "\"}"))
        .andExpect(status().isCreated());

    assertLinked(headers[0], "offers.test/api/offers/" + offerId);
    assertLinked(headers[1], "inventory.test/api/reservations");
  }

  @Test
  void inventoryFailureStaysOnThePurchaseTrace() throws Exception {
    UUID offerId = UUID.randomUUID();
    String[] header = new String[1];
    stubOffer(offerId, request -> { });
    DownstreamServers.server("inventoryClient")
        .expect(requestTo("http://inventory.test/api/reservations"))
        .andExpect(method(HttpMethod.POST))
        .andExpect(request -> header[0] = request.getHeaders().getFirst("traceparent"))
        .andRespond(withServerError());

    mvc.perform(post("/api/orders")
            .header("Authorization", ShopperTokens.bearer(UUID.randomUUID(), "Ada Lovelace", "ada@example.test"))
            .header("Idempotency-Key", UUID.randomUUID().toString())
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"offerId\":\"" + offerId + "\"}"))
        .andExpect(status().is5xxServerError());

    SpanData client = assertLinked(header[0], "inventory.test/api/reservations");
    assertThat(client.getStatus().getStatusCode().name()).isEqualTo("ERROR");
  }

  private SpanData assertLinked(String traceparent, String urlPart) {
    assertThat(traceparent).as(describe(List.copyOf(spans.finished))).startsWith("00-");
    String[] parts = traceparent.split("-");
    String traceId = parts[1];
    String clientSpanId = parts[2];
    List<SpanData> finished = List.copyOf(spans.finished);
    SpanData client = finished.stream()
        .filter(span -> span.getSpanId().equals(clientSpanId))
        .findFirst()
        .orElseThrow(() -> new AssertionError(describe(finished)));
    SpanData server = finished.stream()
        .filter(span -> span.getSpanId().equals(client.getParentSpanId()))
        .findFirst()
        .orElseThrow(() -> new AssertionError(describe(finished)));
    assertThat(client.getTraceId()).isEqualTo(traceId);
    assertThat(server.getTraceId()).isEqualTo(traceId);
    assertThat(text(client)).contains(urlPart);
    assertThat(client.getEndEpochNanos() - client.getStartEpochNanos()).isPositive();
    assertThat(server.getEndEpochNanos() - server.getStartEpochNanos()).isPositive();
    return client;
  }

  private static String text(SpanData span) {
    StringBuilder builder = new StringBuilder(span.getName());
    span.getAttributes().forEach((key, value) -> builder.append(' ').append(key.getKey()).append('=').append(value));
    return builder.toString();
  }

  private static String describe(List<SpanData> finished) {
    return finished.stream().map(span -> span.getName() + " " + span.getSpanId() + " parent=" + span.getParentSpanId()).toList().toString();
  }

  private void stubOffer(UUID offerId, java.util.function.Consumer<HttpRequest> capture) {
    String body = "{\"id\":\"" + offerId + "\",\"name\":\"Morning\",\"status\":\"ACTIVE\","
        + "\"offerPrice\":139.00,\"products\":[{\"id\":\"" + UUID.randomUUID() + "\",\"sku\":\"HED-CARAFE\","
        + "\"name\":\"Carafe\",\"unitPrice\":48.00}]}";
    DownstreamServers.server("offersClient")
        .expect(requestTo("http://offers.test/api/offers/" + offerId))
        .andExpect(method(HttpMethod.GET))
        .andExpect(request -> capture.accept(request))
        .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));
  }
}
