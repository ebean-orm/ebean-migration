package io.ebean.migration.runner;

import io.ebean.migration.MigrationConfig;
import io.ebean.migration.MigrationContext;

import java.sql.Connection;

/**
 * A default implementation of the MigrationContext.
 *
 * @author Roland Praml, FOCONIS AG
 */
public class DefaultMigrationContext implements MigrationContext {
  private final Connection connection;
  private final String migrationPath;
  private final String platform;
  private final String basePlatform;

  public DefaultMigrationContext(MigrationConfig config, Connection connection) {
    this.connection = connection;
    this.migrationPath = config.getMigrationPath();
    this.platform = config.getPlatform();
    this.basePlatform = config.getBasePlatform();
  }

  @Override
  public Connection connection() {
    return connection;
  }

  @Override
  public String migrationPath() {
    return migrationPath;
  }

  @Override
  public String platform() {
    return platform;
  }

  @Override
  public String basePlatform() {
    return basePlatform;
  }
}
