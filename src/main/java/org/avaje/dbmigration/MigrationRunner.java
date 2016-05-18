package org.avaje.dbmigration;

import org.avaje.dbmigration.runner.LocalMigrationResource;
import org.avaje.dbmigration.runner.LocalMigrationResources;
import org.avaje.dbmigration.runner.MigrationTable;
import org.avaje.dbmigration.util.JdbcClose;
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

  private static final Logger logger = LoggerFactory.getLogger("org.avaje.dbmigration.MigrationRunner");

  private final MigrationConfig migrationConfig;

  public MigrationRunner(MigrationConfig migrationConfig) {
    this.migrationConfig = migrationConfig;
  }

  /**
   * Run by creating a DB connection from driver, url, username defined in MigrationConfig.
   */
  public void run() {

    Connection connection = migrationConfig.createConnection();
    run(connection);
  }

  public void run(DataSource dataSource) {

    String username = migrationConfig.getDbUsername();
    String password =  migrationConfig.getDbPassword();
    if (username == null) {
      throw new IllegalStateException("No DB migration user specified (to run the db migration) ?");
    }

    try {
      Connection connection = dataSource.getConnection(username, password);
      logger.debug("using db user [{}] to run migrations ...", username);
      run(connection);

    } catch (SQLException e) {
      throw new IllegalArgumentException("Error trying to connect to database using DB Migration user [" + username + "]", e);
    }
  }

  /**
   * Run the migrations if there are any that need running.
   */
  public void run(Connection connection) {

    LocalMigrationResources resources = new LocalMigrationResources(migrationConfig);
    if (!resources.readResources()) {
      logger.debug("no migrations to check");
      return;
    }

    try {
      connection.setAutoCommit(false);
      runMigrations(resources, connection);

      connection.commit();

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
  private void runMigrations(LocalMigrationResources resources, Connection connection) throws SQLException, IOException {

    MigrationTable table = new MigrationTable(migrationConfig, connection);
    table.createIfNeeded();

    // get the migrations in version order
    List<LocalMigrationResource> localVersions = resources.getVersions();

    logger.info("local migrations:{}  existing migrations:{}", localVersions.size(), table.size());

    LocalMigrationResource priorVersion = null;

    // run migrations in order
    for (int i = 0; i < localVersions.size(); i++) {
      LocalMigrationResource localVersion = localVersions.get(i);
      if (!table.shouldRun(localVersion, priorVersion)) {
        break;
      }
      priorVersion = localVersion;
      connection.commit();
    }
  }

}
