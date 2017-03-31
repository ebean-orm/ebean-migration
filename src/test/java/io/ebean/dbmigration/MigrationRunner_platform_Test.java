package io.ebean.dbmigration;

import org.testng.annotations.Test;

public class MigrationRunner_platform_Test {

  private MigrationConfig createMigrationConfig() {

    MigrationConfig config = new MigrationConfig();
    config.setDbDriver("org.postgresql.Driver");
    config.setDbUrl("jdbc:postgresql://localhost:5432/mult");
    config.setDbUsername("mult");
    config.setDbPassword("mult");

    //config.setDbSchema("ten_1");

    return config;
  }

  /**
   * Run manually against Postgres and other platforms.
   */
  @Test(enabled = false)
  public void run_when_suppliedDataSource() throws Exception {

    MigrationConfig config = createMigrationConfig();

    config.setMigrationPath("dbmig");

    MigrationRunner runner = new MigrationRunner(config);
    runner.run();

    System.out.println("-- run 2 --");
    runner.run();

    System.out.println("-- run 3 --");
    config.setMigrationPath("dbmig2");
    runner.run();
  }

}