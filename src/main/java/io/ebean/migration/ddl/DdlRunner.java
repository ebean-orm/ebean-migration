package io.ebean.migration.ddl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.StringReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Runs DDL scripts.
 */
public class DdlRunner {

  protected static final Logger logger = LoggerFactory.getLogger("io.ebean.DDL");

  private final DdlParser parser;

  private final String scriptName;

  private final boolean expectErrors;

  private boolean commitOnCreateIndex;

  /**
   * Construct with a script name (for logging) and flag indicating if errors are expected.
   */
  public DdlRunner(boolean expectErrors, String scriptName) {
    this(expectErrors, scriptName, new NoAutoCommit());
  }

  /**
   * Create additionally with ddlAutoCommit.
   */
  public DdlRunner(boolean expectErrors, String scriptName, DdlAutoCommit ddlAutoCommit) {
    this.expectErrors = expectErrors;
    this.scriptName = scriptName;
    this.parser = new DdlParser(ddlAutoCommit);
  }

  /**
   * Needed for Cockroach DB. Needs commit after create index and before alter table add FK.
   */
  public void setCommitOnCreateIndex() {
    commitOnCreateIndex = true;
  }

  /**
   * Parse the content into sql statements and execute them in a transaction.
   *
   * @return The non-transactional statements that should execute later.
   */
  public List<String> runAll(String content, Connection connection) throws SQLException {
    List<String> statements = parser.parse(new StringReader(content));
    runStatements(statements, connection);
    return parser.getNonTransactional();
  }

  /**
   * Execute all the statements in a single transaction.
   */
  private void runStatements(List<String> statements, Connection connection) throws SQLException {

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
  }

  /**
   * Execute the statement.
   */
  private void runStatement(boolean expectErrors, String oneOf, String stmt, Connection c) throws SQLException {
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

    try (PreparedStatement statement = c.prepareStatement(stmt)) {
      statement.execute();
    } catch (SQLException e) {
      if (expectErrors) {
        logger.debug(" ... ignoring error executing " + getSummary(stmt) + "  error: " + e.getMessage());
      } else {
        String msg = "Error executing stmt[" + stmt + "] error[" + e.getMessage() + "]";
        throw new SQLException(msg, e);
      }
    }
  }

  private String getSummary(String s) {
    if (s.length() > 80) {
      return s.substring(0, 80).trim().replace('\n', ' ') + "...";
    }
    return s.replace('\n', ' ');
  }

  /**
   * Run any non-transactional statements from the just parsed script.
   */
  public int runNonTransactional(Connection connection) {
    final List<String> nonTransactional = parser.getNonTransactional();
    return !nonTransactional.isEmpty() ? runNonTransactional(connection, nonTransactional) : 0;
  }

  /**
   * Run the non-transactional statements with auto commit true.
   */
  public int runNonTransactional(Connection connection, List<String> nonTransactional) {
    int count = 0;
    String sql = null;
    try {
      logger.debug("running {} non-transactional migration statements", nonTransactional.size());
      connection.setAutoCommit(true);
      for (int i = 0; i < nonTransactional.size(); i++) {
        sql = nonTransactional.get(i);
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
          logger.debug("executing - {}", sql);
          statement.execute();
          count++;
        }
      }
      return count;

    } catch (SQLException e) {
      logger.error("Error running non-transaction migration: " + sql, e);
      return count;
    } finally {
      try {
        connection.setAutoCommit(false);
      } catch (SQLException e) {
        logger.error("Error resetting connection autoCommit to false", e);
      }
    }
  }
}
