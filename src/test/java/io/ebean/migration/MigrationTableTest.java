package io.ebean.migration;

import io.ebean.migration.runner.MigrationPlatform;
import io.ebean.migration.runner.MigrationTable;
import org.avaje.datasource.DataSourceConfig;
import org.avaje.datasource.DataSourcePool;
import org.avaje.datasource.Factory;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class MigrationTableTest {

  private MigrationConfig config;
  private DataSourcePool dataSource;
  private MigrationPlatform platform = new MigrationPlatform();

  @BeforeMethod
  public void setUp() {
    config = new MigrationConfig();
    config.setDbUsername("sa");
    config.setDbPassword("");
    config.setDbDriver("org.h2.Driver");
    config.setDbUrl("jdbc:h2:mem:db2");

    DataSourceConfig dataSourceConfig = new DataSourceConfig();
    dataSourceConfig.setDriver("org.h2.Driver");
    dataSourceConfig.setUrl("jdbc:h2:mem:db2");
    dataSourceConfig.setUsername("sa");
    dataSourceConfig.setPassword("");
    Factory factory = new Factory();
    dataSource = factory.createPool("test", dataSourceConfig);
  }

  @AfterMethod
  public void shutdown() {
    dataSource.shutdown(false);
  }

  @Test
  public void testMigrationTableBase() throws Exception {

    config.setMigrationPath("dbmig");

    MigrationRunner runner = new MigrationRunner(config);
    runner.run(dataSource);
    try (Connection conn = dataSource.getConnection()) {
      MigrationTable table = new MigrationTable(config, conn, false);
      table.createIfNeededAndLock(platform);
      assertThat(table.getVersions()).containsExactly("hello", "1.1", "1.2", "1.2.1", "m2_view");

      List<String> rawVersions = new ArrayList<>();
      try (PreparedStatement stmt = conn.prepareStatement("select mversion from db_migration order by id")) {
        try (ResultSet rset = stmt.executeQuery()) {
          while (rset.next()) {
            rawVersions.add(rset.getString(1));
          }
        }
      }
      assertThat(rawVersions).containsExactly("0", "hello", "1.1", "1.2", "1.2.1", "m2_view");
      conn.rollback();
    }
  }

  @Test
  public void testMigrationTableRepeatableOk() throws Exception {

    config.setMigrationPath("tabletest1");

    MigrationRunner runner = new MigrationRunner(config);
    runner.run(dataSource);

    try (Connection conn = dataSource.getConnection()) {
      MigrationTable table = new MigrationTable(config, conn, false);
      table.createIfNeededAndLock(platform);
      assertThat(table.getVersions()).containsExactly("1.1");
      conn.rollback();
    }

    config.setMigrationPath("tabletest2");

    runner = new MigrationRunner(config);
    runner.run(dataSource);

    try (Connection conn = dataSource.getConnection()) {
      MigrationTable table = new MigrationTable(config, conn, false);
      table.createIfNeededAndLock(platform);
      assertThat(table.getVersions()).containsExactly("1.1", "1.2", "m2_view");
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
      MigrationTable table = new MigrationTable(config, conn, false);
      table.createIfNeededAndLock(platform);
      assertThat(table.getVersions()).containsExactly("1.1");
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
      MigrationTable table = new MigrationTable(config, conn, false);
      table.createIfNeededAndLock(platform);
      assertThat(table.getVersions()).containsExactly("1.1", "1.2");
      conn.rollback();
    }
  }
}
