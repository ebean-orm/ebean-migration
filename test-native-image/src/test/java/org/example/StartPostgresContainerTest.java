package org.example;

import io.ebean.test.containers.PostgresContainer;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class StartPostgresContainerTest {

  @Test
  void test() throws Exception {
    PostgresContainer container = PostgresContainer.builder("15")
      .port(6432)
      .dbName("mig")
      .build()
      .start();

    System.out.println("Postgres container running " + container.isRunning());

    var out = new File("out");

    Process process = new ProcessBuilder()
      .command("./target/test-native-image")
      .redirectErrorStream(true)
      .redirectOutput(ProcessBuilder.Redirect.to(out))
      .start();

    int code = process.waitFor();
    System.out.println("exit code: " + code);

    List<String> lines = Files.lines(out.toPath()).toList();

    lines.forEach(System.out::println);

    Optional<String> dbMigrationsCompleted = lines.stream()
      .filter(line -> line.contains("DB migrations completed"))
      .findFirst();

    assertThat(dbMigrationsCompleted)
      .isPresent()
      .asString().contains("executed:2 totalMigrations:2");
  }
}
