package io.ebean.migration;

import io.ebean.migration.auto.AutoMigrationRunner;

import javax.sql.DataSource;
import java.util.Properties;

/**
 * Automatic running of DB Migrations. Used by Ebean on Database start.
 */
public class AutoRunner implements AutoMigrationRunner {

  private final MigrationConfig config = new MigrationConfig();

  @Override
  public void setName(String name) {
    config.setName(name);
  }

  @Override
  public void setDefaultDbSchema(String defaultDbSchema) {
    if (defaultDbSchema != null) {
      config.setSetCurrentSchema(false);
      config.setDbSchema(defaultDbSchema);
    }
  }

  @Override
  public void loadProperties(Properties properties) {
    config.load(properties);
  }

  @Override
  public void run(DataSource dataSource) {
    new MigrationRunner(config).run(dataSource);
  }
}
