package io.ebean.migration.runner;

import io.ebean.migration.JdbcMigration;
import io.ebean.migration.MigrationChecksumProvider;
import io.ebean.migration.MigrationVersion;

/**
 * A DB migration resource (JdbcMigration with version).
 *
 * @author Roland Praml, FOCONIS AG
 */
final class LocalJdbcMigrationResource extends LocalMigrationResource {

  private final JdbcMigration migration;

  /**
   * Construct with version and resource.
   */
  LocalJdbcMigrationResource(MigrationVersion version, String location, JdbcMigration migration) {
    super(version, location);
    this.migration = migration;
  }

  /**
   * Return the migration
   */
  JdbcMigration migration() {
    return migration;
  }

  /**
   * Returns the checksum of the migration routine.
   */
  int checksum() {
    if (migration instanceof MigrationChecksumProvider) {
      return ((MigrationChecksumProvider) migration).getChecksum();
    } else {
      return 0; // maybe we can build a checksum over the byte code, but this may change on different java versions.
    }
  }

  @Override
  public String content() {
    return "location:" + location;
  }
}
