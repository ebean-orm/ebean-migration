package io.ebean.migration;

import io.ebean.migration.runner.LocalMigrationResource;
import io.ebean.migration.runner.LocalMigrationResources;
import io.ebean.migration.runner.MigrationTable;
import io.ebean.migration.runner.MigrationSchema;
import io.ebean.migration.util.JdbcClose;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.io.IOException;
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
  public void run(Connection connection, boolean checkStateMode) {

    LocalMigrationResources resources = new LocalMigrationResources(migrationConfig);
    if (!resources.readResources()) {
      logger.debug("no migrations to check");
      return;
    }

    try {
      connection.setAutoCommit(false);

      MigrationSchema schema = new MigrationSchema(migrationConfig, connection);
      schema.createAndSetIfNeeded();

      runMigrations(resources, connection, checkStateMode);
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
  private void runMigrations(LocalMigrationResources resources, Connection connection, boolean checkStateMode) throws SQLException, IOException {
    derivePlatformName(migrationConfig, connection);

    MigrationTable table = new MigrationTable(migrationConfig, connection, checkStateMode);
    table.createIfNeededAndLock();

    // get the migrations in version order
    List<LocalMigrationResource> localVersions = resources.getVersions();

    logger.info("local migrations:{}  existing migrations:{}  checkState:{}", localVersions.size(), table.size(), checkStateMode);

    LocalMigrationResource priorVersion = null;

    // run migrations in order
    for (LocalMigrationResource localVersion : localVersions) {
      if (!table.shouldRun(localVersion, priorVersion)) {
        break;
      }
      priorVersion = localVersion;
    }
    if (checkStateMode) {
      checkMigrations = table.ran();
    }
  }

  /**
   * Derive and set the platform name if required.
   */
  private void derivePlatformName(MigrationConfig migrationConfig, Connection connection) {

    if (migrationConfig.getPlatformName() == null) {
      migrationConfig.setPlatformName(DbNameUtil.normalise(connection));
    }
  }

}
