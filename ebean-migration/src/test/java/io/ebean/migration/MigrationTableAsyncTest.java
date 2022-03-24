package io.ebean.migration;

import io.ebean.datasource.DataSourceConfig;
import io.ebean.datasource.DataSourcePool;
import io.ebean.datasource.DataSourceFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

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

  @Test
  public void testDb2() throws Exception {
    // Works
    config.setMigrationPath("dbmig");
    config.setDbUsername("unit");
    config.setDbPassword("test");
    config.setDbUrl("jdbc:db2://localhost:50000/unit:currentSchema=ASYNC;");
    runTest();
  }

  @Test
  public void testMariaDb() throws Exception {
    // does not properly lock migtable
    config.setMigrationPath("dbmig");
    config.setDbUsername("test_ebean");
    config.setDbPassword("test");
    config.setDbUrl("jdbc:mysql://localhost:14307/test_ebean");
    runTest();
  }

  @Test
  public void testSqlServer() throws Exception {
    // works
    config.setMigrationPath("dbmig_sqlserver");
    config.setDbUsername("test_ebean");
    config.setDbPassword("SqlS3rv#r");
    config.setDbUrl("jdbc:sqlserver://localhost:9435;databaseName=test_ebean;sendTimeAsDateTime=false");
    runTest();
  }

  
  @Test
  public void testH2() throws Exception {
    // thread A looses the lock while thread B runs the migrations.
    config.setMigrationPath("dbmig");
    config.setDbUsername("sa");
    config.setDbPassword("");
    config.setDbUrl("jdbc:h2:mem:dbAsync;LOCK_TIMEOUT=100000");
    runTest();

  }

  private void runTest() throws SQLException, InterruptedException, ExecutionException {
    DataSourceConfig dataSourceConfig = new DataSourceConfig();
    dataSourceConfig.setUrl(config.getDbUrl());
    dataSourceConfig.setUsername(config.getDbUsername());
    dataSourceConfig.setPassword(config.getDbPassword());
    dataSource = DataSourceFactory.create("test", dataSourceConfig);
    dropTable("migtable");
    dropTable("m1");
    dropTable("m2");
    dropTable("m3");
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

  private void dropTable(String tableName) throws SQLException {
    try (Connection conn = dataSource.getConnection();
      Statement stmt = conn.createStatement()) {
      if (conn.getMetaData().getDatabaseProductName().toLowerCase().contains("db2")) {
        stmt.execute("begin\n"
          + "if exists (select tabname from syscat.tables where lcase(tabname) = '" + tableName + "' and tabschema = current_schema) then\n"
          + " prepare stmt from 'drop table " + tableName + "';\n"
          + " execute stmt;\n"
          + "end if;\n"
          + "end");
      } else {
        stmt.execute("drop table if exists " + tableName);
      }
      conn.commit();
    }
  }

  String runMigration() throws Exception {
    MigrationRunner runner = new MigrationRunner(config);
    runner.run(dataSource);
    return "OK";
  }
}
