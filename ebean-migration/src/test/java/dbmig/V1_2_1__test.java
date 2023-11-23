package dbmig;

import java.sql.Connection;

import io.ebean.migration.JdbcMigration;
import io.ebean.migration.MigrationConfig;

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
  public void migrate(Connection connection, MigrationConfig config) {
    System.out.println("Executing migration on " + connection);
  }

  @Override
  public String toString() {
    return "Dummy jdbc migration";
  }
}
