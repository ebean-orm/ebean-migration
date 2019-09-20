package dbmig;

import java.sql.Connection;

import io.ebean.migration.ConfigurationAware;
import io.ebean.migration.JdbcMigration;
import io.ebean.migration.MigrationConfig;

/**
 * Sample migration.
 *
 * @author Roland Praml, FOCONIS AG
 *
 */
public class V1_2_1__test implements JdbcMigration, ConfigurationAware{

  private MigrationConfig config;

  public static class MyDto {
    String id;
  }
  
  @Override
  public void setMigrationConfig(MigrationConfig config) {
    this.config = config;
  }

  @Override
  public void migrate(Connection connection) {
    System.out.println("Executing migration on " + connection);
  }

  @Override
  public String toString() {
    return "Dummy jdbc migration";
  }
}
