package io.ebean.migration.ddl;

/**
 * By default no statements require auto commit.
 */
public class NoAutoCommit implements DdlAutoCommit {

  @Override
  public boolean transactional(String sql) {
    return true;
  }
}
