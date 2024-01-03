package io.ebean.migration.runner;

import io.avaje.classpath.scanner.Resource;
import io.avaje.classpath.scanner.core.Scanner;
import io.ebean.migration.JdbcMigration;
import io.ebean.migration.MigrationConfig;
import io.ebean.migration.MigrationContext;
import io.ebean.migration.MigrationVersion;

import java.io.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import static java.lang.System.Logger.Level.DEBUG;

/**
 * Loads the DB migration resources and sorts them into execution order.
 */
final class LocalMigrationResources {

  private static final System.Logger log = MigrationSchema.log;

  private final List<LocalMigrationResource> versions = new ArrayList<>();
  private final MigrationConfig migrationConfig;
  private final ClassLoader classLoader;
  private final Iterable<JdbcMigration> jdbcMigrations;
  private final MigrationContext context;

  /**
   * Construct with configuration options.
   */
  LocalMigrationResources(MigrationConfig migrationConfig, MigrationContext context) {
    this.migrationConfig = migrationConfig;
    this.classLoader = migrationConfig.getClassLoader();
    this.jdbcMigrations = migrationConfig.getJdbcMigrations();
    this.context = context;
  }

  /**
   * Read the init migration resources (usually only 1) returning true if there are versions.
   */
  boolean readInitResources() {
    readResourcesForPath(migrationConfig.getMigrationInitPath());
    Collections.sort(versions);
    return !versions.isEmpty();
  }

  /**
   * Read all the migration resources (SQL scripts and JDBC migrations) returning true if there are versions.
   */
  boolean readResources() {
    if (readFromIndex()) {
      // automatically enable earlyChecksumMode when using index file with pre-computed checksums
      migrationConfig.setEarlyChecksumMode(true);
    } else {
      readResourcesForPath(migrationConfig.getMigrationPath());
    }
    // after we read the SQL migrations from index or classpath scan, we
    // read jdbcMigrations and sort them.
    readJdbcMigrations();
    Collections.sort(versions);
    return !versions.isEmpty();
  }

  /**
   * Returns true, if an index file was found. Although, if file was empty, so we do not fall back
   * to classpath scan!
   */
  private boolean readFromIndex() {
    final var base = "/" + migrationConfig.getMigrationPath() + "/";
    final var basePlatform = migrationConfig.getBasePlatform();
    final var indexName = "idx_" + basePlatform + ".migrations";
    URL idx = resource(base + indexName);
    if (idx != null) {
      loadFromIndexFile(idx, base);
      return true;
    }
    idx = resource(base + basePlatform + '/' + indexName);
    if (idx != null) {
      loadFromIndexFile(idx, base + basePlatform + '/');
      return true;
    }
    final var platform = migrationConfig.getPlatform();
    idx = resource(base + platform + indexName);
    if (idx != null) {
      loadFromIndexFile(idx, base + platform + '/');
      return true;
    }
    return false;
  }

  private void readJdbcMigrations() {
    if (jdbcMigrations != null) {
      for (JdbcMigration jdbcMigration : jdbcMigrations) {
        if (jdbcMigration.matches(context)) {
          versions.add(new LocalJdbcMigrationResource(jdbcMigration.getVersion(), jdbcMigration.getName(), jdbcMigration));
        }
      }
    }
  }

  private URL resource(String base) {
    return LocalMigrationResources.class.getResource(base);
  }

  private void loadFromIndexFile(URL idx, String base) {
    log.log(DEBUG, "Loading index from {0}", idx);
    try (var reader = new LineNumberReader(new InputStreamReader(idx.openStream()))) {
      String line;
      while ((line = reader.readLine()) != null) {
        if (!line.isEmpty()) {
          final String[] pair = line.split(",");
          if (pair.length == 2) {
            final var checksum = Integer.parseInt(pair[0]);
            final var location = pair[1].trim();
            final var substring = location.substring(0, location.length() - 4);
            final var version = MigrationVersion.parse(substring);
            final var url = resource(base + location);
            versions.add(new LocalUriMigrationResource(version, location, url, checksum));
          }
        }
      }
    } catch (IOException e) {
      throw new UncheckedIOException("Error reading idx file", e);
    }
  }

  private void readResourcesForPath(String path) {
    // try to load from base platform first
    final String basePlatform = migrationConfig.getBasePlatform();
    if (basePlatform != null && loadedFrom(path, basePlatform)) {
      return;
    }
    // try to load from specific platform
    final String platform = migrationConfig.getPlatform();
    if (platform != null && loadedFrom(path, platform)) {
      return;
    }
    addResources(scanForMigrations(path));
  }

  /**
   * Return true if migrations were loaded from platform specific location.
   */
  private boolean loadedFrom(String path, String platform) {
    addResources(scanForMigrations(path + "/" + platform));
    if (versions.isEmpty()) {
      return false;
    }
    log.log(DEBUG, "platform migrations for {0}", platform);
    return true;
  }

  /**
   * Scan for SQL migrations.
   */
  private List<Resource> scanForMigrations(String path) {
    return new Scanner(classLoader).scanForResources(path, name -> name.endsWith(".sql"));
  }

  /**
   * adds the script migrations found from classpath scan.
   */
  private void addResources(List<Resource> resourceList) {
    if (!resourceList.isEmpty()) {
      log.log(DEBUG, "resources: {0}", resourceList);
    }
    for (Resource resource : resourceList) {
      String filename = resource.name();
      assert filename.endsWith(".sql");
      String mainName = filename.substring(0, filename.length() - 4);
      versions.add(createScriptMigration(resource, mainName));
    }
  }

  /**
   * Create a script based migration.
   */
  private LocalMigrationResource createScriptMigration(Resource resource, String mainName) {
    MigrationVersion migrationVersion = MigrationVersion.parse(mainName);
    return new LocalDdlMigrationResource(migrationVersion, resource.location(), resource);
  }

  /**
   * Return the list of migration resources in version order.
   */
  List<LocalMigrationResource> versions() {
    return versions;
  }

}
