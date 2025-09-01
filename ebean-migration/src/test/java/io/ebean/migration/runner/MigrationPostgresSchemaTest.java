package io.ebean.migration.runner;

import io.ebean.migration.MigrationConfig;
import io.ebean.migration.MigrationRunner;
import io.ebean.test.containers.PostgresContainer;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

class MigrationPostgresSchemaTest {

  private static PostgresContainer createPostgres() {
    return PostgresContainer.builder("17")
      .port(0) // random port
      .containerName("pg17_temp")
      .user("mig_exp_schema")
      .password("mig_exp_schema")
      .dbName("mig_exp_schema")
      .build();
  }

  @Test
  void runTwice_expect_setSchemaCalled() throws SQLException {
    PostgresContainer postgresContainer = createPostgres();
    postgresContainer.stopRemove();
    postgresContainer.start();


    MigrationConfig config = new MigrationConfig();
    config.setDbUrl(postgresContainer.jdbcUrl());
    config.setDbUsername("mig_exp_schema");
    config.setDbPassword("mig_exp_schema");
    config.setDbSchema("bar");

    // first run, creates and sets the schema correctly (no issue here)
    config.setMigrationPath("dbmig");
    MigrationRunner runner = new MigrationRunner(config);
    runner.run();

    // run again, SHOULD set the schema (this is where the bug is)
    config.setMigrationPath("dbmig2");
    MigrationRunner runner2 = new MigrationRunner(config);
    runner2.run();

    // make sure the m4 table was created in the bar schema
    try (Connection connection = config.createConnection()) {
      try (PreparedStatement stmt = connection.prepareStatement("select * from bar.m4")) {
        try (ResultSet resultSet = stmt.executeQuery()) {
          resultSet.next();
        }
      }
    }
  }

}
