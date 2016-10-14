package org.avaje.dbmigration.runner;

import org.avaje.dbmigration.MigrationConfig;
import org.avaje.dbmigration.util.JdbcClose;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;

/**
 * Create Schema if needed and set current Schema in Migration
 */
public class MigrationSchema {

  private static final Logger logger = LoggerFactory.getLogger(MigrationSchema.class);

  private final Connection connection;

  private final String dbSchema;

  private final boolean createSchemaIfNotExists;

  /**
   * Construct with configuration and connection.
   */
  public MigrationSchema(MigrationConfig migrationConfig, Connection connection) {
    this.dbSchema = trim(migrationConfig.getDbSchema());
    this.createSchemaIfNotExists = migrationConfig.isCreateSchemaIfNotExists();
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
      setSchema();
    }
  }

  private void createSchemaIfNeeded() throws SQLException {
    if (!schemaExists()) {
      logger.info("Creating Schema: {}", dbSchema);
      PreparedStatement query = connection.prepareStatement("CREATE SCHEMA " + dbSchema);
      try {
        query.execute();
      } finally {
        JdbcClose.close(query);
      }
    }
  }

  private boolean schemaExists() throws SQLException {

    ResultSet schemas = connection.getMetaData().getSchemas();
    try {
      while (schemas.next()) {
        String schema = schemas.getString(1);
        if (schema.equalsIgnoreCase(dbSchema)) {
          return true;
        }
      }
    } finally {
      JdbcClose.close(schemas);
    }

    return false;
  }

  private void setSchema() throws SQLException {

    logger.info("Setting Schema: {}", dbSchema);
    PreparedStatement query = connection.prepareStatement("SET SCHEMA " + dbSchema);
    try {
      query.execute();
    } finally {
      JdbcClose.close(query);
    }
  }

}
