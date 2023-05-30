package io.ebean.migration.runner;

import io.ebean.migration.MigrationResource;
import io.ebean.migration.MigrationVersion;

/**
 * A DB migration resource (DDL or Jdbc)
 */
public abstract class LocalMigrationResource implements MigrationResource {

  protected final MigrationVersion version;

  protected final String location;

  private String type;

  /**
   * Construct with version and resource.
   */
  public LocalMigrationResource(MigrationVersion version, String location) {
    this.version = version;
    this.location = location;
    this.type = version.type();
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


  @Override
  public String key() {
    if (isRepeatable()) {
      return version.comment().toLowerCase();
    } else {
      return version.normalised();
    }
  }

  @Override
  public String comment() {
    String comment = version.comment();
    return (comment == null || comment.isEmpty()) ? "-" : comment;
  }

  @Override
  public int compareTo(MigrationResource other) {
    return version.compareTo(other.version());
  }

  @Override
  public MigrationVersion version() {
    return version;
  }

  @Override
  public String location() {
    return location;
  }

  @Override
  public String type() {
    return type;
  }

  /**
   * Set the migration to be an Init migration.
   */
  public void setInitType() {
    this.type = MigrationVersion.BOOTINIT_TYPE;
  }
}
