package io.ebean.migration;

import io.ebean.migration.custom.JavaDbMigration;
import io.ebean.migration.custom.JavaDbMigrationHandler;
import io.ebean.migration.custom.LogHandler;
import io.ebean.migration.custom.RunStaticMethodHandler;
import io.ebean.migration.runner.LocalMigrationResource;
import org.avaje.datasource.DataSourceConfig;
import org.avaje.datasource.DataSourcePool;
import org.avaje.datasource.Factory;
import org.testng.annotations.Test;

import java.sql.Connection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

public class MigrationRunnerTest {

  private static String staticCalledText;

  private MigrationConfig createMigrationConfig() {
    MigrationConfig config = new MigrationConfig();
    config.setDbUsername("sa");
    config.setDbPassword("");
    config.setDbDriver("org.h2.Driver");
    config.setDbUrl("jdbc:h2:mem:db1");

    return config;
  }

  @Test
  public void run_when_createConnection() {

    MigrationConfig config = createMigrationConfig();

    config.setMigrationPath("dbmig");
    MigrationRunner runner = new MigrationRunner(config);

    List<LocalMigrationResource> check = runner.checkState();
    assertThat(check).hasSize(3);

    runner.run();
  }

  @Test
  public void run_when_fileSystemResources() {

    MigrationConfig config = createMigrationConfig();

    config.setMigrationPath("filesystem:test-fs-resources/fsdbmig");
    MigrationRunner runner = new MigrationRunner(config);
    runner.run();
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

    Factory factory = new Factory();
    DataSourcePool dataSource = factory.createPool("test", dataSourceConfig);

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
    List<LocalMigrationResource> checkState = runner.checkState(dataSource);
    assertThat(checkState).hasSize(1);
    assertThat(checkState.get(0).getVersion().asString()).isEqualTo("1.3");

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

  /**
   * Run this integration test manually against CockroachDB.
   */
  @Test
  public void run_with_handler() {

    AtomicInteger counter = new AtomicInteger();

    MigrationConfig config = createMigrationConfig();
    config.setDbUsername("sa");
    config.setDbPassword("");
    config.setDbDriver("org.h2.Driver");
    config.setDbUrl("jdbc:h2:mem:test-handler");
    config.setMigrationPath("dbmig-handler");
    config.registerCustomCommandHandler("runStatic",new RunStaticMethodHandler());
    Map<String, JavaDbMigration> migrations = new HashMap<>();
    migrations.put("mig1", (conn, arg) -> counter.set((int) arg.get(0)));
    config.registerCustomCommandHandler("run",new JavaDbMigrationHandler(migrations::get));
    config.registerCustomCommandHandler("logger",new LogHandler());


    staticCalledText = null;

    MigrationRunner runner = new MigrationRunner(config);
    runner.run();

    assertThat(staticCalledText).isEqualTo("Hello\nWorld");
    assertThat(counter.get()).isEqualTo(42);
  }

  /**
   * Run this integration test manually against CockroachDB.
   */
  @Test(enabled = false)
  public void cockroach_integrationTest() {

    MigrationConfig config = createMigrationConfig();
    config.setDbUsername("unit");
    config.setDbPassword("unit");
    config.setDbDriver("org.postgresql.Driver");
    config.setDbUrl("jdbc:postgresql://127.0.0.1:26257/unit");
    config.setMigrationPath("dbmig-roach");

    MigrationRunner runner = new MigrationRunner(config);
    runner.run();

  }

  public static void myMigration(Connection conn, List<Object> args) {
    assertThat(conn).isNotNull();
    staticCalledText = (String) args.get(0);
  }

  public static void demo(Connection conn, List<Object> args) {
    for (int i = 0; i < args.size(); i++) {
      Object arg = args.get(i);
      String cls = arg == null ? "null" : arg.getClass().getName();
      System.out.println("Arg #" + i + ": Type: " + cls + ", Value: " + arg);
    }
  }

  public static void createTable(Connection conn, List<Object> args) {
    StringBuilder sb = new StringBuilder();
    sb.append("create table ").append(args.get(0));
    sb.append(" (");
    for (int i = 1; i < args.size(); i++) {
      if (i > 1) {
        sb.append(", ");
      }
      sb.append(args.get(i));
    }
    sb.append(')');
    System.out.println(sb);
  }
}