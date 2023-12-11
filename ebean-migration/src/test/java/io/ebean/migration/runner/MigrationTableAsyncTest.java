package io.ebean.migration.runner;

import io.ebean.datasource.DataSourceConfig;
import io.ebean.datasource.DataSourceFactory;
import io.ebean.datasource.DataSourcePool;
import io.ebean.migration.MigrationConfig;
import io.ebean.migration.MigrationRunner;
import io.ebean.test.containers.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

public class MigrationTableAsyncTest {

  private static MigrationConfig config;
  private static DataSourcePool dataSource;

  @BeforeEach
  public void setUp() {
    config = new MigrationConfig();
    config.setMetaTable("migtable");
    config.setMigrationPath("dbmig");
  }

  @AfterEach
  public void shutdown() {
    dataSource.shutdown();
  }

  @Disabled
  @Test
  public void testDb2() throws Exception {
    // Note: Holding any lock during "reorg table" does not work
    // (checked row lock and 'lock table x in exclusive mode')
    // as soon as reorg table is executed, all locks held by the current
    // connection are dropped
    // See comment in 1.1__initial.sql

    Db2Container container = Db2Container.builder("latest")
      .port(50055)
      .containerName("mig_async_db2")
      .dbName("mig_test")
      .user("test_ebean")
      .password("test")
      .build();

    container.startWithDropCreate();

    config.setMigrationPath("dbmig");
    config.setDbUsername("test_ebean");
    config.setDbPassword("test");
    config.setDbUrl(container.jdbcUrl());
    runTest(false);
    runTest(true);
  }

  @Disabled
  @Test
  public void testOracle() throws Exception {
    // init oracle docker container
    OracleContainer container = OracleContainer.builder("latest")
      .dbName("XE")
      .image("oracleinanutshell/oracle-xe-11g:latest")
      .user("test_ebean")
      .password("test")
      .build();
    container.startWithDropCreate();

    config.setMigrationPath("dbmig_oracle");
    config.setDbUsername("test_ebean");
    config.setDbPassword("test");
    config.setDbUrl(container.jdbcUrl());
    runTest(false);
    runTest(true);
  }

  @Test
  public void testMySqlDb() throws Exception {
    // init mysql docker container
    MySqlContainer container = MySqlContainer.builder("8.0")
      .port(14307)
      .containerName("mig_async_mysql")
      .dbName("test_ebean")
      .user("test_ebean")
      .password("test")
      .build();

    container.startWithDropCreate();

    config.setMigrationPath("dbmig");
    config.setDbUsername("test_ebean");
    config.setDbPassword("test");
    config.setDbUrl("jdbc:mysql://localhost:14307/test_ebean");
    runTest(false);
    runTest(true);
  }

  @Test
  public void testMariaDb() throws Exception {
    // init mariadb docker container
    MariaDBContainer container = MariaDBContainer.builder("10")
      .port(14308)
      .containerName("mig_async_mariadb")
      .dbName("test_ebean")
      .user("test_ebean")
      .password("test")
      .build();

    container.startWithDropCreate();

    config.setMigrationPath("dbmig");
    config.setDbUsername("test_ebean");
    config.setDbPassword("test");
    config.setDbUrl("jdbc:mariadb://localhost:14308/test_ebean");
    runTest(false);
    runTest(true);
  }

  //@Disabled
  @Test
  public void testSqlServer() throws Exception {
    // init sqlserver docker container
    SqlServerContainer container = SqlServerContainer.builder("2017-GA-ubuntu")
      .port(9435)
      .containerName("mig_async_sqlserver")
      .dbName("test_ebean")
      .user("test_ebean")
      .build();
    //conf.setPassword("SqlS3rv#r");

    container.startWithDropCreate();

    config.setMigrationPath("dbmig_sqlserver");
    config.setDbUsername("test_ebean");
    config.setDbPassword("SqlS3rv#r");
    config.setDbUrl("jdbc:sqlserver://localhost:9435;databaseName=test_ebean;sendTimeAsDateTime=false");
    runTest(true);
    runTest(false);
  }


  /**
   * H2 using logical lock mechanism.
   */
  @Test
  public void testH2() throws Exception {
    // thread A looses the lock while thread B runs the migrations.
    config.setMigrationPath("dbmig");
    config.setDbUsername("sa");
    config.setDbPassword("");
    config.setDbUrl("jdbc:h2:mem:dbAsync;LOCK_TIMEOUT=100000");
    runTest(false);
    runTest(true);
  }

  private void runTest(boolean withExisting) throws SQLException, InterruptedException, ExecutionException, IOException {
    DataSourceConfig dataSourceConfig = new DataSourceConfig();
    dataSourceConfig.setUrl(config.getDbUrl());
    dataSourceConfig.setUsername(config.getDbUsername());
    dataSourceConfig.setPassword(config.getDbPassword());
    dataSource = DataSourceFactory.create("test", dataSourceConfig);
    dropTable("migtable");
    dropTable("m1");
    dropTable("m2");
    dropTable("m3");

    if (withExisting) {
      // create empty migration table
      try (Connection conn = dataSource.getConnection()) {
        String derivedPlatformName = DbNameUtil.normalise(conn);

        config.setPlatform(derivedPlatformName);
        MigrationPlatform platform = DbNameUtil.platform(derivedPlatformName);
        MigrationTable table = migrationTable(platform, conn);
        table.createIfNeededAndLock();
        table.unlockMigrationTable();
        conn.commit();
      }
    }

    ExecutorService exec = Executors.newFixedThreadPool(8);
    List<Future<String>> futures = new ArrayList<>();
    for (int i = 0; i < 2; i++) {
      Future<String> future = exec.submit(this::runMigration);
      futures.add(future);
    }
    for (Future<String> future : futures) {
      assertThat(future.get()).isEqualTo("OK");
    }
  }

  private static MigrationTable migrationTable(MigrationPlatform platform, Connection connection) {
    var fc = new FirstCheck(config, new DefaultMigrationContext(config, connection), platform);
    return new MigrationTable(fc, false);
  }

  private void dropTable(String tableName) throws SQLException {
    try (Connection conn = dataSource.getConnection();
         Statement stmt = conn.createStatement()) {
      String dbProductName = conn.getMetaData().getDatabaseProductName().toLowerCase();
      if (dbProductName.contains("db2")) {
        stmt.execute("begin\n"
          + "if exists (select tabname from syscat.tables where lcase(tabname) = '" + tableName + "' and tabschema = current_schema) then\n"
          + " prepare stmt from 'drop table " + tableName + "';\n"
          + " execute stmt;\n"
          + "end if;\n"
          + "end");
      } else if (dbProductName.contains("oracle")) {
        // do nothing, re-created via container.startWithDropCreate();
      } else {
        stmt.execute("drop view if exists " + tableName + "_vw");
        stmt.execute("drop table if exists " + tableName);
      }
      conn.commit();
    }
  }

  String runMigration() {
    MigrationRunner runner = new MigrationRunner(config);
    runner.run(dataSource);
    return "OK";
  }
}
