package io.ebean.migration;

import io.ebean.migration.runner.LocalMigrationResource;
import io.ebean.migration.runner.LocalMigrationResources;
import io.ebean.migration.runner.MigrationPlatform;
import io.ebean.migration.runner.MigrationSchema;
import io.ebean.migration.runner.MigrationTable;
import io.ebean.migration.util.JdbcClose;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/**
 * Runs the DB migration typically on application start.
 */
public class MigrationRunner {

  private static final Logger logger = LoggerFactory.getLogger(MigrationRunner.class);

  private final MigrationConfig migrationConfig;

  private List<LocalMigrationResource> checkMigrations;

  public MigrationRunner(MigrationConfig migrationConfig) {
    this.migrationConfig = migrationConfig;
  }

  /**
   * Run by creating a DB connection from driver, url, username defined in MigrationConfig.
   */
  public void run() {
    run(migrationConfig.createConnection());
  }

  /**
   * Return the migrations that would be applied if the migration is run.
   */
  public List<LocalMigrationResource> checkState() {
    run(migrationConfig.createConnection(), true);
    return checkMigrations;
  }

  /**
   * Return the migrations that would be applied if the migration is run.
   */
  public List<LocalMigrationResource> checkState(DataSource dataSource) {
    run(getConnection(dataSource), true);
    return checkMigrations;
  }

  /**
   * Return the migrations that would be applied if the migration is run.
   */
  public List<LocalMigrationResource> checkState(Connection connection) {
    run(connection, true);
    return checkMigrations;
  }

  /**
   * Run using the connection from the DataSource.
   */
  public void run(DataSource dataSource) {
    run(getConnection(dataSource));
  }

  private Connection getConnection(DataSource dataSource) {

    String username = migrationConfig.getDbUsername();
    try {
      if (username == null) {
        return dataSource.getConnection();
      }
      logger.debug("using db user [{}] to run migrations ...", username);
      return dataSource.getConnection(username, migrationConfig.getDbPassword());
    } catch (SQLException e) {
      String msgSuffix = (username == null) ? "" : " using user [" + username + "]";
      throw new IllegalArgumentException("Error trying to connect to database for DB Migration" + msgSuffix, e);
    }
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
  private void run(Connection connection, boolean checkStateMode) {

    LocalMigrationResources resources = new LocalMigrationResources(migrationConfig);
    if (!resources.readResources()) {
      logger.debug("no migrations to check");
      return;
    }

    try {
      connection.setAutoCommit(false);
      MigrationPlatform platform = derivePlatformName(migrationConfig, connection);

      new MigrationSchema(migrationConfig, connection).createAndSetIfNeeded();

      MigrationTable table = new MigrationTable(migrationConfig, connection, checkStateMode);
      table.createIfNeededAndLock(platform);

      runMigrations(resources, table, checkStateMode);
      connection.commit();

    } catch (MigrationException e) {
      JdbcClose.rollback(connection);
      throw e;

    } catch (Exception e) {
      JdbcClose.rollback(connection);
      throw new RuntimeException(e);

    } finally {
      JdbcClose.close(connection);
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
        logger.info("dbinit migration version:{}  local migrations:{}  checkState:{}", initVersion, localVersions.size(), checkStateMode);
        checkMigrations = table.runInit(initVersion, localVersions);
        return;
      }
    }

    logger.info("local migrations:{}  existing migrations:{}  checkState:{}", localVersions.size(), table.size(), checkStateMode);
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

    String platformName = migrationConfig.getPlatformName();
    if (platformName == null) {
      platformName = DbNameUtil.normalise(connection);
      migrationConfig.setPlatformName(platformName);
    }

    return DbNameUtil.platform(platformName);
  }

}
