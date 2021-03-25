package io.ebean.migration.runner;

import io.ebean.migration.MigrationVersion;

import javax.annotation.Nonnull;

/**
 * A DB migration resource (DDL or Jdbc)
 */
public abstract class LocalMigrationResource implements Comparable<LocalMigrationResource> {

  protected final MigrationVersion version;

  protected final String location;

  private String type;

  /**
   * Construct with version and resource.
   */
  public LocalMigrationResource(MigrationVersion version, String location) {
    this.version = version;
    this.location = location;
    this.type = version.getType();
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
   * Return true if the underlying version is "repeatable init".
   */
  public boolean isRepeatableInit() {
    return version.isRepeatableInit();
  }

  /**
   * Return true if the underlying version is "repeatable last".
   */
  public boolean isRepeatableLast() {
    return version.isRepeatableLast();
  }

  /**
   * Return the "key" that identifies the migration.
   */
  @Nonnull
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
  @Nonnull
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
  @Nonnull
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
   * Return the content of the migration.
   */
  @Nonnull
  public abstract String getContent();

  /**
   * Return the type code ("R" or "V") for this migration.
   */
  @Nonnull
  public String getType() {
    return type;
  }

  /**
   * Set the migration to be an Init migration.
   */
  public void setInitType() {
    this.type = MigrationVersion.BOOTINIT_TYPE;
  }
}
