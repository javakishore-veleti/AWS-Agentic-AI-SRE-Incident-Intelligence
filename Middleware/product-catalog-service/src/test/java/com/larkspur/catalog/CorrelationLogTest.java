package com.larkspur.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.larkspur.testing.CommerceServiceTest;
import java.nio.charset.StandardCharsets;
import java.util.List;
import net.logstash.logback.encoder.LogstashEncoder;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

@CommerceServiceTest
class CorrelationLogTest {
  @Autowired MockMvc mvc;

  @Test
  void failureLogKeepsTheCorrelationIdAndDropsTheToken() throws Exception {
    Logger logger = (Logger) LoggerFactory.getLogger(ApiErrors.class);
    ListAppender<ILoggingEvent> appender = new ListAppender<>();
    appender.start();
    logger.addAppender(appender);
    try {
      mvc.perform(get("/no-such-commerce-path")
              .header("X-Correlation-Id", "obs-002-failure")
              .header("Authorization", "Bearer super-secret-token"))
          .andExpect(header().string("X-Correlation-Id", "obs-002-failure"));
    } finally {
      logger.detachAppender(appender);
    }
    List<ILoggingEvent> events = appender.list;
    assertThat(events).isNotEmpty();
    ILoggingEvent event = events.get(events.size() - 1);
    assertThat(event.getMDCPropertyMap()).containsEntry("correlationId", "obs-002-failure");
    assertThat(event.getMDCPropertyMap()).containsEntry("service", "product-catalog-service");
    LogstashEncoder encoder = new LogstashEncoder();
    encoder.start();
    String json = new String(encoder.encode(event), StandardCharsets.UTF_8);
    encoder.stop();
    assertThat(json).contains("\"correlationId\":\"obs-002-failure\"");
    assertThat(json).doesNotContain("super-secret-token");
  }
}
