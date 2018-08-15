package io.ebean.migration.runner;

import io.ebean.migration.MigrationVersion;

/**
 * A DB migration resource (DDL or Jdbc)
 */
public abstract class LocalMigrationResource implements Comparable<LocalMigrationResource> {

  private final MigrationVersion version;

  private final String location;


  /**
   * Construct with version and resource.
   */
  public LocalMigrationResource(MigrationVersion version, String location) {
    this.version = version;
    this.location = location;
  }

  public String toString() {
    return version.toString();
  }

  /**
   * Return true if the underlying version is "repeatable".
   */
  public boolean isRepeatable() {
    return version.isRepeatable();
  }

  /**
   * Return the "key" that identifies the migration.
   */
  public String key() {
    if (isRepeatable()) {
      return version.getComment().toLowerCase();
    } else {
      return version.normalised();
    }
  }

  /**
   * Return the migration comment.
   */
  public String getComment() {
    String comment = version.getComment();
    return (comment == null || comment.isEmpty()) ? "-" : comment;
  }

  /**
   * Default ordering by version.
   */
  @Override
  public int compareTo(LocalMigrationResource o) {
    return version.compareTo(o.version);
  }

  /**
   * Return the underlying migration version.
   */
  public MigrationVersion getVersion() {
    return version;
  }

  /**
   * Return the resource location.
   */
  public String getLocation() {
    return location;
  }

  /**
   * Return the type code ("R" or "V") for this migration.
   */
  public String getType() {
    return version.getType();
  }
}
