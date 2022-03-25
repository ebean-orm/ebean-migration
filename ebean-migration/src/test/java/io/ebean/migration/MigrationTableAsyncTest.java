package io.ebean.migration;

import io.ebean.datasource.DataSourceConfig;
import io.ebean.datasource.DataSourcePool;
import io.ebean.datasource.DataSourceFactory;
import io.ebean.docker.commands.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
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

  @Disabled
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
  public void testMySqlDb() throws Exception {
    // init mysql docker container
    MySqlConfig conf = new MySqlConfig("8.0");
    conf.setPort(14307);
    conf.setContainerName("mig_async_mysql");
    conf.setDbName("test_ebean");
    conf.setUser("test_ebean");
    conf.setPassword("test");
    MySqlContainer container = new MySqlContainer(conf);
    container.startWithDropCreate();

    config.setMigrationPath("dbmig");
    config.setDbUsername("test_ebean");
    config.setDbPassword("test");
    config.setDbUrl("jdbc:mysql://localhost:14307/test_ebean");
    runTest();
  }

  @Test
  public void testMariaDb() throws Exception {
    // init mariadb docker container
    MariaDBConfig conf = new MariaDBConfig("10");
    conf.setPort(14308);
    conf.setContainerName("mig_async_mariadb");
    conf.setDbName("test_ebean");
    conf.setUser("test_ebean");
    conf.setPassword("test");
    MariaDBContainer container = new MariaDBContainer(conf);
    container.startWithDropCreate();

    config.setMigrationPath("dbmig");
    config.setDbUsername("test_ebean");
    config.setDbPassword("test");
    config.setDbUrl("jdbc:mariadb://localhost:14308/test_ebean");
    runTest();
  }

  //@Disabled
  @Test
  public void testSqlServer() throws Exception {
    // init sqlserver docker container
    SqlServerConfig conf = new SqlServerConfig("2017-GA-ubuntu");
    conf.setPort(9435);
    conf.setContainerName("mig_async_sqlserver");
    conf.setDbName("test_ebean");
    conf.setUser("test_ebean");
    //conf.setPassword("SqlS3rv#r");
    SqlServerContainer container = new SqlServerContainer(conf);
    container.startWithDropCreate();

    config.setMigrationPath("dbmig_sqlserver");
    config.setDbUsername("test_ebean");
    config.setDbPassword("SqlS3rv#r");
    config.setDbUrl("jdbc:sqlserver://localhost:9435;databaseName=test_ebean;sendTimeAsDateTime=false");
    runTest();
  }


  /**
   * H2 does not work, implicit commits in DDL.
   */
  @Disabled
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

  String runMigration() {
    MigrationRunner runner = new MigrationRunner(config);
    runner.run(dataSource);
    return "OK";
  }
}
