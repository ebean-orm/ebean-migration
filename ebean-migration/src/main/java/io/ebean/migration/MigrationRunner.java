package io.ebean.migration;

import io.avaje.applog.AppLog;
import io.ebean.migration.runner.MigrationEngine;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import static java.lang.System.Logger.Level.*;

/**
 * Runs the DB migration typically on application start.
 */
public class MigrationRunner {

  static final System.Logger log = AppLog.getLogger("io.ebean.migration");

  protected final MigrationConfig migrationConfig;

  public MigrationRunner(MigrationConfig migrationConfig) {
    this.migrationConfig = migrationConfig;
  }

  /**
   * Return the migrations that would be applied if the migration is run.
   */
  public List<MigrationResource> checkState() {
    return checkState(migrationConfig.createConnection());
  }

  /**
   * Return the migrations that would be applied if the migration is run.
   */
  public List<MigrationResource> checkState(DataSource dataSource) {
    return checkState(connection(dataSource));
  }

  /**
   * Return the migrations that would be applied if the migration is run.
   */
  public List<MigrationResource> checkState(Connection connection) {
    return run(connection, true);
  }

  /**
   * Return the migrations that would be applied if the migration is run.
   */
  public List<MigrationResource> checkState(MigrationContext context) {
    return run(context, true);
  }

  /**
   * Run by creating a DB connection from driver, url, username defined in MigrationConfig.
   */
  public void run() {
    run(migrationConfig.createConnection());
  }

  /**
   * Run using the connection from the DataSource.
   */
  public void run(DataSource dataSource) {
    run(connection(dataSource));
  }

  /**
   * Run the migrations if there are any that need running.
   */
  public void run(Connection connection) {
    run(connection, false);
  }

  /**
   * Run the migrations if there are any that need running.
   */
  public void run(MigrationContext context) {
    run(context, false);
  }

  private Connection connection(DataSource dataSource) {
    String username = migrationConfig.getDbUsername();
    try {
      if (username == null) {
        return dataSource.getConnection();
      }
      log.log(DEBUG, "using db user [{0}] to run migrations ...", username);
      return dataSource.getConnection(username, migrationConfig.getDbPassword());
    } catch (SQLException e) {
      String msgSuffix = (username == null) ? "" : " using user [" + username + "]";
      throw new IllegalArgumentException("Error trying to connect to database for DB Migration" + msgSuffix, e);
    }
  }

  /**
   * Run the migrations if there are any that need running.
   */
  private List<MigrationResource> run(Connection connection, boolean checkStateOnly) {
    return new MigrationEngine(migrationConfig, checkStateOnly).run(connection);
  }

  /**
   * Run the migrations if there are any that need running.
   */
  private List<MigrationResource> run(MigrationContext context, boolean checkStateOnly) {
    return new MigrationEngine(migrationConfig, checkStateOnly).run(context);
  }
}
