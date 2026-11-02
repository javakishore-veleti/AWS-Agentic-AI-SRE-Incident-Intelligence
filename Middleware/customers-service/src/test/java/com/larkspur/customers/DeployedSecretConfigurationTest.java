package com.larkspur.customers;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

class DeployedSecretConfigurationTest {

  @Test
  void applicationFilesLoadSecretsFromTheEnvironment() throws Exception {
    Path middleware = Path.of("..").toAbsolutePath().normalize();
    StandardEnvironment environment = new StandardEnvironment();
    environment
        .getPropertySources()
        .addFirst(
            new MapPropertySource(
                "environment",
                Map.of("DB_PASSWORD", "from-the-environment", "JWT_SECRET", "from-the-environment")));

    for (String service :
        new String[] {
          "customers-service",
          "orders-service",
          "product-catalog-service",
          "offers-service",
          "inventory-service"
        }) {
      String yaml =
          Files.readString(middleware.resolve(service).resolve("src/main/resources/application.yml"));
      String password =
          yaml.lines().filter(line -> line.trim().startsWith("password:")).findFirst().orElseThrow().trim();
      assertThat(password).isEqualTo("password: ${DB_PASSWORD}");
      assertThat(environment.resolveRequiredPlaceholders("${DB_PASSWORD}"))
          .isEqualTo("from-the-environment");
      yaml.lines()
          .filter(line -> line.trim().startsWith("secret:"))
          .forEach(
              line -> {
                assertThat(line.trim()).isEqualTo("secret: ${JWT_SECRET}");
                assertThat(environment.resolveRequiredPlaceholders("${JWT_SECRET}"))
                    .isEqualTo("from-the-environment");
              });
    }

    String compose =
        Files.readString(
            middleware.resolve("../CICD/Local/Postgres/docker-compose.yaml").normalize());
    assertThat(compose).contains("POSTGRES_PASSWORD: ${DB_PASSWORD:?DB_PASSWORD is required}");
    assertThat(
            compose
                .lines()
                .filter(line -> line.contains("POSTGRES_PASSWORD:"))
                .findFirst()
                .orElseThrow()
                .trim())
        .isEqualTo("POSTGRES_PASSWORD: ${DB_PASSWORD:?DB_PASSWORD is required}");
  }
}
