package io.ebean.migration.runner;

import io.ebean.migration.MigrationConfig;
import org.junit.jupiter.api.Test;

import java.sql.Connection;

class MigrationSchemaTest {

  @Test
  void testCreateAndSetIfNeeded() throws Exception {

    MigrationConfig config = createMigrationConfig();
    config.setDbSchema("SOME_NEW_SCHEMA");
    config.setCreateSchemaIfNotExists(true);

    try (Connection connection = config.createConnection()) {
      MigrationSchema.createIfNeeded(config, connection);
    }
  }

  private MigrationConfig createMigrationConfig() {
    MigrationConfig config = new MigrationConfig();
    config.setDbUsername("sa");
    config.setDbPassword("");
    config.setDbUrl("jdbc:h2:mem:db1");
    return config;
  }
}
