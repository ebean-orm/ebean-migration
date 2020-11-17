package io.ebean.migration;

/**
 * Factory to create and initialise a JdbcMigration.
 *
 * @author Roland Praml, FOCONIS AG
 */
public interface JdbcMigrationFactory {

  /**
   * Create a JDBC based migration given the class name.
   */
  JdbcMigration createInstance(String className);
}
