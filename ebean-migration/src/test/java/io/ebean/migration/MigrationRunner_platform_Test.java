package io.ebean.migration;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.ebean.ddlrunner.DdlRunner;
import io.ebean.test.containers.*;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;

public class MigrationRunner_platform_Test {

  private final NuoDBContainer nuoDBContainer = createNuoDB();
  private final PostgresContainer postgresContainer = createPostgres();
  private final SqlServerContainer sqlServerContainer = createSqlServer();
  private final MySqlContainer mysqlContainer = createMySqlContainer();
  private final OracleContainer oracleContainer = createOracleContainer();

  private static void setContainerName(ContainerBuilderDb<?, ?> config, String suffix) {
    config.containerName("test_ebean_migration_" + suffix);
    config.user("mig_test");
    config.dbName("mig_test");
  }

  private static NuoDBContainer createNuoDB() {
    return NuoDBContainer.builder("4.0")
      .schema("mig_test")
      .user("mig_test")
      .password("test")
      .build();
  }

  private static PostgresContainer createPostgres() {
    PostgresContainer.Builder builder = PostgresContainer.builder("15")
      .port(9823);
    setContainerName(builder, "pg15");
    return builder.build();
  }

  private static SqlServerContainer createSqlServer() {
    SqlServerContainer.Builder builder = SqlServerContainer.builder("2017-GA-ubuntu").port(2433);
    setContainerName(builder, "sql17");
    return builder.build();
  }

  private static MySqlContainer createMySqlContainer() {
    MySqlContainer.Builder builder = MySqlContainer.builder("8.0").port(14306);
    setContainerName(builder, "mysql");
    return builder.build();
  }

  private static OracleContainer createOracleContainer() {
    OracleContainer.Builder builder = OracleContainer.builder("latest");
    setContainerName(builder, "oracle");
    return builder
      .dbName("XE")
      .image("oracleinanutshell/oracle-xe-11g:latest")
      .build();
  }

  private MigrationConfig newMigrationConfig() {
    MigrationConfig config = new MigrationConfig();
    config.setDbUsername("mig_test");
    config.setDbPassword("test");
    return config;
  }

  private MigrationConfig postgresMigrationConfig() {
    MigrationConfig config = newMigrationConfig();
    config.setDbUrl(postgresContainer.jdbcUrl());
    return config;
  }

  private MigrationConfig sqlServerMigrationConfig() {
    MigrationConfig config = newMigrationConfig();
    config.setDbPassword("SqlS3rv#r");
    config.setDbUrl(sqlServerContainer.jdbcUrl());
    return config;
  }

  private MigrationConfig nuodDbMigrationConfig() {
    MigrationConfig config = newMigrationConfig();
    config.setDbUrl(nuoDBContainer.jdbcUrl());
    config.setDbSchema("mig_test");
    config.setDbUsername("mig_test");
    config.setDbPassword("test");
    return config;
  }

  private MigrationConfig mysqlMigrationConfig() {
    MigrationConfig config = newMigrationConfig();
    config.setDbUrl(mysqlContainer.jdbcUrl());
    return config;
  }

  private MigrationConfig oracleMigrationConfig() {
    MigrationConfig config = newMigrationConfig();
    config.setDbUrl(oracleContainer.jdbcUrl());
    return config;
  }

  @Test
  public void postgres_migration() throws SQLException {

    postgresContainer.startWithDropCreate();

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
      assertThat(readQuery(connection, "select * from m1")).isEqualTo(0);
      readQuery(connection, "select * from m2");
      readQuery(connection, "select * from m3");
    }

    config.setMigrationPath("dbmig_postgres_concurrently0");
    runner.run();

    config.setMigrationPath("dbmig_postgres_concurrently1");
    runner.run();

    ddlRunnerBasic();
    ddlRunnerWithConcurrently();
    try (Connection connection = postgresContainer.createConnection()) {
      assertThat(readQuery(connection, "select * from m1")).isEqualTo(6);
    }

