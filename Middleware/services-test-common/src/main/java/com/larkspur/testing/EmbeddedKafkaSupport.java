package com.larkspur.testing;

import org.springframework.kafka.test.EmbeddedKafkaKraftBroker;

public final class EmbeddedKafkaSupport {
  private EmbeddedKafkaKraftBroker broker;

  public void start() {
    broker = new EmbeddedKafkaKraftBroker(1, 1);
    try {
      broker.afterPropertiesSet();
    } catch (Exception ex) {
      throw new IllegalStateException("Kafka did not start", ex);
    }
  }

  public String bootstrapServers() {
    return broker.getBrokersAsString();
  }

  public void stop() {
    if (broker != null) {
      broker.destroy();
      broker = null;
    }
  }
}
