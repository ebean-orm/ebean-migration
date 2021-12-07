package io.ebean.migration;

import io.ebean.migration.runner.LocalMigrationResource;
import io.ebean.migration.runner.LocalMigrationResources;
import io.ebean.migration.runner.MigrationPlatform;
import io.ebean.migration.runner.MigrationSchema;
import io.ebean.migration.runner.MigrationTable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/**
 * Runs the DB migration typically on application start.
 */
public class MigrationRunner {

  static final Logger log = LoggerFactory.getLogger("io.ebean.migration");

  protected final MigrationConfig migrationConfig;

  protected List<LocalMigrationResource> checkMigrations;

  public MigrationRunner(MigrationConfig migrationConfig) {
    this.migrationConfig = migrationConfig;
  }

  /**
   * Return the migrations that would be applied if the migration is run.
   */
  @Nonnull
  public List<LocalMigrationResource> checkState() {
    return checkState(migrationConfig.createConnection());
  }

  /**
   * Return the migrations that would be applied if the migration is run.
   */
  @Nonnull
  public List<LocalMigrationResource> checkState(DataSource dataSource) {
    return checkState(getConnection(dataSource));
  }

  /**
   * Return the migrations that would be applied if the migration is run.
   */
  @Nonnull
  public List<LocalMigrationResource> checkState(Connection connection) {
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
      log.debug("using db user [{}] to run migrations ...", username);
      return dataSource.getConnection(username, migrationConfig.getDbPassword());
    } catch (SQLException e) {
      String msgSuffix = (username == null) ? "" : " using user [" + username + "]";
      throw new IllegalArgumentException("Error trying to connect to database for DB Migration" + msgSuffix, e);
    }
  }

  /**
   * Run the migrations if there are any that need running.
   */
  protected void run(Connection connection, boolean checkStateMode) {
    try {
      LocalMigrationResources resources = new LocalMigrationResources(migrationConfig);
      if (!resources.readResources()) {
        log.debug("no migrations to check");
        return;
      }

      connection.setAutoCommit(false);
      MigrationPlatform platform = derivePlatformName(migrationConfig, connection);
      new MigrationSchema(migrationConfig, connection).createAndSetIfNeeded();

      MigrationTable table = new MigrationTable(migrationConfig, connection, checkStateMode, platform);
      table.createIfNeededAndLock();

      runMigrations(resources, table, checkStateMode);
      connection.commit();

      table.runNonTransactional();

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
  private void runMigrations(LocalMigrationResources resources, MigrationTable table, boolean checkStateMode) throws SQLException {
    // get the migrations in version order
    List<LocalMigrationResource> localVersions = resources.getVersions();

    if (table.isEmpty()) {
      LocalMigrationResource initVersion = getInitVersion();
      if (initVersion != null) {
        // run using a dbinit script
        log.info("dbinit migration version:{}  local migrations:{}  checkState:{}", initVersion, localVersions.size(), checkStateMode);
        checkMigrations = table.runInit(initVersion, localVersions);
        return;
      }
    }

    log.info("Local migrations:{}  existing migrations:{}  checkState:{}", localVersions.size(), table.size(), checkStateMode);
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
    String platformName = migrationConfig.getPlatformName();
    if (platformName == null) {
      platformName = DbNameUtil.normalise(connection);
      migrationConfig.setPlatformName(platformName);
    }
    return DbNameUtil.platform(platformName);
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
      log.warn("Error closing connection", e);
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
      log.warn("Error on connection rollback", e);
    }
  }
}
