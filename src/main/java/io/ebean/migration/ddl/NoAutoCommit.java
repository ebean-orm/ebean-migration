package io.ebean.migration.ddl;

/**
 * By default no statements require auto commit.
 */
class NoAutoCommit implements DdlAutoCommit {

  @Override
  public boolean transactional(String sql) {
    return true;
  }
}
