package io.ebean.migration;

import io.ebean.ddlrunner.DdlRunner;
import io.ebean.docker.commands.DbConfig;
import io.ebean.docker.commands.MySqlConfig;
import io.ebean.docker.commands.MySqlContainer;
import io.ebean.docker.commands.NuoDBConfig;
import io.ebean.docker.commands.NuoDBContainer;
import io.ebean.docker.commands.OracleConfig;
import io.ebean.docker.commands.OracleContainer;
import io.ebean.docker.commands.PostgresConfig;
import io.ebean.docker.commands.PostgresContainer;
import io.ebean.docker.commands.SqlServerConfig;
import io.ebean.docker.commands.SqlServerContainer;
import org.testng.annotations.Test;

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

  private static void setContainerName(DbConfig config, String suffix) {
    config.setContainerName("test_ebean_migration_" + suffix);
    config.setUser("mig_test");
    config.setDbName("mig_test");
  }

  private static NuoDBContainer createNuoDB() {
    NuoDBConfig config = new NuoDBConfig();
    config.setSchema("mig_test");
    config.setUser("mig_test");
    config.setPassword("test");
    return new NuoDBContainer(config);
  }

  private static PostgresContainer createPostgres() {
    PostgresConfig config = new PostgresConfig("10.1");
    config.setPort("9823");
    setContainerName(config, "pg10");
    return new PostgresContainer(config);
  }

  private static SqlServerContainer createSqlServer() {
    SqlServerConfig config = new SqlServerConfig("2017-GA-ubuntu");
    config.setPort("2433");
    setContainerName(config, "sql17");
    return new SqlServerContainer(config);
  }

  private static MySqlContainer createMySqlContainer() {
    MySqlConfig config = new MySqlConfig("8.0");
    config.setPort("14306");
    setContainerName(config, "mysql");
    return new MySqlContainer(config);
  }

  private static OracleContainer createOracleContainer() {
    OracleConfig config = new OracleConfig("latest");
    setContainerName(config, "oracle");
    config.setDbName("XE");
    config.setImage("oracleinanutshell/oracle-xe-11g:latest");
    return new OracleContainer(config);
  }

  private MigrationConfig newMigrationConfig() {
    MigrationConfig config = new MigrationConfig();
    config.setDbUsername("mig_test");
    config.setDbPassword("test");
    return config;
  }

  private MigrationConfig postgresMigrationConfig() {
    MigrationConfig config = newMigrationConfig();
    config.setDbDriver("org.postgresql.Driver");
    config.setDbUrl(postgresContainer.jdbcUrl());
    return config;
  }

  private MigrationConfig sqlServerMigrationConfig() {
    MigrationConfig config = newMigrationConfig();
    config.setDbPassword("SqlS3rv#r");
    config.setDbDriver("com.microsoft.sqlserver.jdbc.SQLServerDriver");
    config.setDbUrl(sqlServerContainer.jdbcUrl());
    return config;
  }

  private MigrationConfig nuodDbMigrationConfig() {
    MigrationConfig config = newMigrationConfig();
    config.setDbDriver("com.nuodb.jdbc.Driver");
    config.setDbUrl(nuoDBContainer.jdbcUrl());
    config.setDbSchema("mig_test");
    config.setDbUsername("mig_test");
    config.setDbPassword("test");
    return config;
  }

  private MigrationConfig mysqlMigrationConfig() {
    MigrationConfig config = newMigrationConfig();
    config.setDbDriver("com.mysql.cj.jdbc.Driver");
    config.setDbUrl(mysqlContainer.jdbcUrl());
    return config;
  }

  private MigrationConfig oracleMigrationConfig() {
    MigrationConfig config = newMigrationConfig();
    config.setDbDriver("oracle.jdbc.OracleDriver");
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

  @Test(enabled = false)
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

    MigrationRunner runner = new MigrationRunner(config);
    runner.run();

    try (Connection connection = mysqlContainer.createConnection()) {
      readQuery(connection, "select * from m1");
      readQuery(connection, "select * from m2");
      readQuery(connection, "select * from m3");
    }

    mysqlContainer.stopRemove();
  }

  @Test(enabled = false)
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

  @Test(enabled = false)
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
