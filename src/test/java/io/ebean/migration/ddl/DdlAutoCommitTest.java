package io.ebean.migration.ddl;

import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class DdlAutoCommitTest {

  @Test
  public void testForPlatform_postgres() {
    assertThat(DdlAutoCommit.forPlatform("postgres")).isInstanceOf(PostgresAutoCommit.class);
    assertThat(DdlAutoCommit.forPlatform("POSTGRES")).isInstanceOf(PostgresAutoCommit.class);
    assertThat(DdlAutoCommit.forPlatform("Postgres")).isInstanceOf(PostgresAutoCommit.class);
  }

  @Test
  public void testForPlatform_cockroach() {
    assertThat(DdlAutoCommit.forPlatform("cockroach")).isInstanceOf(CockroachAutoCommit.class);
    assertThat(DdlAutoCommit.forPlatform("COCKROACH")).isInstanceOf(CockroachAutoCommit.class);
  }

  @Test
  public void testForPlatform_others() {
    assertThat(DdlAutoCommit.forPlatform("other")).isInstanceOf(NoAutoCommit.class);
    assertThat(DdlAutoCommit.forPlatform("mysql")).isInstanceOf(NoAutoCommit.class);
    assertThat(DdlAutoCommit.forPlatform("oracle")).isInstanceOf(NoAutoCommit.class);
  }
}