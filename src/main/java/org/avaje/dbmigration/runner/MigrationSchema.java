package org.avaje.dbmigration.runner;

import org.avaje.dbmigration.MigrationConfig;
import org.avaje.dbmigration.util.JdbcClose;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;

/**
 * Create Schema if need and Set current Schema in Migration
 */
public class MigrationSchema {

    private static final Logger logger = LoggerFactory.getLogger("org.avaje.dbmigration.runner.MigrationSchema");

    private final MigrationConfig migrationConfig;

    private final Connection connection;

    public MigrationSchema(MigrationConfig migrationConfig, Connection connection) {
        this.migrationConfig = migrationConfig;
        this.connection = connection;
    }

    private Connection getConnection() {
        return this.connection;
    }

    private String getSchema() {
        return this.migrationConfig.getDbSchema();
    }

    private boolean getCreateSchemaIfNeed() {
        return this.migrationConfig.getCreateSchemaIfNotExists();
    }

    public void createAndSetIfNeeded() throws SQLException {
        if (getSchema() != null) {
            logger.info("Migration Schema: " + getSchema());

            createSchemaIfNeed();
            setSchema();
        }
    }

    private void createSchemaIfNeed() throws SQLException {
        if (getCreateSchemaIfNeed()) {
            checkAndSchemaIfNeed();
        }
    }

    private void checkAndSchemaIfNeed() throws SQLException {
        boolean existSchema = existSchame();
        if (!existSchema) {
            logger.info("Creating Schema: " + getSchema());

            PreparedStatement query = getConnection().prepareStatement("CREATE SCHEMA " + getSchema().trim());
            try {
                query.execute();
            } finally {
                JdbcClose.close(query);
            }
        }
    }

    public boolean existSchame() throws SQLException {
        DatabaseMetaData databaseMetaData = getConnection().getMetaData();
        ResultSet schemas = databaseMetaData.getSchemas();

        try {
            while (schemas.next()) {
                String tableSchema = schemas.getString(1);
                if (tableSchema.equalsIgnoreCase(getSchema())) {
                    return true;
                }
            }
        } finally {
            JdbcClose.close(schemas);
        }

        return false;
    }

    private void setSchema() throws SQLException {
        logger.info("Setting Schema: " + getSchema());

        PreparedStatement query = getConnection().prepareStatement("SET SCHEMA " + getSchema().trim());
        try {
            query.execute();
        } finally {
            JdbcClose.close(query);
        }
    }


}
