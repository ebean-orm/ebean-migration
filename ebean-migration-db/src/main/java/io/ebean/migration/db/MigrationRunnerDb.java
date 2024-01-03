package io.ebean.migration.db;

import io.ebean.Database;
import io.ebean.Transaction;
import io.ebean.migration.MigrationConfig;
import io.ebean.migration.MigrationResource;
import io.ebean.migration.MigrationRunner;

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
  public List<MigrationResource> checkState(Database db) {
    try (Transaction txn = db.beginTransaction()) {
      return checkState(new TransactionBasedMigrationContext(migrationConfig, txn, db));
    }
  }

  /**
   * Run the migrations if there are any that need running.
   */
  public void run(Database db) {
    try (Transaction txn = db.beginTransaction()) {
      run(new TransactionBasedMigrationContext(migrationConfig, txn, db));
      txn.end();
    }
  }
}
