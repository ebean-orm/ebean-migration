package io.ebean.dbmigration;

import org.avaje.datasource.DataSourceConfig;
import org.avaje.datasource.DataSourcePool;
import org.avaje.datasource.Factory;
import org.testng.annotations.Test;

import java.sql.Connection;

public class MigrationRunnerTest {

  private MigrationConfig createMigrationConfig() {
    MigrationConfig config = new MigrationConfig();
    config.setDbUsername("sa");
    config.setDbPassword("");
    config.setDbDriver("org.h2.Driver");
    config.setDbUrl("jdbc:h2:mem:db1");
    return config;
  }

  @Test
  public void run_when_createConnection() throws Exception {

    MigrationConfig config = createMigrationConfig();

    config.setMigrationPath("dbmig");
    MigrationRunner runner = new MigrationRunner(config);
    runner.run();
  }

  @Test
  public void run_when_fileSystemResources() throws Exception {

    MigrationConfig config = createMigrationConfig();

    config.setMigrationPath("filesystem:test-fs-resources/fsdbmig");
    MigrationRunner runner = new MigrationRunner(config);
    runner.run();
  }

  @Test
  public void run_when_suppliedConnection() {

    MigrationConfig config = createMigrationConfig();
    Connection connection = config.createConnection();

    config.setMigrationPath("dbmig");
    MigrationRunner runner = new MigrationRunner(config);
    runner.run(connection);
  }

  @Test
  public void run_when_suppliedDataSource() throws Exception {

    DataSourceConfig dataSourceConfig = new DataSourceConfig();
    dataSourceConfig.setDriver("org.h2.Driver");
    dataSourceConfig.setUrl("jdbc:h2:mem:tests");
    dataSourceConfig.setUsername("sa");
    dataSourceConfig.setPassword("");

    Factory factory = new Factory();
    DataSourcePool dataSource = factory.createPool("test", dataSourceConfig);

    MigrationConfig config = createMigrationConfig();
    config.setMigrationPath("dbmig");

    MigrationRunner runner = new MigrationRunner(config);
    runner.run(dataSource);
    System.out.println("-- run second time --");
    runner.run(dataSource);

    // simulate change to repeatable migration
    config.setMigrationPath("dbmig3");
    System.out.println("-- run third time --");
    runner.run(dataSource);

  }

}