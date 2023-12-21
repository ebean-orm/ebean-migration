package io.ebean.migration.runner;

import io.avaje.applog.AppLog;
import io.ebean.migration.MigrationConfig;
import io.ebean.migration.MigrationContext;
import io.ebean.migration.MigrationException;
import io.ebean.migration.MigrationResource;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import static java.lang.System.Logger.Level.*;
import static java.lang.System.Logger.Level.WARNING;
import static java.util.Collections.emptyList;

/**
 * Actually runs the migrations.
 */
public class MigrationEngine {

  static final System.Logger log = AppLog.getLogger("io.ebean.migration");

  private final MigrationConfig migrationConfig;
  private final boolean checkStateOnly;
  private final boolean fastMode;

  /**
   * Create with the MigrationConfig.
   */
  public MigrationEngine(MigrationConfig migrationConfig, boolean checkStateOnly) {
    this.migrationConfig = migrationConfig;
    this.checkStateOnly = checkStateOnly;
    this.fastMode = !checkStateOnly && migrationConfig.isFastMode();
  }

  /**
   * Run the migrations if there are any that need running.
   *
   * @param connection the connection to run on. Note the connection will be closed.
   */
  public List<MigrationResource> run(Connection connection) {
    try {
      return run(new DefaultMigrationContext(migrationConfig, connection));
    } finally {
      close(connection);
    }
  }

  /**
   * Run the migrations if there are any that need running. (Does not close connection)
   */
  public List<MigrationResource> run(MigrationContext context) {

    long startMs = System.currentTimeMillis();
    LocalMigrationResources resources = new LocalMigrationResources(migrationConfig);
    if (!resources.readResources() && !resources.readInitResources()) {
      log.log(DEBUG, "no migrations to check");
      return emptyList();
    }

    var connection = context.connection();
    long splitMs = System.currentTimeMillis() - startMs;
    final var platform = derivePlatform(migrationConfig, connection);
    final var firstCheck = new FirstCheck(migrationConfig, context, platform);
    if (fastMode && firstCheck.fastModeCheck(resources.versions())) {
      long checkMs = System.currentTimeMillis() - startMs;
      log.log(INFO, "DB migrations completed in {0}ms - totalMigrations:{1} readResources:{2}ms", checkMs, firstCheck.count(), splitMs);
      return emptyList();
    }
    // ensure running with autoCommit false
    setAutoCommitFalse(connection);

    final MigrationTable table = initialiseMigrationTable(firstCheck, connection);
    try {
      List<MigrationResource> result = runMigrations(table, resources.versions());
      connection.commit();
      if (!checkStateOnly) {
        long commitMs = System.currentTimeMillis();
        log.log(INFO, "DB migrations completed in {0}ms - executed:{1} totalMigrations:{2} mode:{3}", (commitMs - startMs), table.count(), table.size(), table.mode());
        int countNonTransactional = table.runNonTransactional();
        if (countNonTransactional > 0) {
          log.log(INFO, "Non-transactional DB migrations completed in {0}ms - executed:{1}", (System.currentTimeMillis() - commitMs), countNonTransactional);
        }
      }
      return result;
    } catch (MigrationException e) {
      rollback(connection);
      throw e;
    } catch (Throwable e) {
      log.log(ERROR, "Perform rollback due to DB migration error", e);
      rollback(connection);
      throw new MigrationException("Error running DB migrations", e);
    } finally {
      table.unlockMigrationTable();
    }
  }

  private static void setAutoCommitFalse(Connection connection) {
    try {
      connection.setAutoCommit(false);
    } catch (SQLException e) {
      throw new MigrationException("Error running DB migrations", e);
    }
  }

  private MigrationTable initialiseMigrationTable(FirstCheck firstCheck, Connection connection) {
    try {
      final MigrationTable table = firstCheck.initTable(checkStateOnly);
      table.createIfNeededAndLock();
      return table;
    } catch (Throwable e) {
      rollback(connection);
      throw new MigrationException("Error initialising db migrations table", e);
    }
  }

  /**
   * Run all the migrations as needed.
   */
  private List<MigrationResource> runMigrations(MigrationTable table, List<LocalMigrationResource> localVersions) throws SQLException {
    // get the migrations in version order
    if (table.isEmpty()) {
      LocalMigrationResource initVersion = lastInitVersion();
      if (initVersion != null) {
        // run using a dbinit script
        log.log(INFO, "dbinit migration version:{0}  local migrations:{1}  checkState:{2}", initVersion, localVersions.size(), checkStateOnly);
        return table.runInit(initVersion, localVersions);
      }
    }
    return table.runAll(localVersions);
  }

  /**
   * Return the last init migration.
   */
  private LocalMigrationResource lastInitVersion() {
    LocalMigrationResources initResources = new LocalMigrationResources(migrationConfig);
    if (initResources.readInitResources()) {
      List<LocalMigrationResource> initVersions = initResources.versions();
      if (!initVersions.isEmpty()) {
        return initVersions.get(initVersions.size() - 1);
      }
    }
    return null;
  }

  /**
   * Return the platform deriving from connection if required.
   */
  private MigrationPlatform derivePlatform(MigrationConfig migrationConfig, Connection connection) {
    final String platform = migrationConfig.getPlatform();
    if (platform != null) {
      return DbNameUtil.platform(platform);
    }
    // determine the platform from the db connection
    String derivedPlatformName = DbNameUtil.normalise(connection);
    migrationConfig.setPlatform(derivedPlatformName);
    return DbNameUtil.platform(derivedPlatformName);
  }

  /**
   * Close the connection logging if an error occurs.
   */
  private void close(Connection connection) {
    try {
      if (connection != null) {
        connection.close();
      }
    } catch (SQLException e) {
      log.log(WARNING, "Error closing connection", e);
    }
  }

  /**
   * Rollback the connection logging if an error occurs.
   */
  static void rollback(Connection connection) {
    try {
      if (connection != null) {
        connection.rollback();
      }
    } catch (SQLException e) {
      log.log(WARNING, "Error on connection rollback", e);
    }
  }
}
