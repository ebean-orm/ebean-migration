package io.ebean.migration;

/**
 * Factory to create and initalize a JdbcMigration.
 *
 * @author Roland Praml, FOCONIS AG
 *
 */
public interface JdbcMigrationFactory {
  JdbcMigration createInstance(String className);
}
