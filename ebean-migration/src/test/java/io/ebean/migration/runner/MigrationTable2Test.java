package io.ebean.migration.runner;

import io.ebean.migration.MigrationConfig;
import io.ebean.migration.MigrationVersion;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MigrationTable2Test {


  private static MigrationTable migrationTable(MigrationConfig config) {
    var fc = new FirstCheck(config, new DefaultMigrationContext(config, null), new MigrationPlatform());
    return new MigrationTable(fc, false);
  }

  @Test
  void testCreateTableDdl() throws Exception {

    MigrationConfig config = new MigrationConfig();
    config.setDbSchema("foo");

    MigrationTable mt = migrationTable(config);
    String tableSql = mt.createTableDdl();

    assertThat(tableSql).contains("create table foo.db_migration ");
    assertThat(tableSql).contains("constraint pk_db_migration primary key (id)");
  }

  @Test
  void testCreateTableDdl_sqlserver() throws Exception {

    MigrationConfig config = new MigrationConfig();
    config.setDbSchema("bar");
    config.setPlatform(DbPlatformNames.SQLSERVER);

    MigrationTable mt = migrationTable(config);
    String tableSql = mt.createTableDdl();

    assertThat(tableSql).contains("datetime2 ");
    assertThat(tableSql).contains("create table bar.db_migration ");
    assertThat(tableSql).contains("constraint pk_db_migration primary key (id)");
  }

  @Test
  void test_skipMigration_Repeatable() throws Exception {

    MigrationConfig config = new MigrationConfig();

    LocalMigrationResource local = local("R__hello");
    MigrationMetaRow existing = new MigrationMetaRow(12, "R", "", "comment", 42, null, null, 0);

    MigrationTable mt = migrationTable(config);
    // checksum different - no skip
    assertFalse(mt.skipMigration(100, 100, local, existing));
    // checksum same - skip
    assertTrue(mt.skipMigration(42, 42, local, existing));
  }

  @Test
  void test_skipMigration_skipChecksum() throws Exception {

    MigrationConfig config = new MigrationConfig();
    config.setSkipChecksum(true);

    MigrationTable mt = migrationTable(config);

    // skip regardless of checksum difference
    LocalMigrationResource local = local("R__hello");
    MigrationMetaRow existing = new MigrationMetaRow(12, "R", "", "comment", 42, null, null, 0);

    // repeatable checksum mismatch
    assertFalse(mt.skipMigration(44, 44, local, existing));

    // repeatable match checksum
    assertTrue(mt.skipMigration(42, 42, local, existing));
    // assertTrue(mt.skipMigration(99, 42, local, existing));

    LocalMigrationResource localVer = local("V1__hello");
    MigrationMetaRow localExisting = new MigrationMetaRow(12, "V", "1", "comment", 42, null, null, 0);

    // re-run on checksum mismatch and skipChecksum
    assertFalse(mt.skipMigration(44, 44, localVer, localExisting));

    // match checksum so skip
    assertTrue(mt.skipMigration(42, 42, localVer, localExisting));
  }

  private LocalMigrationResource local(String raw) {
    return new LocalDdlMigrationResource(MigrationVersion.parse(raw), "loc", null);
  }
}
