package io.ebean.migration.runner;

import io.avaje.applog.AppLog;
import io.ebean.datasource.DataSourceConfig;
import io.ebean.datasource.DataSourceFactory;
import io.ebean.datasource.DataSourcePool;
import io.ebean.migration.MigrationConfig;
import io.ebean.migration.MigrationRunner;
import io.ebean.test.containers.PostgresContainer;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Map;

import static java.lang.System.Logger.Level.INFO;
import static org.assertj.core.api.Assertions.fail;

class MigrationEarlyModeTest {

  private static final System.Logger log = AppLog.getLogger(MigrationEarlyModeTest.class);

  String un = "mig_early";
  String pw = "test";

  private static PostgresContainer createPostgres() {
    return PostgresContainer.builder("15")
      .port(9823)
      .containerName("pg15")
      .user("mig_early")
      .dbName("mig_early")
      .build();
  }

  // @Disabled
  @Test
  void testEarlyMode() {
    createPostgres().startWithDropCreate();

    String url = createPostgres().jdbcUrl();
    DataSourcePool dataSource = dataSource(url);

    MigrationConfig config = new MigrationConfig();
    config.setDbUrl(url);
    config.setDbUsername(un);
    config.setDbPassword(pw);
    config.setMigrationPath("dbmig_postgres_early");
    config.setRunPlaceholderMap(Map.of("my_table_name", "my_table"));

    // legacy mode
    new MigrationRunner(config).run(dataSource);

    // early mode
    log.log(INFO, "-- EARLY MODE -- ");
    config.setEarlyChecksumMode(true);
    new MigrationRunner(config).run(dataSource);

    log.log(INFO, "-- RE-RUN EARLY MODE -- ");
    new MigrationRunner(config).run(dataSource);

    log.log(INFO, "-- EARLY MODE AGAIN -- ");
    config.setEarlyChecksumMode(false);
    new MigrationRunner(config).run(dataSource);

    config.setRunPlaceholderMap(Map.of("my_table_name", "my_table", "other_table_name", "other"));
    config.setMigrationPath("dbmig_postgres_early1");

    new MigrationRunner(config).run(dataSource);

    config.setEarlyChecksumMode(true);
    new MigrationRunner(config).run(dataSource);

  }

  private DataSourcePool dataSource(String url) {
    DataSourceConfig dataSourceConfig = new DataSourceConfig()
      .setUrl(url)
      .setUsername(un)
      .setPassword(pw);
    return DataSourceFactory.create("mig_early", dataSourceConfig);
  }

//  private static void dropTable(Connection conn) throws SQLException {
//    try (PreparedStatement stmt = conn.prepareStatement("drop table if exists db_migration")) {
//      stmt.executeUpdate();
//    }
//  }

}
