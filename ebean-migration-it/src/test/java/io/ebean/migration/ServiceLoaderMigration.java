package io.ebean.migration;

import java.sql.Connection;

/**
 * @author Roland Praml, FOCONIS AG
 */
public class ServiceLoaderMigration implements JdbcMigration {

  @Override
  public String getName() {
    return "1.4.1__serviceLoaded";
  }

  @Override
  public void migrate(MigrationContext context) {

  }

  @Override
  public boolean matches(MigrationContext context) {
    return "dbmig".equals(context.migrationPath());
  }
}
