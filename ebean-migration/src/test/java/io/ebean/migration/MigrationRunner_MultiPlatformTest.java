package io.ebean.migration;

import org.junit.jupiter.api.Test;

public class MigrationRunner_MultiPlatformTest {

  private MigrationConfig createMigrationConfig() {
    MigrationConfig config = new MigrationConfig();
    config.setDbUsername("sa");
    config.setDbPassword("");
    config.setDbUrl("jdbc:h2:mem:dbMxMultiPlatform");

    return config;
  }

  @Test
  public void run_when() {

    MigrationConfig config = createMigrationConfig();

    config.setPlatform("h2");
    config.setMigrationPath("multiplatform");

    MigrationRunner runner = new MigrationRunner(config);
    runner.run();
  }

}
