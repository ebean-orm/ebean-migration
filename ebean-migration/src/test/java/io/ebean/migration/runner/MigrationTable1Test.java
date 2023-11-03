package io.ebean.migration.runner;

import io.ebean.migration.MigrationConfig;
import io.ebean.migration.MigrationRunner;
import io.ebean.datasource.DataSourceConfig;
import io.ebean.datasource.DataSourcePool;
import io.ebean.datasource.DataSourceFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class MigrationTable1Test {

  private static MigrationConfig config;
  private static DataSourcePool dataSource;
  private MigrationPlatform platform = new MigrationPlatform();

  @BeforeEach
  public void setUp() {
    config = new MigrationConfig();
    config.setDbUsername("sa");
    config.setDbPassword("");
    config.setDbUrl("jdbc:h2:mem:db2");

    DataSourceConfig dataSourceConfig = new DataSourceConfig();
    dataSourceConfig.setUrl("jdbc:h2:mem:db2");
    dataSourceConfig.setUsername("sa");
    dataSourceConfig.setPassword("");
    dataSource = DataSourceFactory.create("test", dataSourceConfig);
  }

  @AfterEach
  public void shutdown() {
    dataSource.shutdown();
  }


  private MigrationTable migrationTable(Connection conn) {
    var fc = new FirstCheck(config, conn, platform);
    return new MigrationTable(fc, false);
  }

  @Test
  public void testMigrationTableBase() throws Exception {

    config.setMigrationPath("dbmig");

    MigrationRunner runner = new MigrationRunner(config);
    runner.run(dataSource);
    try (Connection conn = dataSource.getConnection()) {
      MigrationTable table = migrationTable(conn);
      table.createIfNeededAndLock();
      assertThat(table.versions()).containsExactly("hello", "1.1", "1.2", "1.2.1", "m2_view");

      List<String> rawVersions = new ArrayList<>();
      try (PreparedStatement stmt = conn.prepareStatement("select mversion from db_migration order by id")) {
        try (ResultSet rset = stmt.executeQuery()) {
          while (rset.next()) {
            rawVersions.add(rset.getString(1));
          }
        }
      }
      assertThat(rawVersions).containsExactly("0", "hello", "1.1", "1.2", "1.2.1", "m2_view");
      table.unlockMigrationTable();
      conn.rollback();
    }
  }


  @Test
  public void testMigrationTableRepeatableOk() throws Exception {

    config.setMigrationPath("tabletest1");

    MigrationRunner runner = new MigrationRunner(config);
    runner.run(dataSource);

    try (Connection conn = dataSource.getConnection()) {
      MigrationTable table = migrationTable(conn);
      table.createIfNeededAndLock();
      assertThat(table.versions()).containsExactly("1.1");
      table.unlockMigrationTable();
      conn.rollback();
    }

    config.setMigrationPath("tabletest2");

    runner = new MigrationRunner(config);
    runner.run(dataSource);

    try (Connection conn = dataSource.getConnection()) {
      MigrationTable table = migrationTable(conn);
      table.createIfNeededAndLock();
      assertThat(table.versions()).containsExactly("1.1", "1.2", "m2_view");
      table.unlockMigrationTable();
      conn.rollback();
    }
  }

  @Test
  public void testMigrationTableRepeatableFail() throws Exception {

    config.setMigrationPath("tabletest1");
    config.setAllowErrorInRepeatable(true);

    MigrationRunner runner = new MigrationRunner(config);
    runner.run(dataSource);

    try (Connection conn = dataSource.getConnection()) {
      MigrationTable table = migrationTable(conn);
      table.createIfNeededAndLock();
      assertThat(table.versions()).containsExactly("1.1");
      table.unlockMigrationTable();
      conn.rollback();
    }

    // now execute corrupt migration
    config.setMigrationPath("tabletest2-err");

    MigrationRunner runner2 = new MigrationRunner(config);

    runner2.run(dataSource);

    try (Connection conn = dataSource.getConnection()) {

      List<String> m3Content = new ArrayList<>();
      try (
          // the next statement will fail, if "1.3__add_m3" is not executed
          PreparedStatement stmt = conn.prepareStatement("select * from m3");
          ResultSet result = stmt.executeQuery()) {
        while (result.next()) {
          m3Content.add(result.getString(2));
        }
      }

      // the next statement will fail, if "1.3__add_m3" is not executed
      //
      // BUT: currently, we will fail here. that means, the 1.3 script creates the table
      // but does not insert the data. (there was not commit) This might be also a bug in H2
      assertThat(m3Content).contains("text with ; sign");

      // we expect, that 1.1 and 1.2 is executed (but not the R__ script)
      MigrationTable table = migrationTable(conn);
      table.createIfNeededAndLock();
      assertThat(table.versions()).containsExactly("1.1", "1.2");
      conn.rollback();
    }
  }
}
