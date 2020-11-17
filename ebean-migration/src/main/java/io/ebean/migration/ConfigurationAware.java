package io.ebean.migration;

/**
 * Marks a class as configuration aware (JdbcMigrations) Configuration aware
 * classes get the migration configuration injected upon creation. The
 * implementer is responsible for correctly storing the provided
 * MigrationConfig (usually in a field).
 *
 * @author Roland Praml, FOCONIS AG
 *
 */
public interface ConfigurationAware {

  /**
   * Set the configuration being used.
   */
  void setMigrationConfig(MigrationConfig config);
}
