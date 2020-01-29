package io.ebean.migration.ddl;

/**
 * All DDL using Auto commit true for Cockroach.
 */
class CockroachAutoCommit implements DdlAutoCommit {

  @Override
  public boolean transactional(String sql) {
    return false;
  }

  @Override
  public boolean isAutoCommit() {
    return true;
  }
}
