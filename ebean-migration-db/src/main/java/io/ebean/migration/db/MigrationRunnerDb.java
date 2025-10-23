package io.ebean.migration.db;

import io.ebean.Database;
import io.ebean.Transaction;
import io.ebean.migration.MigrationConfig;
import io.ebean.migration.MigrationResource;
import io.ebean.migration.MigrationRunner;

import java.util.List;

/**
 * Runs the default checkState and run method on a current ebean server.
 *
 * @author Roland Praml, FOCONIS AG
 */
public class MigrationRunnerDb extends MigrationRunner {
  private final Database db;

  public MigrationRunnerDb(MigrationConfig migrationConfig, Database db) {
    super(migrationConfig);
    this.db = db;
  }

  /**
   * Return the migrations that would be applied if the migration is run.
   */
  @Override
  public List<MigrationResource> checkState() {
    try (Transaction txn = db.beginTransaction()) {
      return checkState(new TransactionBasedMigrationContext(migrationConfig, txn, db));
    }
  }

  /**
   * Run the migrations if there are any that need running.
   */
  @Override
  public void run() {
    try (Transaction txn = db.beginTransaction()) {
      run(new TransactionBasedMigrationContext(migrationConfig, txn, db));
      // No commit here!
    }
  }
}
