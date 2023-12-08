package io.ebean.migration.runner;

import io.ebean.migration.MigrationConfig;
import io.ebean.migration.MigrationContext;

import java.sql.Connection;

/**
 *
 * @author Roland Praml, FOCONIS AG
 */
public class DefaultMigrationContext implements MigrationContext {
  private final Connection connection;
  private final String migrationPath;
  private final String platform;

  public DefaultMigrationContext(MigrationConfig config, Connection connection) {
    this.connection = connection;
    this.migrationPath = config.getMigrationPath();
    this.platform = config.getPlatform();
  }

  public Connection connection() {
    return connection;
  }

  @Override
  public String migrationPath() {
    return migrationPath;
  }

  public String platform() {
    return platform;
  }
}
