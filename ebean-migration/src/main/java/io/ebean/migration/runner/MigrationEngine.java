package io.ebean.migration.runner;

import io.avaje.applog.AppLog;
import io.ebean.migration.MigrationConfig;
import io.ebean.migration.MigrationException;
import io.ebean.migration.MigrationResource;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.lang.System.Logger.Level.*;
import static java.lang.System.Logger.Level.WARNING;

/**
 * Actually runs the migrations.
 */
public class MigrationEngine {

  static final System.Logger log = AppLog.getLogger("io.ebean.migration");

  private final MigrationConfig migrationConfig;
  private final boolean checkStateOnly;
  private final boolean fastMode;
  private int fastModeCount;
  private MigrationTable table;

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
   */
  public List<MigrationResource> run(Connection connection) {
    try {
      LocalMigrationResources resources = new LocalMigrationResources(migrationConfig);
      if (!resources.readResources() && !resources.readInitResources()) {
        log.log(DEBUG, "no migrations to check");
        return Collections.emptyList();
      }
      long startMs = System.currentTimeMillis();
      setAutoCommitFalse(connection);
      table = initMigrationTable(connection);
      if (fastMode && fastModeCheck(resources.versions())) {
        long checkMs = System.currentTimeMillis() - startMs;
        log.log(INFO, "DB migrations completed in {0}ms - totalMigrations:{1}", checkMs, fastModeCount);
        return Collections.emptyList();
      }

      initialiseMigrationTable(connection);
      try {
        List<MigrationResource> result = runMigrations(resources.versions());
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
    } finally {
      close(connection);
    }
  }

  private static void setAutoCommitFalse(Connection connection) {
    try {
      connection.setAutoCommit(false);
    } catch (SQLException e) {
      throw new MigrationException("Error running DB migrations", e);
    }
  }

  private MigrationTable initMigrationTable(Connection connection) {
    final MigrationPlatform platform = derivePlatformName(migrationConfig, connection);
    return new MigrationTable(migrationConfig, connection, checkStateOnly, platform);
  }

  private boolean fastModeCheck(List<LocalMigrationResource> versions) {
    try {
      final List<MigrationMetaRow> rows = table.fastRead();
      if (rows.size() != versions.size() + 1) {
        // difference in count of migrations
        return false;
      }
      final Map<String, Integer> dbChecksums = dbChecksumMap(rows);
      for (LocalMigrationResource local : versions) {
        Integer dbChecksum = dbChecksums.get(local.key());
        if (dbChecksum == null) {
          // no match, unexpected missing migration
          return false;
        }
        int localChecksum = checksumFor(local);
        if (localChecksum != dbChecksum) {
          // no match, perhaps repeatable migration change
          return false;
        }
      }
      // successful fast check
      fastModeCount = versions.size();
      return true;
    } catch (SQLException e) {
      // probably migration table does not exist
      return false;
    }
  }

  private static Map<String, Integer> dbChecksumMap(List<MigrationMetaRow> rows) {
    return rows.stream().collect(Collectors.toMap(MigrationMetaRow::version, MigrationMetaRow::checksum));
  }

  private int checksumFor(LocalMigrationResource local) {
    if (local instanceof LocalUriMigrationResource) {
      return ((LocalUriMigrationResource)local).checksum();
    } else if (local instanceof LocalDdlMigrationResource) {
      return Checksum.calculate(local.content());
    } else {
      return ((LocalJdbcMigrationResource) local).checksum();
    }
  }

  private void initialiseMigrationTable(Connection connection) {
    try {
      table.createIfNeededAndLock();
    } catch (Throwable e) {
      rollback(connection);
      throw new MigrationException("Error initialising db migrations table", e);
    }
  }

  /**
   * Run all the migrations as needed.
   */
  private List<MigrationResource> runMigrations(List<LocalMigrationResource> localVersions) throws SQLException {
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
