package io.ebean.migration;

import io.ebean.datasource.DataSourceConfig;
import io.ebean.datasource.DataSourceFactory;
import io.ebean.datasource.DataSourcePool;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class MigrationRunner_FastCheckTest {

  private MigrationConfig createMigrationConfig() {
    MigrationConfig config = new MigrationConfig();
    config.setDbUsername("sa");
    config.setDbPassword("");
    config.setDbUrl("jdbc:h2:mem:dbMxFastCheck");

    return config;
  }

  @Test
  public void run_when_createConnection() {

    MigrationConfig config = createMigrationConfig();

    config.setPlatform("h2");
    config.setMigrationPath("fastcheck");

    MigrationRunner runner = new MigrationRunner(config);

    runner.run();
  }

  @Test
  public void run_when() {

    MigrationConfig config = createMigrationConfig();

    config.setPlatform("h2");
    config.setMigrationPath("index0");

    MigrationRunner runner = new MigrationRunner(config);

    runner.run();
  }

  @Test
  public void autoEnableEarlyMode_when_indexFileAddedToExistingMigrations() {

    String url = "jdbc:h2:mem:autoEnableEarlyMode";
    DataSourceConfig dataSourceConfig = new DataSourceConfig()
      .setUrl(url)
      .setUsername("sa")
      .setPassword("");

    DataSourcePool dataSource = DataSourceFactory.create("test", dataSourceConfig);

    MigrationConfig config = new MigrationConfig();
    config.setPlatform("h2");
    config.setRunPlaceholderMap(Map.of("my_table_name", "bar"));

    // initial traditional migration
    config.setMigrationPath("indexB_0");
    MigrationRunner runner = new MigrationRunner(config);
    runner.run(dataSource);

    assertThat(config.isEarlyChecksumMode()).isFalse();

    // add an index file now, expect automatically go to early mode + patch checksums
    config.setMigrationPath("indexB_1");
    new MigrationRunner(config).run(dataSource);
    assertThat(config.isEarlyChecksumMode()).isTrue();

    // early mode via <init> row + add an extra migration
    config.setMigrationPath("indexB_2");
    new MigrationRunner(config).run(dataSource);

    dataSource.shutdown();
  }

  @Test
  public void autoEnableEarlyMode() {

    MigrationConfig config = createMigrationConfig();

    config.setPlatform("h2");
    config.setMigrationPath("indexB_1");
    config.setDbUrl("jdbc:h2:mem:autoEnableEarlyMode_simple");
    config.setRunPlaceholderMap(Map.of("my_table_name", "bar"));

    MigrationRunner runner = new MigrationRunner(config);
    runner.run();

    assertThat(config.isEarlyChecksumMode()).isTrue();
  }

  @Test
  public void checkIndex_valid() {

    MigrationConfig config = createMigrationConfig();

    config.setPlatform("h2");
    config.setMigrationPath("indexB_check_valid");
    config.setForceLocalCheck(true);
    config.setDbUrl("jdbc:h2:mem:checkindex_valid");
    config.setRunPlaceholderMap(Map.of("my_table_name", "bar"));

    MigrationRunner runner = new MigrationRunner(config);
    runner.run();

  }

  @Test
  public void checkIndex_invalid() {

    MigrationConfig config = createMigrationConfig();

    config.setPlatform("h2");
    config.setMigrationPath("indexB_check_invalid");
    config.setForceLocalCheck(true);
    config.setDbUrl("jdbc:h2:mem:checkindex_invalid");
    config.setRunPlaceholderMap(Map.of("my_table_name", "bar"));

    MigrationRunner runner = new MigrationRunner(config);
    assertThatThrownBy(runner::checkState)
      .isInstanceOf(MigrationException.class)
      .hasMessageContaining("'1.2' checksum mismatch (index -123456, local -212580746)")
      .hasMessageContaining("'1.3' not in index file");

    // switch to forceLocal only. This means, we get a warning about index validations on the console,
    // but the local files are used
    config.setForceLocalCheck(false);
    config.setForceLocal(true);

    List<MigrationResource> state = runner.checkState();
    assertThat(state).hasSize(4);
    // 1.3 not mentioned in the idx.
    assertThat(state.get(3).location()).isEqualTo("indexB_check_invalid/1.3.sql");
  }
}
