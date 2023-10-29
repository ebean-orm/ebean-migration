package org.example;

import io.ebean.migration.*;

import java.io.InputStream;
import java.util.List;
import java.util.Properties;

public class Main {

    public static void main(String[] args) {
      // InputStream is = Main.class.getResourceAsStream("/dbmigration/postgres/idx_postgres.migrations");
      // System.out.println("GOT is: " + is);

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
