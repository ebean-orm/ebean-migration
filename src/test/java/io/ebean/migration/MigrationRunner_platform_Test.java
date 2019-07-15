package io.ebean.migration;

import io.ebean.docker.commands.PostgresConfig;
import io.ebean.docker.commands.PostgresContainer;
import io.ebean.docker.commands.SqlServerConfig;
import io.ebean.docker.commands.SqlServerContainer;
import org.testng.annotations.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class MigrationRunner_platform_Test {

  private final PostgresContainer postgresContainer = createPostgres();
  private final SqlServerContainer sqlServerContainer = createSqlServer();

  private static PostgresContainer createPostgres() {
    PostgresConfig config = new PostgresConfig("10.1");
    config.setContainerName("test_ebean_migration_pg10");
    config.setPort("9823");
    config.setFastStartMode(true);
    config.setUser("mig_test");
    config.setDbName("mig_test");

    return new PostgresContainer(config);
  }

  private MigrationConfig postgresMigrationConfig() {

    MigrationConfig config = new MigrationConfig();
    config.setDbDriver("org.postgresql.Driver");
    config.setDbUrl("jdbc:postgresql://localhost:9823/mig_test");
    config.setDbUsername("mig_test");
    config.setDbPassword("mig_test");
    return config;
  }

  private static SqlServerContainer createSqlServer() {
    SqlServerConfig config = new SqlServerConfig("2017-GA-ubuntu");
    config.setContainerName("test_ebean_migration_sql17");
    config.setPort("2433");
    config.setUser("mig_test");
    config.setDbName("mig_test");

    return new SqlServerContainer(config);
  }

  private MigrationConfig sqlServerMigrationConfig() {

    MigrationConfig config = new MigrationConfig();
    config.setDbDriver("com.microsoft.sqlserver.jdbc.SQLServerDriver");
    config.setDbUsername("mig_test");
    config.setDbPassword("SqlS3rv#r");
    config.setDbUrl(sqlServerContainer.jdbcUrl());
    return config;
  }

  /**
   * Run manually against Postgres and other platforms.
   */
  @Test
  public void run_when_suppliedDataSource() throws SQLException {

    postgresContainer.start();

    MigrationConfig config = postgresMigrationConfig();

    config.setMigrationPath("dbmig");

    MigrationRunner runner = new MigrationRunner(config);
    runner.run();

    System.out.println("-- run 2 --");
    runner.run();

    System.out.println("-- run 3 --");
    config.setMigrationPath("dbmig2");
    runner.run();

    try (Connection connection = postgresContainer.createConnection()) {
      readQuery(connection, "select * from m1");
      readQuery(connection, "select * from m2");
      readQuery(connection, "select * from m3");
    }

    postgresContainer.stop();
  }

  @Test
  public void sqlServer_migration() throws SQLException {

    sqlServerContainer.start();

    MigrationConfig config = sqlServerMigrationConfig();

    config.setMigrationPath("dbmig_sqlserver");

    MigrationRunner runner = new MigrationRunner(config);
    runner.run();

    try (Connection connection = sqlServerContainer.createConnection()) {
      readQuery(connection, "select * from m1");
      readQuery(connection, "select * from m2");
      readQuery(connection, "select * from m3");
    }

    sqlServerContainer.stop();
  }

  private void readQuery(Connection connection, String sql) throws SQLException {
    try (PreparedStatement stmt = connection.prepareStatement(sql)) {
      try (ResultSet rset = stmt.executeQuery()) {
        while (rset.next()) {
          rset.getObject(1);
        }
      }
    }
  }

}
