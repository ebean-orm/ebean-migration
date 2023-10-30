package org.example;

import org.junit.jupiter.api.Test;
import io.ebean.test.containers.*;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.List;

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

    Files.lines(out.toPath()).forEach(System.out::println);
  }
}
