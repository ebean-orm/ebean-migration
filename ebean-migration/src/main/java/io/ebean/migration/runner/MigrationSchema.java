package io.ebean.migration.runner;

import io.ebean.migration.MigrationConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Create Schema if needed and set current Schema in Migration
 */
public class MigrationSchema {

  private static final Logger logger = LoggerFactory.getLogger(MigrationSchema.class);

  private final Connection connection;

  private final String dbSchema;

  private final boolean createSchemaIfNotExists;

  private final boolean setCurrentSchema;

  /**
   * Construct with configuration and connection.
   */
  public MigrationSchema(MigrationConfig migrationConfig, Connection connection) {
    this.dbSchema = trim(migrationConfig.getDbSchema());
    this.createSchemaIfNotExists = migrationConfig.isCreateSchemaIfNotExists();
    this.setCurrentSchema = migrationConfig.isSetCurrentSchema();
    this.connection = connection;
  }

  private String trim(String dbSchema) {
    return (dbSchema == null) ? null : dbSchema.trim();
  }

  /**
   * Create and set the DB schema if desired.
   */
  public void createAndSetIfNeeded() throws SQLException {
    if (dbSchema != null) {
      logger.info("Migration Schema: {}", dbSchema);
      if (createSchemaIfNotExists) {
        createSchemaIfNeeded();
      }
      if (setCurrentSchema) {
        setSchema();
      }
    }
  }

  private void createSchemaIfNeeded() throws SQLException {
    if (!schemaExists()) {
      logger.info("Creating Schema: {}", dbSchema);
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

    logger.info("Setting Schema: {}", dbSchema);
    connection.setSchema(dbSchema);
  }

}