    postgresContainer.stopRemove();
  }

  private void ddlRunnerWithConcurrently() throws SQLException {
    String content =
      "insert into m1 (id, acol) values (2001, '2one');\n" +
        "insert into m1 (id, acol) values (2002, '2two');\n" +
        "insert into m1 (id, acol) values (2003, '2three');\n" +
        "create index concurrently ix2_m1_acol0 on m1 (acol);\n" +
        "create index concurrently ix2_m1_acol1 on m1 (lower(acol), id);\n";

    try (Connection connection = postgresContainer.createConnection()) {
      connection.setAutoCommit(false);
      DdlRunner runner = new DdlRunner(false, "test", "postgres");
      runner.runAll(content, connection);
      connection.commit();

      assertThat(runner.runNonTransactional(connection)).isEqualTo(2).as("executed create index concurrently");
    }
  }

  private void ddlRunnerBasic() throws SQLException {

    String content =
      "insert into m1 (id, acol) values (1001, 'one');\n" +
        "insert into m1 (id, acol) values (1002, 'two');\n" +
        "insert into m1 (id, acol) values (1003, 'three');\n";

    try (Connection connection = postgresContainer.createConnection()) {
      connection.setAutoCommit(false);
      DdlRunner runner = new DdlRunner(false, "test");
      runner.runAll(content, connection);
      connection.commit();

      assertThat(runner.runNonTransactional(connection)).isEqualTo(0).as("all transactional");
    }
  }

  @Disabled
  @Test
  public void sqlServer_migration() throws SQLException {

    sqlServerContainer.startWithDropCreate();

    MigrationConfig config = sqlServerMigrationConfig();

    config.setMigrationPath("dbmig_sqlserver");

    MigrationRunner runner = new MigrationRunner(config);
    runner.run();

    try (Connection connection = sqlServerContainer.createConnection()) {
      readQuery(connection, "select * from m1");
      readQuery(connection, "select * from m2");
      readQuery(connection, "select * from m3");
    }

//    sqlServerContainer.stopRemove();
  }

  @Test
  public void mysql_migration() throws SQLException {

    mysqlContainer.stopRemove();
    mysqlContainer.startWithDropCreate();

    MigrationConfig config = mysqlMigrationConfig();

    config.setMigrationPath("dbmig_basic");

    HikariConfig hikariConfig = new HikariConfig();
    hikariConfig.setJdbcUrl(mysqlContainer.jdbcUrl());
    hikariConfig.setUsername("mig_test");
    hikariConfig.setPassword("test");

    try (HikariDataSource ds = new HikariDataSource(hikariConfig)) {
      MigrationRunner runner = new MigrationRunner(config);
      try (Connection connection1 = ds.getConnection()) {
        // connection1.setAutoCommit(false);
        runner.run(connection1);
      }

      try (Connection connection = mysqlContainer.createConnection()) {
        readQuery(connection, "select * from m1");
        readQuery(connection, "select * from m2");
        readQuery(connection, "select * from m3");
      }
    }

    mysqlContainer.stopRemove();
  }

  @Disabled
  @Test
  public void nuodb_migration() throws SQLException {

    //nuoDBContainer.stopRemove();
    nuoDBContainer.startWithDropCreate();

    MigrationConfig config = nuodDbMigrationConfig();
    config.setMigrationPath("dbmig_nuodb");

    MigrationRunner runner = new MigrationRunner(config);
    runner.run();

    try (Connection connection = nuoDBContainer.createConnection()) {
      readQuery(connection, "select * from m1");
      readQuery(connection, "select * from orp_master");
      readQuery(connection, "select * from orp_master_with_history");
    }
    nuoDBContainer.stop();
  }

  @Disabled
  @Test
  public void oracle_migration() throws SQLException {

    oracleContainer.startWithDropCreate();

    MigrationConfig config = oracleMigrationConfig();

    config.setMigrationPath("dbmig_basic");

    MigrationRunner runner = new MigrationRunner(config);
    runner.run();

    try (Connection connection = oracleContainer.createConnection()) {
      readQuery(connection, "select * from m1");
      readQuery(connection, "select * from m2");
      readQuery(connection, "select * from m3");
    }

    //oracleContainer.stopRemove();
  }

  private int readQuery(Connection connection, String sql) throws SQLException {
    int rowCount = 0;
    try (PreparedStatement stmt = connection.prepareStatement(sql)) {
      try (ResultSet rset = stmt.executeQuery()) {
        while (rset.next()) {
          rset.getObject(1);
          rowCount++;
        }
      }
    }
    return rowCount;
  }

}
