package io.ebean.migration;

import io.ebean.datasource.DataSourceConfig;
import io.ebean.datasource.DataSourceFactory;
import io.ebean.datasource.DataSourcePool;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

class AutoRunnerTest {

  @Test
  void run() throws SQLException {

    DataSourceConfig dataSourceConfig = new DataSourceConfig();
    dataSourceConfig.setUrl("jdbc:h2:mem:testsAutoRun");
    dataSourceConfig.setUsername("sa");
    dataSourceConfig.setPassword("");

    DataSourcePool dataSource = DataSourceFactory.create("test", dataSourceConfig);
    try {
      Properties properties = new Properties();
      properties.setProperty("dbmigration.migrationPath", "dbmig_autorun");

      AutoRunner autoRunner = new AutoRunner();
      autoRunner.setDefaultDbSchema("other");
      autoRunner.loadProperties(properties);
      autoRunner.run(dataSource);


      assertTrue(executeQuery(dataSource));
    } finally {
      dataSource.shutdown();
    }
  }

  private boolean executeQuery(DataSourcePool dataSource) throws SQLException {
    try (final Connection connection = dataSource.getConnection()) {
      try (final PreparedStatement stmt = connection.prepareStatement("select * from m1")) {
        try (final ResultSet resultSet = stmt.executeQuery()) {
          return true;
        }
      }
    }
  }
}
