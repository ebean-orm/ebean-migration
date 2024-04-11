package io.ebean.migration.auto;

import javax.sql.DataSource;
import java.util.Properties;

/**
 * Automatically run DB Migrations on application start.
 */
public interface AutoMigrationRunner {

  /**
   * Set the name of the database the migration is run for.
   * <p>
   * This name can be used when loading properties like:
   * <code>ebean.${name}.migration.migrationPath</code>
   */
  void setName(String name);

  /**
   * Set a default DB schema to use.
   * <p>
   * This is mostly for Postgres use where the dbSchema matches the DB username. In this case
   * we don't set the current schema as that can mess up the Postgres search path.
   * </p>
   */
  void setDefaultDbSchema(String defaultDbSchema);

  /**
   * Load configuration properties.
   */
  void loadProperties(Properties properties);

  /**
   * Set the platform for running the migration.
   * <p>
   * In the case where we have migrations for many platforms this defines the associated platform
   * that is being used to run the migration.
   */
  default void setPlatform(String platform) {
    // do nothing by default
  }

  /**
   * Set the base platform for running the migration.
   * <p>
   * For example, with sqlserver17 the base platform is "sqlserver" and platform is "sqlserver17".
   * Similarly, with db2luw the base platform is "db2" and the platform is "db2luw".
   * <p>
   * The migration runner can look for migrations to run based on the base platform or specific platform.
   */
  default void setBasePlatform(String basePlatform) {
    // do nothing by default
  }

  /**
   * Run DB migrations using the given DataSource.
   */
  void run(DataSource dataSource);
}
