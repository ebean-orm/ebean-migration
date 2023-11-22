package io.ebean.migration;

import io.ebean.datasource.DataSourceConfig;
import io.ebean.datasource.DataSourceFactory;
import io.ebean.datasource.DataSourcePool;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.fail;

public class MigrationRunnerTest {

  private MigrationConfig createMigrationConfig() {
    MigrationConfig config = new MigrationConfig();
    config.setDbUsername("sa");
    config.setDbPassword("");
    config.setDbUrl("jdbc:h2:mem:dbMx1");

    return config;
  }

  public static boolean javaMigrationExecuted;

  @Test
  public void run_when_createConnection() {

    javaMigrationExecuted = false;
    MigrationConfig config = createMigrationConfig();

    config.setMigrationPath("dbmig");
    MigrationRunner runner = new MigrationRunner(config);
    List<MigrationResource> check = runner.checkState();
    assertThat(check).hasSize(5);

    assertThat(check.get(0).content()).contains("-- do nothing");
    assertThat(check.get(1).content()).contains("create table m1");
    assertThat(check.get(2).content()).contains("create table m3");
    assertThat(check.get(3).location()).isEqualTo("dbmig_idx/V1_2_1__test.class");
    assertThat(javaMigrationExecuted).isFalse();
    runner.run();
    assertThat(javaMigrationExecuted).isTrue();
  }

  @Test
  public void win_with_idx_file() {

    javaMigrationExecuted = false;
    MigrationConfig config = createMigrationConfig();

    config.setMigrationPath("dbmig_idx");
    MigrationRunner runner = new MigrationRunner(config);
    List<MigrationResource> check = runner.checkState();
    assertThat(check).hasSize(5);

    assertThat(check.get(0).content()).contains("-- do nothing");
    assertThat(check.get(1).content()).contains("create table m1");
    assertThat(check.get(2).content()).contains("create table m3");
    assertThat(check.get(3).location()).isEqualTo("dbmig_idx/V1_2_1__test.class");
    assertThat(javaMigrationExecuted).isFalse();
    runner.run();
    assertThat(javaMigrationExecuted).isTrue();
  }

  @Test
  public void run_when_fileSystemResources() {

    MigrationConfig config = createMigrationConfig();

    config.setMigrationPath("filesystem:test-fs-resources/fsdbmig");
    MigrationRunner runner = new MigrationRunner(config);
    runner.run();
  }

  @Test
  public void run_when_error() throws SQLException {

    DataSourceConfig dataSourceConfig = new DataSourceConfig();
    dataSourceConfig.setDriver("org.h2.Driver");
    dataSourceConfig.setUrl("jdbc:h2:mem:err.db");
    dataSourceConfig.setUsername("sa");
    dataSourceConfig.setPassword("");

    DataSourcePool dataSource = DataSourceFactory.create("test", dataSourceConfig);

    MigrationConfig config = createMigrationConfig();
    config.setMigrationPath("dbmig_error");
    MigrationRunner runner = new MigrationRunner(config);
    try {
      runner.run(dataSource);
    } catch (Exception expected) {
      try (Connection connection = dataSource.getConnection()) {
        try (var pstmt = connection.prepareStatement("select count(*) from m1")) {
          try (var resultSet = pstmt.executeQuery()) {
            assertThat(resultSet.next()).isTrue();
            int val = resultSet.getInt(1);
            assertThat(val).isEqualTo(0);
          }
        } catch (SQLException ex) {
          fail(ex);
        }
      }
    }
  }

  @Test
  public void run_when_suppliedConnection() {

    MigrationConfig config = createMigrationConfig();
    Connection connection = config.createConnection();

    config.setMigrationPath("dbmig");
    MigrationRunner runner = new MigrationRunner(config);
    runner.run(connection);
  }

