package io.ebean.migration.ddl;

/**
 * Detect non transactional SQL statements that must run after normal
 * sql statements with connection set to auto commit true.
 */
public interface DdlAutoCommit {

  DdlAutoCommit NONE = new NoAutoCommit();

  DdlAutoCommit POSTGRES = new PostgresAutoCommit();

  /**
   * Return the implementation for the given platform.
   */
  static DdlAutoCommit forPlatform(String name) {
    return name.equalsIgnoreCase("postgres") ? POSTGRES : NONE;
  }

  /**
   * Return false if the SQL is non transactional and should run with auto commit.
   */
  boolean transactional(String sql);

}
