package io.ebean.migration.runner;

import io.ebean.migration.MigrationConfig;
import io.ebean.migration.MigrationResource;

import java.sql.Connection;
import java.util.List;

/**
 * @author Roland Praml, FOCONIS AG
 */
public class MigrationEngineDb extends MigrationEngine {
  /**
   * Create with the MigrationConfig.
   *
   * @param migrationConfig
   * @param checkStateOnly
   */
  public MigrationEngineDb(MigrationConfig migrationConfig, boolean checkStateOnly) {
    super(migrationConfig, checkStateOnly);
  }

  /**
   * Run the migrations if there are any that need running.
   *
   * @param connection the connection to run on. Note the connection will be closed.
   */
  /*
  public List<MigrationResource> run(Connection connection, Database db) {
    try {
      return run(new DefaultMigrationContext(migrationConfig, connection, db));
    } finally {
      close(connection);
    }
  }*/
}