  @Test
  public void run_when_suppliedDataSource() {

    DataSourceConfig dataSourceConfig = new DataSourceConfig();
    dataSourceConfig.setDriver("org.h2.Driver");
    dataSourceConfig.setUrl("jdbc:h2:mem:tests");
    dataSourceConfig.setUsername("sa");
    dataSourceConfig.setPassword("");

    DataSourcePool dataSource = DataSourceFactory.create("test", dataSourceConfig);

    MigrationConfig config = createMigrationConfig();
    config.setMigrationPath("dbmig");

    MigrationRunner runner = new MigrationRunner(config);
    runner.run(dataSource);
    System.out.println("-- run second time --");
    runner.run(dataSource);

    // simulate change to repeatable migration
    config.setMigrationPath("dbmig3");
    System.out.println("-- run third time --");
    runner.run(dataSource);

    config.setMigrationPath("dbmig4");

    config.setPatchResetChecksumOn("m2_view,1.2");
    List<MigrationResource> checkState = runner.checkState(dataSource);
    assertThat(checkState).hasSize(1);
    assertThat(checkState.get(0).version().asString()).isEqualTo("1.3");

    config.setPatchInsertOn("1.3");
    checkState = runner.checkState(dataSource);
    assertThat(checkState).isEmpty();

    System.out.println("-- run forth time --");
    runner.run(dataSource);

    System.out.println("-- run fifth time --");
    checkState = runner.checkState(dataSource);
    assertThat(checkState).isEmpty();
    runner.run(dataSource);

  }

  @Test
  public void run_with_dbinit() throws SQLException {

    DataSourceConfig dataSourceConfig = new DataSourceConfig();
    dataSourceConfig.setDriver("org.h2.Driver");
    dataSourceConfig.setUrl("jdbc:h2:mem:testsDbInit");
    dataSourceConfig.setUsername("sa");
    dataSourceConfig.setPassword("");

    DataSourcePool dataSource = DataSourceFactory.create("test", dataSourceConfig);

    MigrationConfig config = createMigrationConfig();
    config.setDbUrl("jdbc:h2:mem:testsDbInit");
    config.setMigrationPath("dbmig5_base");
    config.setMigrationInitPath("dbmig5_init");

    MigrationRunner runner = new MigrationRunner(config);
    runner.run(dataSource);

    MigrationRunner runner2 = new MigrationRunner(config);
    runner2.run(dataSource);

    try (final Connection connection = dataSource.getConnection()) {
      final List<String> names = migrationNames(connection);
      assertThat(names).containsExactly("<init>", "some_i", "m4", "some_r");
    }
  }

  @Test
  public void run_only_dbinit_available() throws SQLException {
    DataSourceConfig dataSourceConfig = new DataSourceConfig();
    dataSourceConfig.setDriver("org.h2.Driver");
    dataSourceConfig.setUrl("jdbc:h2:mem:testsDbInit2");
    dataSourceConfig.setUsername("sa");
    dataSourceConfig.setPassword("");

    DataSourcePool dataSource = DataSourceFactory.create("test", dataSourceConfig);

    MigrationConfig config = createMigrationConfig();
    config.setDbUrl("jdbc:h2:mem:testsDbInit2");
    config.setMigrationPath("dbmig6_base");
    config.setMigrationInitPath("dbmig6_init");

    MigrationRunner runner = new MigrationRunner(config);
    runner.run(dataSource);

    MigrationRunner runner2 = new MigrationRunner(config);
    runner2.run(dataSource);

    try (final Connection connection = dataSource.getConnection()) {
      final List<String> names = migrationNames(connection);
      assertThat(names).containsExactly("<init>", "m4");
    }
  }

  @Test
  public void run_with_min_version() {

    DataSourceConfig dataSourceConfig = new DataSourceConfig();
    dataSourceConfig.setDriver("org.h2.Driver");
    dataSourceConfig.setUrl("jdbc:h2:mem:testsMinV");
    dataSourceConfig.setUsername("sa");
    dataSourceConfig.setPassword("");

    DataSourcePool dataSource = DataSourceFactory.create("test", dataSourceConfig);

    MigrationConfig config = createMigrationConfig();
    config.setMigrationPath("dbmig");
    config.setMinVersion("1.3"); // dbmig must run, if DB is empty!
    new MigrationRunner(config).run(dataSource);


    config = createMigrationConfig();
    config.setMigrationPath("dbmig3");
    config.setMinVersion("1.3");
    config.setMinVersionFailMessage("Must run dbmig2 first.");

    MigrationRunner runner3 = new MigrationRunner(config);
    assertThatThrownBy(() -> runner3.run(dataSource))
      .isInstanceOf(MigrationException.class)
      .hasMessageContaining("Must run dbmig2 first. MigrationVersion mismatch: v1.2.1 < v1.3");

    // now run dbmig2, as intended by error message
    config = createMigrationConfig();
    config.setMigrationPath("dbmig2");
    new MigrationRunner(config).run(dataSource);

    // dbmig3 should pass now
    runner3.run(dataSource);
  }

