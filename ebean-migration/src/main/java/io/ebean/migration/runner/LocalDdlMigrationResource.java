package io.ebean.migration.runner;

import io.avaje.classpath.scanner.Resource;
import io.ebean.migration.MigrationVersion;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * A DB migration resource (DDL script with version).
 */
final class LocalDdlMigrationResource extends LocalMigrationResource {

  private final Resource resource;

  /**
   * Construct with version and resource.
   */
  LocalDdlMigrationResource(MigrationVersion version, String location, Resource resource) {
    super(version, location);
    this.resource = resource;
  }

  /**
   * Return the content for the migration apply ddl script.
   */
  @Override
  public String content() {
    try {
      return resource.loadAsString(StandardCharsets.UTF_8);
    } catch (NullPointerException e) {
      throw new IllegalStateException(missingOpensMessage(), e);
    }
  }

  private String missingOpensMessage() {
    return "NPE reading DB migration content at [" + location + "] Probably missing an 'opens dbmigration;' in module-info.java";
  }

  @Override
  int checksum() {
    return Checksum.calculate(content());
  }
}
