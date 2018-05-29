package io.ebean.migration.ddl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.ebean.migration.custom.CustomCommandHandler;

import java.io.StringReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Runs DDL scripts.
 */
public class DdlRunner {

  protected static final Logger logger = LoggerFactory.getLogger("io.ebean.DDL");

  private DdlParser ddlParser = new DdlParser();

  private final String scriptName;

  private final boolean expectErrors;

  private boolean commitOnCreateIndex;

  private Map<String, CustomCommandHandler> customCommandHandlers;
  /**
   * Construct with a script name (for logging) and flag indicating if errors are expected.
   */
  public DdlRunner(boolean expectErrors, String scriptName) {
    this.expectErrors = expectErrors;
    this.scriptName = scriptName;
  }

  /**
   * Needed for Cockroach DB. Needs commit after create index and before alter table add FK.
   */
  public void setCommitOnCreateIndex() {
    commitOnCreateIndex = true;
  }

  /**
   * Returns the CustomCommandHandler.
   */
  public Map<String, CustomCommandHandler> getCustomCommandHandlers() {
    return customCommandHandlers;
  }

  /**
   * Sets the CustomCommandHandler to handle custom migration commands.
   */
  public void setCustomCommandHandlers(Map<String, CustomCommandHandler> customCommandHandlers) {
    if (customCommandHandlers == null) {
      this.customCommandHandlers = null;
    } else {
      this.customCommandHandlers = new HashMap<>(customCommandHandlers);
    }
  }

  /**
   * Registers a CustomCommandHandler to handle custom migration commands.
   */
  public void registerCustomCommandHandler(String prefix, CustomCommandHandler customCommandHandler) {
    if (customCommandHandlers == null) {
      customCommandHandlers = new HashMap<>();
    }
    this.customCommandHandlers.put(prefix, customCommandHandler);
  }

  /**
   * Parse the content into sql statements and execute them in a transaction.
   */
  public int runAll(String content, Connection connection) throws SQLException {

    List<String> statements = ddlParser.parse(new StringReader(content));
    return runStatements(statements, connection);
  }

  /**
   * Execute all the statements in a single transaction.
   */
  private int runStatements(List<String> statements, Connection connection) throws SQLException {

    List<String> noDuplicates = new ArrayList<>();

    for (String statement : statements) {
      if (!noDuplicates.contains(statement)) {
        noDuplicates.add(statement);
      }
    }

    logger.info("Executing {} - {} statements", scriptName, noDuplicates.size());

    for (int i = 0; i < noDuplicates.size(); i++) {
      String xOfy = (i + 1) + " of " + noDuplicates.size();
      String ddl = noDuplicates.get(i);
      runStatement(expectErrors, xOfy, ddl, connection);
      if (commitOnCreateIndex && ddl.startsWith("create index ")) {
        logger.debug("commit on create index ...");
        connection.commit();
      }
    }

    return noDuplicates.size();
  }

  /**
   * Execute the statement.
   */
  private void runStatement(boolean expectErrors, String oneOf, String stmt, Connection c) throws SQLException {

    PreparedStatement pstmt = null;
    try {

      // trim and remove trailing ; or /
      stmt = stmt.trim();
      if (stmt.endsWith(";")) {
        stmt = stmt.substring(0, stmt.length() - 1);
      } else if (stmt.endsWith("/")) {
        stmt = stmt.substring(0, stmt.length() - 1);
      }

      if (stmt.isEmpty()) {
        logger.debug("skip empty statement at " + oneOf);
        return;
      }

      if (logger.isDebugEnabled()) {
        logger.debug("executing " + oneOf + " " + getSummary(stmt));
      }

      if (!handleCustomCommand(stmt, c)) {
        pstmt = c.prepareStatement(stmt);
        pstmt.execute();
      }

    } catch (SQLException e) {
      if (expectErrors) {
        logger.debug(" ... ignoring error executing " + getSummary(stmt) + "  error: " + e.getMessage());
      } else {
        String msg = "Error executing stmt[" + stmt + "] error[" + e.getMessage() + "]";
        throw new SQLException(msg, e);
      }

    } finally {
      if (pstmt != null) {
        try {
          pstmt.close();
        } catch (SQLException e) {
          logger.error("Error closing pstmt", e);
        }
      }
    }
  }

  /**
   * Checks if this is a custom command and handles it.
   */
  private boolean handleCustomCommand(String stmt, Connection c) throws SQLException {
    if (customCommandHandlers == null || customCommandHandlers.isEmpty()) {
      return false;
    }
    int pos = stmt.indexOf(':');
    if (pos == -1) {
      return false;
    }
    String key = stmt.substring(0, pos);
    CustomCommandHandler handler = customCommandHandlers.get(key);
    if (handler == null) {
      return false;
    }
    String cmd = stmt.substring(pos+1);
    handler.handle(c, cmd);
    return true;
  }

  private String getSummary(String s) {
    if (s.length() > 80) {
      return s.substring(0, 80).trim() + "...";
    }
    return s;
  }


}