  @Test
  public void run_init_with_min_version() {

    DataSourceConfig dataSourceConfig = new DataSourceConfig();
    dataSourceConfig.setDriver("org.h2.Driver");
    dataSourceConfig.setUrl("jdbc:h2:mem:testsMinVinit");
    dataSourceConfig.setUsername("sa");
    dataSourceConfig.setPassword("");

    DataSourcePool dataSource = DataSourceFactory.create("test", dataSourceConfig);

    // init
    MigrationConfig config = createMigrationConfig();
    config.setMigrationPath("dbmig5_base");
    config.setMigrationInitPath("dbmig5_init");
    config.setMinVersion("2.0"); // init must run, although DB is empty!
    new MigrationRunner(config).run(dataSource);

    // test if migration detects correct init-version (1.3)
    config = createMigrationConfig();
    config.setMigrationPath("dbmig3");
    config.setMinVersion("2.0");

    MigrationRunner runner = new MigrationRunner(config);
    assertThatThrownBy(() -> runner.run(dataSource))
      .isInstanceOf(MigrationException.class)
      .hasMessageContaining("MigrationVersion mismatch: v1.3 < v2.0");
  }

  @Test
  public void run_with_skipMigration() throws SQLException {

    DataSourceConfig dataSourceConfig = new DataSourceConfig();
    dataSourceConfig.setDriver("org.h2.Driver");
    dataSourceConfig.setUrl("jdbc:h2:mem:testsSkipMigration");
    dataSourceConfig.setUsername("sa");
    dataSourceConfig.setPassword("");

    MigrationConfig config = createMigrationConfig();
    // not actually run the migrations but populate the migration table
    config.setSkipMigrationRun(true);
    config.setMigrationPath("dbmig");

    DataSourcePool dataSource = DataSourceFactory.create("skipMigration", dataSourceConfig);
    new MigrationRunner(config).run(dataSource);

    // assert migrations are in the migration table
    try (final Connection connection = dataSource.getConnection()) {
      final List<String> names = migrationNames(connection);
      assertThat(names).contains("<init>", "hello", "initial", "add_m3", "test", "m2_view");
    }

    // assert the migrations didn't actually run (create the tables etc)
    try (final Connection connection = dataSource.getConnection()) {
      singleQueryResult(connection, "select acol from m3");
      fail();
    } catch (SQLException e) {
      assertThat(e.getMessage()).contains("Table \"M3\" not found;");
    }
  }


  /**
   * Run this integration test manually against CockroachDB.
   */
  @Disabled
  @Test
  public void cockroach_integrationTest() {

    MigrationConfig config = createMigrationConfig();
    config.setDbUsername("unit");
    config.setDbPassword("unit");
    config.setDbUrl("jdbc:postgresql://127.0.0.1:26257/unit");
    config.setMigrationPath("dbmig-roach");

    MigrationRunner runner = new MigrationRunner(config);
    runner.run();
  }

  private List<String> migrationNames(Connection connection) throws SQLException {
    return singleQueryResult(connection, "select mcomment from db_migration");
  }

  private List<String> singleQueryResult(Connection connection, String sql) throws SQLException {
    List<String> names = new ArrayList<>();
    try (final PreparedStatement statement = connection.prepareStatement(sql)) {
      try (final ResultSet resultSet = statement.executeQuery()) {
        while (resultSet.next()) {
          names.add(resultSet.getString(1));
        }
      }
    }
    return names;
  }
}
