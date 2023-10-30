package org.example;

import io.ebean.migration.MigrationConfig;
import io.ebean.migration.MigrationRunner;

import java.net.URL;

public class Main {

  public static void main(String[] args) {
    URL url = Main.class.getResource("/dbmigration/postgres/idx_postgres.migrations");
    System.out.println("Found idx_postgres.migrations: " + url);

    MigrationConfig config = new MigrationConfig();
    config.setDbUsername("mig");
    config.setDbPassword("test");
    config.setDbUrl("jdbc:postgresql://localhost:6432/mig");
    config.setBasePlatform("postgres");
    config.setPlatform("postgres");
    config.setMigrationPath("dbmigration");

    MigrationRunner runner = new MigrationRunner(config);
    runner.run();

    //System.out.println("state: " + runner.checkState());
    System.out.println("DONE");
  }

}
