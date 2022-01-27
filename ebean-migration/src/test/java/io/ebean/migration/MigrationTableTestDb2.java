package io.ebean.migration;

import io.ebean.migration.runner.MigrationPlatform;
import io.ebean.migration.runner.MigrationTable;
import io.ebean.datasource.DataSourceConfig;
import io.ebean.datasource.DataSourcePool;
import io.ebean.datasource.DataSourceFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class MigrationTableTestDb2 {

  private static MigrationConfig config;
  private static DataSourcePool dataSource;
  private MigrationPlatform platform = new MigrationPlatform();

  @BeforeEach
  public void setUp() {
    config = new MigrationConfig();
    config.setDbUsername("unit");
    config.setDbPassword("test");
    config.setMetaTable("bla");
    config.setDbUrl("jdbc:db2://localhost:50000/unit:currentSchema=Sch_2;"); // Sch2 will work

    DataSourceConfig dataSourceConfig = new DataSourceConfig();
    dataSourceConfig.setUrl("jdbc:db2://localhost:50000/unit:currentSchema=Sch_2;");
    dataSourceConfig.setUsername("unit");
    dataSourceConfig.setPassword("test");
    dataSource = DataSourceFactory.create("test", dataSourceConfig);
  }

  @AfterEach
  public void shutdown() {
    dataSource.shutdown();
  }

  @Disabled // run test manually
  @Test
  public void testMigrationTableBase() throws Exception {

    config.setMigrationPath("dbmig");

    try (Connection conn = dataSource.getConnection()) {
      MigrationTable table = new MigrationTable(config, conn, false, platform);
      table.createIfNeededAndLock();
      table.createIfNeededAndLock();
      conn.commit();
    }
  }
}
