package io.ebean.migration.runner;

import io.ebean.migration.ddl.DdlAutoCommit;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

public class MigrationPlatformTest {

  private final MigrationPlatform.Postgres platform = new MigrationPlatform.Postgres();

  final DdlAutoCommit pg = platform.ddlAutoCommit();

  @Test
  public void transaction_false() {
    assertFalse(pg.transactional("create index concurrently foo"));
    assertFalse(pg.transactional("drop index concurrently foo"));
    assertFalse(pg.transactional("CREATE INDEX CONCURRENTLY foo"));
    assertFalse(pg.transactional("DROP INDEX CONCURRENTLY foo"));
  }

  @Test
  public void transaction_true() {
    assertTrue(pg.transactional("create index foo"));
    assertTrue(pg.transactional("drop index foo"));
    assertTrue(pg.transactional("CREATE INDEX foo"));
    assertTrue(pg.transactional("DROP INDEX foo"));
  }
}