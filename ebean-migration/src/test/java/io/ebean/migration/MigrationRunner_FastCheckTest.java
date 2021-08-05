package io.ebean.migration;

import org.junit.jupiter.api.Test;

public class MigrationRunner_FastCheckTest {

  private MigrationConfig createMigrationConfig() {
    MigrationConfig config = new MigrationConfig();
    config.setDbUsername("sa");
    config.setDbPassword("");
    config.setDbUrl("jdbc:h2:mem:dbMxFastCheck");

    return config;
  }

  @Test
  public void run_when_createConnection() {

    MigrationConfig config = createMigrationConfig();

    config.setPlatform("h2");
    config.setMigrationPath("fastcheck");

    MigrationRunner runner = new MigrationRunner(config);

    runner.run();
  }

  @Test
  public void run_when() {

    MigrationConfig config = createMigrationConfig();

    config.setPlatform("h2");
    config.setMigrationPath("index0");

    MigrationRunner runner = new MigrationRunner(config);

    runner.run();
  }

}
