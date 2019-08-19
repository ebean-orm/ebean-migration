package io.ebean.migration.runner;

import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;


public class MigrationMetaRowTest {

  @Test
  public void testSelectSql() {

    final MigrationPlatform sqlServer = new MigrationPlatform.SqlServer();
    String sql = sqlServer.sqlSelectForReading("someTable");
    assertThat(sql).isEqualTo("select id, mtype, mstatus, mversion, mcomment, mchecksum, run_on, run_by, run_time from someTable with (updlock) order by id");

    final MigrationPlatform noLocking = new MigrationPlatform.NoLocking();
    sql = noLocking.sqlSelectForReading("someTable");
    assertThat(sql).isEqualTo("select id, mtype, mstatus, mversion, mcomment, mchecksum, run_on, run_by, run_time from someTable order by id");

    final MigrationPlatform postgres = new MigrationPlatform.Postgres();
    sql = postgres.sqlSelectForReading("someTable");
    assertThat(sql).isEqualTo("select id, mtype, mstatus, mversion, mcomment, mchecksum, run_on, run_by, run_time from someTable order by id for update");

    final MigrationPlatform defaultPlatform = new MigrationPlatform();
    sql = defaultPlatform.sqlSelectForReading("someTable");
    assertThat(sql).isEqualTo("select id, mtype, mstatus, mversion, mcomment, mchecksum, run_on, run_by, run_time from someTable order by id for update");
  }

}
