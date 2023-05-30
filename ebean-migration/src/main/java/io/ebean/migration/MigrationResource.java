package io.ebean.migration;

/**
 * A DB migration resource - typically a resource containing SQL.
 */
public interface MigrationResource extends Comparable<MigrationResource> {

  /**
   * Return the "key" that identifies the migration.
   */
  String key();

  /**
   * Return the migration comment.
   */
  String comment();

  /**
   * Deprecated migrate to comment().
   */
  @Deprecated
  default String getComment() {
    return comment();
  }

  /**
   * Return the content of the migration.
   */
  String content();

  /**
   * Deprecated migrate to content().
   */
  @Deprecated
  default String getContent() {
    return content();
  }

  /**
   * Default ordering by version.
   */
  @Override
  int compareTo(MigrationResource other);

  /**
   * Return the underlying migration version.
   */
  MigrationVersion version();

  /**
   * Deprecated migrate to version().
   */
  @Deprecated
  default MigrationVersion getVersion() {
    return version();
  }

  /**
   * Return the resource location.
   */
  String location();

  /**
   * Deprecated migrate to location().
   */
  @Deprecated
  default String getLocation() {
    return location();
  }

  /**
   * Return the type code ("R" or "V") for this migration.
   */
  String type();

  /**
   * Deprecated migrate to type().
   */
  @Deprecated
  default String getType() {
    return type();
  }
}
