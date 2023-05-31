package io.ebean.migration.runner;

import io.avaje.applog.AppLog;
import io.ebean.migration.MigrationConfig;
import io.ebean.migration.MigrationException;
import io.ebean.migration.MigrationResource;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

import static java.lang.System.Logger.Level.*;
import static java.lang.System.Logger.Level.WARNING;

/**
 * Actually runs the migrations.
 */
public class MigrationEngine {

  static final System.Logger log = AppLog.getLogger("io.ebean.migration");

  private final MigrationConfig migrationConfig;

  /**
   * Create with the MigrationConfig.
   */
  public MigrationEngine(MigrationConfig migrationConfig) {
    this.migrationConfig = migrationConfig;
  }

  /**
   * Run the migrations if there are any that need running.
   */
  public List<MigrationResource> run(Connection connection, boolean checkStateOnly) {
    try {
      LocalMigrationResources resources = new LocalMigrationResources(migrationConfig);
      if (!resources.readResources() && !resources.readInitResources()) {
        log.log(DEBUG, "no migrations to check");
        return Collections.emptyList();
      }

      long startMs = System.currentTimeMillis();
      connection.setAutoCommit(false);
      MigrationPlatform platform = derivePlatformName(migrationConfig, connection);
      new MigrationSchema(migrationConfig, connection).createAndSetIfNeeded();

      MigrationTable table = new MigrationTable(migrationConfig, connection, checkStateOnly, platform);
      table.createIfNeededAndLock();
      try {
        List<LocalMigrationResource> migrations = resources.versions();
        List<MigrationResource> result = runMigrations(migrations, table, checkStateOnly);
        connection.commit();
        if (!checkStateOnly) {
          long commitMs = System.currentTimeMillis();
          log.log(INFO, "DB migrations completed in {0}ms - executed:{1} totalMigrations:{2}", (commitMs - startMs), table.count(), migrations.size());
          int countNonTransactional = table.runNonTransactional();
          if (countNonTransactional > 0) {
            log.log(INFO, "Non-transactional DB migrations completed in {0}ms - executed:{1}", (System.currentTimeMillis() - commitMs), countNonTransactional);
          }
        }
        return result;
      } finally {
        table.unlockMigrationTable();
      }

    } catch (MigrationException e) {
      rollback(connection);
      throw e;

    } catch (Exception e) {
      rollback(connection);
      throw new RuntimeException(e);

    } finally {
      close(connection);
    }
  }

  /**
   * Run all the migrations as needed.
   */
  private List<MigrationResource> runMigrations(List<LocalMigrationResource> localVersions, MigrationTable table, boolean checkStateMode) throws SQLException {
    // get the migrations in version order
    if (table.isEmpty()) {
      LocalMigrationResource initVersion = getInitVersion();
      if (initVersion != null) {
        // run using a dbinit script
        log.log(INFO, "dbinit migration version:{0}  local migrations:{1}  checkState:{2}", initVersion, localVersions.size(), checkStateMode);
        return table.runInit(initVersion, localVersions);
      }
    }
    return table.runAll(localVersions);
  }

  /**
   * Return the last init migration.
   */
  private LocalMigrationResource getInitVersion() {
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
  private MigrationPlatform derivePlatformName(MigrationConfig migrationConfig, Connection connection) {
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
  private void rollback(Connection connection) {
    try {
      if (connection != null) {
        connection.rollback();
      }
    } catch (SQLException e) {
      log.log(WARNING, "Error on connection rollback", e);
    }
  }
}
