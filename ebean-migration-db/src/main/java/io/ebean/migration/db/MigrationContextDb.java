package io.ebean.migration.db;

import io.ebean.Database;
import io.ebean.Transaction;
import io.ebean.migration.MigrationContext;

/**
 * @author Roland Praml, FOCONIS AG
 */
public interface MigrationContextDb extends MigrationContext {
  public Transaction transaction();

  public Database database();
}
