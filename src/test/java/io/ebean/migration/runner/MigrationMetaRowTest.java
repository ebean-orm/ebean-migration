package io.ebean.migration.runner;

import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;


public class MigrationMetaRowTest {

  @Test
  public void testSelectSql() throws Exception {

    String sql = MigrationMetaRow.selectSql("someTable", "sqlserver");
    assertThat(sql).isEqualTo("select id, mtype, mstatus, mversion, mcomment, mchecksum, run_on, run_by, run_time from someTable with (updlock) order by id");

    sql = MigrationMetaRow.selectSql("someTable", "sqlite");
    assertThat(sql).isEqualTo("select id, mtype, mstatus, mversion, mcomment, mchecksum, run_on, run_by, run_time from someTable order by id");

    sql = MigrationMetaRow.selectSql("someTable", "postgres");
    assertThat(sql).isEqualTo("select id, mtype, mstatus, mversion, mcomment, mchecksum, run_on, run_by, run_time from someTable order by id for update");
  }

}