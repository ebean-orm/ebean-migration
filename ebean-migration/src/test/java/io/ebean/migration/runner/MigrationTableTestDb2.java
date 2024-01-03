package io.ebean.migration.runner;

import io.ebean.migration.MigrationConfig;
import io.ebean.datasource.DataSourceConfig;
import io.ebean.datasource.DataSourcePool;
import io.ebean.datasource.DataSourceFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.sql.Connection;

public class MigrationTableTestDb2 {

  private static MigrationConfig config;
  private static DataSourcePool dataSource;
  private final MigrationPlatform platform = new MigrationPlatform();

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
      var fc = new FirstCheck(config, new DefaultMigrationContext(config, conn), platform);
      MigrationTable table = new MigrationTable(fc, false);
      table.createIfNeededAndLock();
      table.unlockMigrationTable();
      table.createIfNeededAndLock();
      table.unlockMigrationTable();
      conn.commit();
    }
  }
}
