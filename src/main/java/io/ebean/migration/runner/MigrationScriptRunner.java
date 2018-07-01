package io.ebean.migration.runner;

import io.ebean.migration.ddl.DdlRunner;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Runs the DDL migration scripts.
 */
class MigrationScriptRunner {

  private final Connection connection;

  /**
   * Construct with a given connection.
   */
  MigrationScriptRunner(Connection connection) {
    this.connection = connection;
  }

  /**
   * Execute all the DDL statements in the script.
   */
  int runScript(boolean expectErrors, String content, String scriptName) throws SQLException {

    DdlRunner runner = new DdlRunner(expectErrors, scriptName);
    return runner.runAll(content, connection);
  }
}
