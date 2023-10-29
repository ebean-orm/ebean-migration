package org.example;

import org.junit.jupiter.api.Test;
import io.ebean.test.containers.*;

class StartPostgresContainerTest {

  @Test
  void test() {
    PostgresContainer.builder("15")
      .port(6432)
      .dbName("mig")
      .build()
      .start();
  }
}
