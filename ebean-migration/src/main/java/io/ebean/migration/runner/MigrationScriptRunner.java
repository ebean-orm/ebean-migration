package io.ebean.migration.runner;

import io.ebean.ddlrunner.DdlRunner;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Runs the DDL migration scripts.
 */
class MigrationScriptRunner {

  private final Connection connection;

  private final MigrationPlatform platform;

  private final List<String> nonTransactional = new ArrayList<>();

  /**
   * Construct with a given connection.
   */
  MigrationScriptRunner(Connection connection, MigrationPlatform platform) {
    this.connection = connection;
    this.platform = platform;
  }

  /**
   * Execute all the DDL statements in the script.
   */
  void runScript(String content, String scriptName) throws SQLException {
    DdlRunner runner = new DdlRunner(false, scriptName, platform.ddlDetect());
    nonTransactional.addAll(runner.runAll(content, connection));
  }

  public void runNonTransactional() {
    if (!nonTransactional.isEmpty()) {
      DdlRunner runner = new DdlRunner(false, "Non-transactional DDL", platform.ddlDetect());
      runner.runNonTransactional(connection, nonTransactional);
    }
  }
}
