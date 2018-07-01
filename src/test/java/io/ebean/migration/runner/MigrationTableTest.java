package io.ebean.migration.runner;

import io.ebean.migration.DbPlatformNames;
import io.ebean.migration.MigrationConfig;
import io.ebean.migration.MigrationVersion;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class MigrationTableTest {

  @Test
  public void testCreateTableDdl() throws Exception {

    MigrationConfig config = new MigrationConfig();
    config.setDbSchema("foo");

    MigrationTable mt = new MigrationTable(config, null, false);
    String tableSql = mt.createTableDdl();

    assertThat(tableSql).contains("create table foo.db_migration ");
    assertThat(tableSql).contains("constraint pk_db_migration primary key (id)");
  }

  @Test
  public void testCreateTableDdl_sqlserver() throws Exception {

    MigrationConfig config = new MigrationConfig();
    config.setDbSchema("bar");
    config.setPlatformName(DbPlatformNames.SQLSERVER);

    MigrationTable mt = new MigrationTable(config, null, false);
    String tableSql = mt.createTableDdl();

    assertThat(tableSql).contains("datetime2 ");
    assertThat(tableSql).contains("create table bar.db_migration ");
    assertThat(tableSql).contains("constraint pk_db_migration primary key (id)");
  }

  @Test
  public void test_skipMigration_Repeatable() throws Exception {

    MigrationConfig config = new MigrationConfig();

    LocalMigrationResource local = local("R__hello");
    MigrationMetaRow existing = new MigrationMetaRow(12, "R", "", "comment", 42, null, null, 0);

    MigrationTable mt = new MigrationTable(config, null, false);
    // checksum different - no skip
    assertFalse(mt.skipMigration(100, local, existing));
    // checksum same - skip
    assertTrue(mt.skipMigration(42, local, existing));
  }

  @Test
  public void test_skipMigration_skipChecksum() throws Exception {

    MigrationConfig config = new MigrationConfig();
    config.setSkipChecksum(true);

    MigrationTable mt = new MigrationTable(config, null, false);

    // skip regardless of checksum difference
    LocalMigrationResource local = local("R__hello");
    MigrationMetaRow existing = new MigrationMetaRow(12, "R", "", "comment", 42, null, null, 0);

    // repeatable checksum mismatch
    assertFalse(mt.skipMigration(44, local, existing));

    // repeatable match checksum
    assertTrue(mt.skipMigration(42, local, existing));

    LocalMigrationResource localVer = local("V1__hello");
    MigrationMetaRow localExisting = new MigrationMetaRow(12, "V", "1", "comment", 42, null, null, 0);

    // re-run on checksum mismatch and skipChecksum
    assertFalse(mt.skipMigration(44, localVer, localExisting));

    // match checksum so skip
    assertTrue(mt.skipMigration(42, localVer, localExisting));
  }

  private LocalMigrationResource local(String raw) {
    return new LocalDdlMigrationResource(MigrationVersion.parse(raw), "loc", null);
  }
}