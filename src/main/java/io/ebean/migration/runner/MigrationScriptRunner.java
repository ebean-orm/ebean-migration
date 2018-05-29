package io.ebean.migration.runner;

import io.ebean.migration.custom.CustomCommandHandler;
import io.ebean.migration.ddl.DdlRunner;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * Runs the DDL migration scripts.
 */
public class MigrationScriptRunner {

  private final Connection connection;

  /**
   * Construct with a given connection.
   */
  public MigrationScriptRunner(Connection connection) {
    this.connection = connection;
  }

  int runScript(boolean expectErrors, String content, String scriptName) throws SQLException {
    return runScript(expectErrors, content, scriptName, null);
  }

  /**
   * Execute all the DDL statements in the script.
   */
  int runScript(boolean expectErrors, String content, String scriptName, Map<String, CustomCommandHandler> handlers) throws SQLException {

    DdlRunner runner = new DdlRunner(expectErrors, scriptName);
    runner.setCustomCommandHandlers(handlers);
    return runner.runAll(content, connection);
  }
}
