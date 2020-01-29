package io.ebean.migration.ddl;

/**
 * Detect non transactional SQL statements that must run after normal
 * sql statements with connection set to auto commit true.
 */
public interface DdlAutoCommit {

  DdlAutoCommit NONE = new NoAutoCommit();

  DdlAutoCommit POSTGRES = new PostgresAutoCommit();

  DdlAutoCommit COCKROACH = new CockroachAutoCommit();

  /**
   * Return the implementation for the given platform.
   */
  static DdlAutoCommit forPlatform(String name) {
    switch (name.toLowerCase()) {
      case "postgres":
        return POSTGRES;
      case "cockroach":
        return COCKROACH;
      default:
        return NONE;
    }
  }

  /**
   * Return false if the SQL is non transactional and should run with auto commit.
   */
  boolean transactional(String sql);

  /**
   * Return true if auto commit true should be used for all DDL for the database platform.
   */
  boolean isAutoCommit();
}
