package io.ebean.migration.runner;

import io.ebean.datasource.DataSourceConfig;
import io.ebean.datasource.DataSourceFactory;
import io.ebean.datasource.DataSourcePool;
import io.ebean.migration.MigrationConfig;
import io.ebean.test.containers.PostgresContainer;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.fail;

class MigrationTableCreateTableRaceTest {

  private final MigrationPlatform platform = new MigrationPlatform();

  private static PostgresContainer createPostgres() {
    PostgresContainer.Builder builder = PostgresContainer.builder("15")
      .port(9823);
    builder.containerName("pg15");
    builder.user("mig_create_test");
    builder.dbName("mig_create_test");
    return builder.build();
  }

  // @Disabled
  @Test
  void testRaceCondition_expect_loserOfCreateTableCanPerformTableExistsCheck() throws SQLException, IOException {
    PostgresContainer postgresContainer = createPostgres();
    postgresContainer.start();

    String url = postgresContainer.jdbcUrl();
    String un = "mig_create_test";
    String pw = "test";

    MigrationConfig config = new MigrationConfig();
    config.setDbUrl(url);
    config.setDbUsername(un);
    config.setDbPassword(pw);

    DataSourceConfig dataSourceConfig = new DataSourceConfig();
    dataSourceConfig.setUrl(url);
    dataSourceConfig.setUsername(un);
    dataSourceConfig.setPassword(pw);
    DataSourcePool dataSource = DataSourceFactory.create("mig_create_test", dataSourceConfig);

    try (Connection conn = dataSource.getConnection()) {
      dropTable(conn);

      var fc = new FirstCheck(config, new DefaultMigrationContext(config, conn), platform);
      MigrationTable table = new MigrationTable(fc, false);
      table.createTable();
      try {
        // simulate losing the race, this createTable() will fail as the table exists
        table.createTable();
      } catch (SQLException e) {
        // need rollback to allow further use of the connection
        if (!table.tableExists()) {
          fail("Table should exist");
        }
      }
      // cleanup
      dropTable(conn);
    } finally {
      dataSource.shutdown();
    }
  }

  private static void dropTable(Connection conn) throws SQLException {
    try (PreparedStatement stmt = conn.prepareStatement("drop table if exists db_migration")) {
      stmt.executeUpdate();
    }
  }

}
