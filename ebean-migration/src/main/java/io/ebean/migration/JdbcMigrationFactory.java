package io.ebean.migration;

import javax.annotation.Nonnull;

/**
 * Factory to create and initialise a JdbcMigration.
 *
 * @author Roland Praml, FOCONIS AG
 */
public interface JdbcMigrationFactory {

  /**
   * Create a JDBC based migration given the class name.
   */
  @Nonnull
  JdbcMigration createInstance(String className);
}
