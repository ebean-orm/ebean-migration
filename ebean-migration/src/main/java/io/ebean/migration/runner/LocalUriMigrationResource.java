package io.ebean.migration.runner;

import io.ebean.migration.MigrationVersion;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.net.URL;

/**
 * A local URL based DB migration resource.
 */
final class LocalUriMigrationResource extends LocalMigrationResource {

  private final URL resource;
  private final int checksum;

  LocalUriMigrationResource(MigrationVersion version, String location, URL resource, int checksum) {
    super(version, location);
    this.resource = resource;
    this.checksum = checksum;
  }

  public int checksum() {
    return checksum;
  }

  @Override
  public String content() {
    try (var reader = new InputStreamReader(resource.openStream())) {
      var writer = new StringWriter(1024);
      reader.transferTo(writer);
      return writer.toString();
    } catch (IOException e) {
      throw new IllegalStateException(missingOpensMessage(), e);
    }
  }

  private String missingOpensMessage() {
    return "NPE reading DB migration content at [" + location + "] Probably missing an 'opens dbmigration;' in module-info.java";
  }
}
