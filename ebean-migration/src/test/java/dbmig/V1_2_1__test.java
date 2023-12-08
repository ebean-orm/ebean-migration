package dbmig;

import java.sql.Connection;

import io.ebean.migration.JdbcMigration;
import io.ebean.migration.MigrationConfig;
import io.ebean.migration.MigrationContext;

/**
 * Sample migration.
 *
 * @author Roland Praml, FOCONIS AG
 */
public class V1_2_1__test implements JdbcMigration {

  public static class MyDto {
    String id;
  }

  @Override
  public void migrate(MigrationContext context) {
    System.out.println("Executing migration on " + context);
  }

  @Override
  public String toString() {
    return "Dummy jdbc migration";
  }
}
