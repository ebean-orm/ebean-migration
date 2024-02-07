package io.ebean.migration;

import io.ebean.datasource.DataSourceConfig;
import io.ebean.datasource.DataSourceFactory;
import io.ebean.datasource.DataSourcePool;
import org.junit.jupiter.api.Test;

class MigrationRunner_emptyTest {

  @Test
  void run_withDatasource_expectNoLeak() {
    DataSourceConfig dataSourceConfig = new DataSourceConfig();
    dataSourceConfig.setDriver("org.h2.Driver");
    dataSourceConfig.setUrl("jdbc:h2:mem:dbEmpty1");
    dataSourceConfig.setUsername("sa");
    dataSourceConfig.setPassword("");

    DataSourcePool dataSource = DataSourceFactory.create("test-empty", dataSourceConfig);
    try {
      MigrationConfig config = new MigrationConfig();
      config.setMigrationPath("empty-dbmig");

      MigrationRunner runner = new MigrationRunner(config);
      runner.run(dataSource);
    } finally {
      dataSource.shutdown();
    }
  }

}
