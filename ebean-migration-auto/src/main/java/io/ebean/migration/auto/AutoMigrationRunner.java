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
   * Run DB migrations using the given DataSource.
   */
  void run(DataSource dataSource);
}
