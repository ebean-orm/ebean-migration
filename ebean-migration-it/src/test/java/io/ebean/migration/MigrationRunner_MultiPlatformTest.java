package io.ebean.migration;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MigrationRunner_MultiPlatformTest {

  private MigrationConfig createMigrationConfig(String memName) {
    MigrationConfig config = new MigrationConfig();
    config.setDbUsername("sa");
    config.setDbPassword("");
    config.setDbUrl("jdbc:h2:mem:" + memName);
    return config;
  }

  @Test
  void run_withPlatformOnly() {
    MigrationConfig config = createMigrationConfig("dbMxMultiPlatform0");

    config.setPlatform("h2");
    config.setMigrationPath("multiplatform");

    MigrationRunner runner = new MigrationRunner(config);
    runner.run();

    assertThat(runner.checkState()).hasSize(1);
  }

  @Test
  void run_withBase_thatDoesNotExist() {
    MigrationConfig config = createMigrationConfig("dbMxMultiPlatform1");

    config.setBasePlatform("doesNotExist");
    config.setPlatform("h2");
    config.setMigrationPath("multiplatform");

    MigrationRunner runner = new MigrationRunner(config);
    runner.run();

    assertThat(runner.checkState()).hasSize(1);
  }

  @Test
  void run_withBase() {
    MigrationConfig config = createMigrationConfig("dbMxBasePlatform");

    config.setBasePlatform("mybase");
    config.setPlatform("h2");
    config.setMigrationPath("multiplatform");

    MigrationRunner runner = new MigrationRunner(config);
    runner.run();

    assertThat(runner.checkState()).hasSize(2);
  }

}
