package io.ebean.migration.db;

import io.ebean.Database;
import io.ebean.Transaction;
import io.ebean.migration.MigrationConfig;
import io.ebean.migration.MigrationContext;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * A default implementation of the MigrationContext.
 *
 * @author Roland Praml, FOCONIS AG
 */
class TransactionBasedMigrationContext implements MigrationContextDb {
  private final Transaction transaction;
  private final String migrationPath;
  private final String platform;
  private final String basePlatform;
  private final Database database;

  TransactionBasedMigrationContext(MigrationConfig config, Transaction transaction, Database database) {
    this.transaction = transaction;
    this.migrationPath = config.getMigrationPath();
    this.platform = config.getPlatform();
    this.basePlatform = config.getBasePlatform();
    this.database = database;
  }

  @Override
  public Connection connection() {
    return transaction.connection();
  }

  @Override
  public String migrationPath() {
    return migrationPath;
  }

  @Override
  public String platform() {
    return platform;
  }

  @Override
  public String basePlatform() {
    return basePlatform;
  }

  @Override
  public void commit() throws SQLException {
    // we must not use txn.commit here, as this closes the underlying connection, which is needed for logicalLock etc.
    transaction.commitAndContinue();
  }

  @Override
  public Transaction transaction() {
    return transaction;
  }

  @Override
  public Database database() {
    return database;
  }
}
