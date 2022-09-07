package io.ebean.migration.runner;

import io.ebean.datasource.DataSourceConfig;
import io.ebean.datasource.DataSourceFactory;
import io.ebean.datasource.DataSourcePool;
import io.ebean.docker.commands.PostgresContainer;
import io.ebean.migration.MigrationConfig;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.fail;

class MigrationTableCreateTableRaceTest {

  private MigrationPlatform platform = new MigrationPlatform();

  private static PostgresContainer createPostgres() {
    PostgresContainer.Builder builder = PostgresContainer.newBuilder("13")
      .port(9823);
    builder.containerName("test_ebean_migration_pg13");
    builder.user("mig_test");
    builder.dbName("mig_test");
    return builder.build();
  }

  @Disabled
  @Test
  void testRaceCondition_expect_loserOfCreateTableCanPerformTableExistsCheck() throws SQLException, IOException {
    PostgresContainer postgresContainer = createPostgres();
    postgresContainer.start();

    String url = postgresContainer.jdbcUrl();
    String un = "mig_test";
    String pw = "test";

    MigrationConfig config = new MigrationConfig();
    config.setDbUrl(url);
    config.setDbUsername(un);
    config.setDbPassword(pw);

    DataSourceConfig dataSourceConfig = new DataSourceConfig();
    dataSourceConfig.setUrl(url);
    dataSourceConfig.setUsername(un);
    dataSourceConfig.setPassword(pw);
    DataSourcePool dataSource = DataSourceFactory.create("createTableTest", dataSourceConfig);


    try (Connection conn = dataSource.getConnection()) {
      try (PreparedStatement stmt = conn.prepareStatement("drop table if exists db_migration")) {
        stmt.executeUpdate();
      }

      MigrationTable table = new MigrationTable(config, conn, false, platform);
      table.createTable();
      try {
        table.createTable();
      } catch (SQLException e) {
        // need rollback to allow further use of the connection
        if (!table.tableExists()) {
          fail("Table should exist");
        }
      }
    }
  }

}
