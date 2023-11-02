package io.ebean.migration.runner;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;


class MigrationMetaRowTest {

  @Test
  void testSelectSql() {

    final MigrationPlatform sqlServer = new MigrationPlatform.SqlServer();
    String sql = sqlServer.sqlSelectForReading("someTable");
    assertThat(sql).isEqualTo("select id, mtype, mversion, mchecksum from someTable with (updlock) order by id");

    final MigrationPlatform noLocking = new MigrationPlatform.NoLocking();
    sql = noLocking.sqlSelectForReading("someTable");
    assertThat(sql).isEqualTo("select id, mtype, mversion, mchecksum from someTable order by id");

    final MigrationPlatform postgres = new MigrationPlatform.Postgres();
    sql = postgres.sqlSelectForReading("someTable");
    assertThat(sql).isEqualTo("select id, mtype, mversion, mchecksum from someTable order by id for update");

    final MigrationPlatform defaultPlatform = new MigrationPlatform();
    sql = defaultPlatform.sqlSelectForReading("someTable");
    assertThat(sql).isEqualTo("select id, mtype, mversion, mchecksum from someTable order by id for update");
  }

}
