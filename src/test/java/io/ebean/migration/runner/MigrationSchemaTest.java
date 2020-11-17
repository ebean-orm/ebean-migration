package io.ebean.migration.runner;

import io.ebean.migration.MigrationConfig;
import org.junit.jupiter.api.Test;

import java.sql.Connection;

public class MigrationSchemaTest {

  @Test
  void testCreateAndSetIfNeeded() throws Exception {

    MigrationConfig config = createMigrationConfig();
    config.setDbSchema("SOME_NEW_SCHEMA");
    config.setCreateSchemaIfNotExists(true);

    Connection connection = config.createConnection();

    MigrationSchema migrationSchema = new MigrationSchema(config, connection);
    migrationSchema.createAndSetIfNeeded();
  }

  private MigrationConfig createMigrationConfig() {

    MigrationConfig config = new MigrationConfig();
    config.setDbUsername("sa");
    config.setDbPassword("");
    config.setDbUrl("jdbc:h2:mem:db1");
    config.setDbDriver("org.h2.Driver");

//    config.setDbUsername("unit");
//    config.setDbPassword("unit");
//    config.setDbDriver("org.postgresql.Driver");
//    config.setDbUrl("jdbc:postgresql://127.0.0.1:5432/unit");
    return config;
  }
}