package io.ebean.migration.ddl;

import java.util.regex.Pattern;

/**
 * Postgres requires create/drop index concurrently to run with auto commit true.
 */
class PostgresAutoCommit implements DdlAutoCommit {

  private static final Pattern IX_CONCURRENTLY = Pattern.compile(Pattern.quote(" index concurrently "), Pattern.CASE_INSENSITIVE);

  @Override
  public boolean transactional(String sql) {
    return !IX_CONCURRENTLY.matcher(sql).find();
  }

  @Override
  public boolean isAutoCommit() {
    return false;
  }
}
