package com.larkspur.orders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.larkspur.testing.CommerceServiceTest;
import com.larkspur.testing.PrometheusSeries;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

@CommerceServiceTest
class PrometheusMetricsTest {
  @Autowired MockMvc mvc;

  @Test
  void prometheusExposesHttpLatencyErrorsAndPool() throws Exception {
    mvc.perform(get("/no-such-commerce-path"));
    String series = mvc.perform(get("/actuator/prometheus"))
        .andExpect(status().isOk())
        .andReturn()
        .getResponse()
        .getContentAsString();
    PrometheusSeries.assertCommerceSeries(series);
  }
}
