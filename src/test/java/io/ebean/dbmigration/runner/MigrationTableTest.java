package io.ebean.dbmigration.runner;

import io.ebean.dbmigration.MigrationConfig;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class MigrationTableTest {

  @Test
  public void testCreateTableDdl() throws Exception {

    MigrationConfig config = new MigrationConfig();
    config.setDbSchema("foo");

    MigrationTable mt = new MigrationTable(config, null);
    String tableSql = mt.createTableDdl();

    assertThat(tableSql).contains("create table foo.db_migration ");
    assertThat(tableSql).contains("constraint pk_db_migration primary key (id)");
  }

  @Test
  public void testCreateTableDdl_sqlserver() throws Exception {

    MigrationConfig config = new MigrationConfig();
    config.setDbSchema("bar");
    config.setPlatformName("sqlserver");

    MigrationTable mt = new MigrationTable(config, null);
    String tableSql = mt.createTableDdl();

    assertThat(tableSql).contains("datetime2 ");
    assertThat(tableSql).contains("create table bar.db_migration ");
    assertThat(tableSql).contains("constraint pk_db_migration primary key (id)");
  }
}