package io.ebean.migration;

import io.ebean.datasource.DataSourceConfig;
import io.ebean.datasource.DataSourceFactory;
import io.ebean.datasource.DataSourcePool;
import org.junit.jupiter.api.Test;

class MigrationRunner_PatchResetTest {

  @Test
  void patchReset() {

    String url = "jdbc:h2:mem:patchReset";
    DataSourceConfig dataSourceConfig = new DataSourceConfig()
      .setUrl(url)
      .setUsername("sa")
      .setPassword("");

    DataSourcePool dataSource = DataSourceFactory.create("test", dataSourceConfig);
    try {
      MigrationConfig config = new MigrationConfig();
      config.setPlatform("h2");

      config.setMigrationPath("indexPatchReset_0");
      MigrationRunner runner = new MigrationRunner(config);
      runner.run(dataSource);

      // add an index file now, expect automatically go to early mode + patch checksums
      config.setMigrationPath("indexPatchReset_1");
      config.setPatchResetChecksumOn("*");
      new MigrationRunner(config).run(dataSource);

    } finally {
      dataSource.shutdown();
    }
  }

}
