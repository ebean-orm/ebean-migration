package io.ebean.migration.runner;

import io.avaje.applog.AppLog;
import io.ebean.migration.MigrationConfig;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static java.lang.System.Logger.Level.DEBUG;
import static java.lang.System.Logger.Level.INFO;

/**
 * Create Schema if needed and set current Schema in Migration
 */
final class MigrationSchema {

  static final System.Logger log = AppLog.getLogger("io.ebean.migration");

  private final Connection connection;
  private final String dbSchema;
  private final boolean createSchemaIfNotExists;
  private final boolean setCurrentSchema;

  /**
   * Construct with configuration and connection.
   */
  private MigrationSchema(MigrationConfig migrationConfig, Connection connection) {
    this.dbSchema = trim(migrationConfig.getDbSchema());
    this.createSchemaIfNotExists = migrationConfig.isCreateSchemaIfNotExists();
    this.setCurrentSchema = migrationConfig.isSetCurrentSchema();
    this.connection = connection;
  }

  static void createIfNeeded(MigrationConfig config, Connection connection) throws SQLException {
    new MigrationSchema(config, connection).createAndSetIfNeeded();
  }

  private String trim(String dbSchema) {
    return (dbSchema == null) ? null : dbSchema.trim();
  }

  /**
   * Create and set the DB schema if desired.
   */
  void createAndSetIfNeeded() throws SQLException {
    if (dbSchema != null) {
      log.log(DEBUG, "Migration using schema: {0}", dbSchema);
      if (createSchemaIfNotExists) {
        createSchemaIfNeeded();
      }
      if (setCurrentSchema) {
        setSchema();
      }
    }
  }

  @SuppressWarnings("SqlDialectInspection")
  private void createSchemaIfNeeded() throws SQLException {
    if (!schemaExists()) {
      log.log(INFO, "Creating schema: {0}", dbSchema);
      try (Statement query = connection.createStatement()) {
        query.executeUpdate("CREATE SCHEMA " + dbSchema);
      }
    }
  }

  private boolean schemaExists() throws SQLException {
    try (ResultSet schemas = connection.getMetaData().getSchemas()) {
      while (schemas.next()) {
        String schema = schemas.getString(1);
        if (schema.equalsIgnoreCase(dbSchema)) {
          return true;
        }
      }
    }
    return false;
  }

  private void setSchema() throws SQLException {
    log.log(DEBUG, "Setting schema: {0}", dbSchema);
    connection.setSchema(dbSchema);
  }

}
