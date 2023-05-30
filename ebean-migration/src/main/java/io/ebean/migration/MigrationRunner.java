package io.ebean.migration;

import io.avaje.applog.AppLog;
import io.ebean.migration.runner.*;

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

  protected List<MigrationResource> checkMigrations;

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
    return checkState(getConnection(dataSource));
  }

  /**
   * Return the migrations that would be applied if the migration is run.
   */
  public List<MigrationResource> checkState(Connection connection) {
    run(connection, true);
    return checkMigrations;
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
    run(getConnection(dataSource));
  }

  /**
   * Run the migrations if there are any that need running.
   */
  public void run(Connection connection) {
    run(connection, false);
  }

  private Connection getConnection(DataSource dataSource) {
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
  protected void run(Connection connection, boolean checkStateOnly) {
    try {
      LocalMigrationResources resources = new LocalMigrationResources(migrationConfig);
      if (!resources.readResources() && !resources.readInitResources()) {
        log.log(DEBUG, "no migrations to check");
        return;
      }

      long startMs = System.currentTimeMillis();
      connection.setAutoCommit(false);
      MigrationPlatform platform = derivePlatformName(migrationConfig, connection);
      new MigrationSchema(migrationConfig, connection).createAndSetIfNeeded();

      MigrationTable table = new MigrationTable(migrationConfig, connection, checkStateOnly, platform);
      table.createIfNeededAndLock();
      try {
        List<LocalMigrationResource> migrations = resources.getVersions();
        runMigrations(migrations, table, checkStateOnly);
        connection.commit();
        if (!checkStateOnly) {
          long commitMs = System.currentTimeMillis();
          log.log(INFO, "DB migrations completed in {0}ms - executed:{1} totalMigrations:{2}", (commitMs - startMs), table.count(), migrations.size());
          int countNonTransactional = table.runNonTransactional();
          if (countNonTransactional > 0) {
            log.log(INFO, "Non-transactional DB migrations completed in {0}ms - executed:{1}", (System.currentTimeMillis() - commitMs), countNonTransactional);
          }
        }
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
  private void runMigrations(List<LocalMigrationResource> localVersions, MigrationTable table, boolean checkStateMode) throws SQLException {
    // get the migrations in version order
    if (table.isEmpty()) {
      LocalMigrationResource initVersion = getInitVersion();
      if (initVersion != null) {
        // run using a dbinit script
        log.log(INFO, "dbinit migration version:{0}  local migrations:{1}  checkState:{2}", initVersion, localVersions.size(), checkStateMode);
        checkMigrations = table.runInit(initVersion, localVersions);
        return;
      }
    }
    checkMigrations = table.runAll(localVersions);
  }

  /**
   * Return the last init migration.
   */
  private LocalMigrationResource getInitVersion() {
    LocalMigrationResources initResources = new LocalMigrationResources(migrationConfig);
    if (initResources.readInitResources()) {
      List<LocalMigrationResource> initVersions = initResources.getVersions();
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
