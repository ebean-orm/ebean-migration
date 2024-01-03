package io.ebean.migration;

import io.ebean.migration.runner.MigrationEngine;

import java.sql.Connection;
import java.util.List;

/**
 * @author Roland Praml, FOCONIS AG
 */
public class MigrationRunnerDb extends MigrationRunner {
  public MigrationRunnerDb(MigrationConfig migrationConfig) {
    super(migrationConfig);
  }

  /**
   * Return the migrations that would be applied if the migration is run.
   */
  /* public List<MigrationResource> checkState(Database server) {
    return run(connection(server.dataSource()), null, true);
  }*/
  /**
   * Run the migrations if there are any that need running.
   */
  /*
  public void run(Database db) {
    try (Transaction txn = db.beginTransaction()){
      run(connection(db.dataSource()), db, false);
    }
  }*/


  /**
   * Run the migrations if there are any that need running. Uses optionl DB as context
   */
  /*
  private List<MigrationResource> run(Connection connection, Database db, boolean checkStateOnly) {
    return new MigrationEngine(migrationConfig, checkStateOnly).run(connection, db);
  }
  */
}
