package io.ebean.migration.runner;

import io.avaje.classpath.scanner.Resource;
import io.avaje.classpath.scanner.core.Scanner;
import io.ebean.migration.JdbcMigration;
import io.ebean.migration.MigrationConfig;
import io.ebean.migration.MigrationVersion;

import java.io.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

import static java.lang.System.Logger.Level.DEBUG;

/**
 * Loads the DB migration resources and sorts them into execution order.
 */
final class LocalMigrationResources {

  private static final System.Logger log = MigrationSchema.log;

  private final List<LocalMigrationResource> versions = new ArrayList<>();
  private final MigrationConfig migrationConfig;
  private final ClassLoader classLoader;
  private final boolean searchForJdbcMigrations;

  /**
   * Construct with configuration options.
   */
  LocalMigrationResources(MigrationConfig migrationConfig) {
    this.migrationConfig = migrationConfig;
    this.classLoader = migrationConfig.getClassLoader();
    this.searchForJdbcMigrations = migrationConfig.getJdbcMigrationFactory() != null;
  }

  /**
   * Read the init migration resources (usually only 1) returning true if there are versions.
   */
  boolean readInitResources() {
    return readResourcesForPath(migrationConfig.getMigrationInitPath());
  }

  /**
   * Read all the migration resources (SQL scripts) returning true if there are versions.
   */
  boolean readResources() {
    if (readFromIndex()) {
      // automatically enable earlyChecksumMode when using index file with pre-computed checksums
      migrationConfig.setEarlyChecksumMode(true);
      return true;
    }
    return readResourcesForPath(migrationConfig.getMigrationPath());
  }

  private boolean readFromIndex() {
    final var base = "/" + migrationConfig.getMigrationPath() + "/";
    final var basePlatform = migrationConfig.getBasePlatform();
    final var indexName = "idx_" + basePlatform + ".migrations";
    URL idx = resource(base + indexName);
    if (idx != null) {
      return loadFromIndexFile(idx, base);
    }
    idx = resource(base + basePlatform + '/' + indexName);
    if (idx != null) {
      return loadFromIndexFile(idx, base + basePlatform + '/');
    }
    final var platform = migrationConfig.getPlatform();
    idx = resource(base + platform + indexName);
    if (idx != null) {
      return loadFromIndexFile(idx, base + platform + '/');
    }
    return false;
  }

  private URL resource(String base) {
    return LocalMigrationResources.class.getResource(base);
  }

  private boolean loadFromIndexFile(URL idx, String base) {
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
            if (location.endsWith(".class")) {
              String className = base.replace('/', '.').substring(1)
                + location.substring(0, location.length() - 6);
              JdbcMigration instance = migrationConfig.getJdbcMigrationFactory().createInstance(className);
              assert instance.getChecksum() == checksum;
              versions.add(new LocalJdbcMigrationResource(version, location, instance));
            } else {
              final var url = resource(base + location);
              versions.add(new LocalUriMigrationResource(version, location, url, checksum));
            }
          }
        }
      }

      return !versions.isEmpty();

    } catch (IOException e) {
      throw new UncheckedIOException("Error reading idx file", e);
    }
  }

  private boolean readResourcesForPath(String path) {
    // try to load from base platform first
    final String basePlatform = migrationConfig.getBasePlatform();
    if (basePlatform != null && loadedFrom(path, basePlatform)) {
      return true;
    }
    // try to load from specific platform
    final String platform = migrationConfig.getPlatform();
    if (platform != null && loadedFrom(path, platform)) {
      return true;
    }
    addResources(scanForBoth(path));
    Collections.sort(versions);
    return !versions.isEmpty();
  }

  /**
   * Return true if migrations were loaded from platform specific location.
   */
  private boolean loadedFrom(String path, String platform) {
    addResources(scanForBoth(path + "/" + platform));
    if (versions.isEmpty()) {
      return false;
    }
    log.log(DEBUG, "platform migrations for {0}", platform);
    if (searchForJdbcMigrations) {
      addResources(scanForJdbcOnly(path));
    }
    Collections.sort(versions);
    return true;
  }

  /**
   * Scan only for JDBC migrations.
   */
  private List<Resource> scanForJdbcOnly(String path) {
    return new Scanner(classLoader).scanForResources(path, new JdbcOnly());
  }

  /**
   * Scan for both SQL and JDBC migrations.
   */
  private List<Resource> scanForBoth(String path) {
    return new Scanner(classLoader).scanForResources(path, new Match(searchForJdbcMigrations));
  }

  private void addResources(List<Resource> resourceList) {
    if (!resourceList.isEmpty()) {
      log.log(DEBUG, "resources: {0}", resourceList);
    }
    for (Resource resource : resourceList) {
      String filename = resource.name();
      if (filename.endsWith(".sql")) {
        versions.add(createScriptMigration(resource, filename));
      } else if (searchForJdbcMigrations && filename.endsWith(".class")) {
        versions.add(createJdbcMigration(resource, filename));
      }
    }
  }

  /**
   * Return a programmatic JDBC migration.
   */
  private LocalMigrationResource createJdbcMigration(Resource resource, String filename) {
    int pos = filename.lastIndexOf(".class");
    String mainName = filename.substring(0, pos);
    MigrationVersion migrationVersion = MigrationVersion.parse(mainName);
    String className = resource.location().replace('/', '.');
    className = className.substring(0, className.length() - 6);
    JdbcMigration instance = migrationConfig.getJdbcMigrationFactory().createInstance(className);
    return new LocalJdbcMigrationResource(migrationVersion, resource.location(), instance);
  }

  /**
   * Create a script based migration.
   */
  private LocalMigrationResource createScriptMigration(Resource resource, String filename) {
    int pos = filename.lastIndexOf(".sql");
    String mainName = filename.substring(0, pos);
    MigrationVersion migrationVersion = MigrationVersion.parse(mainName);
    return new LocalDdlMigrationResource(migrationVersion, resource.location(), resource);
  }

  /**
   * Return the list of migration resources in version order.
   */
  List<LocalMigrationResource> versions() {
    return versions;
  }

  /**
   * Filter used to find the migration scripts.
   */
  private static final class Match implements Predicate<String> {

    private final boolean searchJdbc;

    Match(boolean searchJdbc) {
      this.searchJdbc = searchJdbc;
    }

    @Override
    public boolean test(String name) {
      return name.endsWith(".sql") || (searchJdbc && name.endsWith(".class") && !name.contains("$"));
    }
  }

  /**
   * Filter to find JDBC migrations only.
   */
  private static final class JdbcOnly implements Predicate<String> {
    @Override
    public boolean test(String name) {
      return name.endsWith(".class") && !name.contains("$");
    }
  }
}
