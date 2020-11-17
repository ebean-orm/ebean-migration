package io.ebean.migration.auto;

import javax.sql.DataSource;
import java.util.Properties;

/**
 * Automatically run DB Migrations on application start.
 */
public interface AutoMigrationRunner {

  /**
   * Set a default DB schema to use.
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
