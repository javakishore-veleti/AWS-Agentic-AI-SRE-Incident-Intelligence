package com.larkspur.orders;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.larkspur.testing.CommerceServiceTest;
import com.larkspur.testing.DownstreamServers;
import com.larkspur.testing.ShopperTokens;
import java.net.ConnectException;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@CommerceServiceTest
class OrderOfferLookupTest {
  @Autowired MockMvc mvc;

  @BeforeEach
  void resetDownstream() {
    DownstreamServers.reset();
  }

  @Test
  void missingOfferIs404() throws Exception {
    UUID offerId = UUID.randomUUID();
    DownstreamServers.server("offersClient")
        .expect(requestTo("http://offers.test/api/offers/" + offerId))
        .andRespond(withStatus(HttpStatus.NOT_FOUND));
    purchase(offerId)
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").value("Offer not found."));
  }

  @Test
  void offersHttpErrorIs502() throws Exception {
    UUID offerId = UUID.randomUUID();
    DownstreamServers.server("offersClient")
        .expect(requestTo("http://offers.test/api/offers/" + offerId))
        .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));
    purchase(offerId)
        .andExpect(status().isBadGateway())
        .andExpect(jsonPath("$.message").value("Offers service rejected the lookup."));
  }

  @Test
  void emptyOfferBodyIs502() throws Exception {
    UUID offerId = UUID.randomUUID();
    DownstreamServers.server("offersClient")
        .expect(requestTo("http://offers.test/api/offers/" + offerId))
        .andRespond(withSuccess("", MediaType.APPLICATION_JSON));
    purchase(offerId)
        .andExpect(status().isBadGateway())
        .andExpect(jsonPath("$.message").value("Offers service returned an empty response."));
  }

  @Test
  void offersConnectionDropIs502() throws Exception {
    UUID offerId = UUID.randomUUID();
    DownstreamServers.server("offersClient")
        .expect(requestTo("http://offers.test/api/offers/" + offerId))
        .andRespond(withException(new ConnectException("down")));
    purchase(offerId)
        .andExpect(status().isBadGateway())
        .andExpect(jsonPath("$.message").value("Offers service is unreachable."));
  }

  private ResultActions purchase(UUID offerId) throws Exception {
    return mvc.perform(post("/api/orders")
        .header("Authorization", ShopperTokens.bearer(UUID.randomUUID(), "Ada Lovelace", "ada@example.test"))
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"offerId\":\"" + offerId + "\"}"));
  }
}
