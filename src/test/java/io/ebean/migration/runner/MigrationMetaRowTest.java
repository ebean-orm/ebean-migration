package io.ebean.migration.runner;

import io.ebean.migration.DbPlatformNames;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;


public class MigrationMetaRowTest {

  @Test
  public void testSelectSql() throws Exception {

    String sql = MigrationMetaRow.selectSql("someTable", DbPlatformNames.SQLSERVER);
    assertThat(sql).isEqualTo("select id, mtype, mstatus, mversion, mcomment, mchecksum, run_on, run_by, run_time from someTable with (updlock) order by id");

    sql = MigrationMetaRow.selectSql("someTable", DbPlatformNames.SQLITE);
    assertThat(sql).isEqualTo("select id, mtype, mstatus, mversion, mcomment, mchecksum, run_on, run_by, run_time from someTable order by id");

    sql = MigrationMetaRow.selectSql("someTable", DbPlatformNames.POSTGRES);
    assertThat(sql).isEqualTo("select id, mtype, mstatus, mversion, mcomment, mchecksum, run_on, run_by, run_time from someTable order by id for update");

    sql = MigrationMetaRow.selectSql("someTable", DbPlatformNames.COCKROACH);
    assertThat(sql).isEqualTo("select id, mtype, mstatus, mversion, mcomment, mchecksum, run_on, run_by, run_time from someTable order by id");
  }

}