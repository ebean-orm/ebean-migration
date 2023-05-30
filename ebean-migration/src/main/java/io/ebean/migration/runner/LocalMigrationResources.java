package io.ebean.migration.runner;

import io.avaje.classpath.scanner.Resource;
import io.avaje.classpath.scanner.core.Scanner;
import io.ebean.migration.JdbcMigration;
import io.ebean.migration.MigrationConfig;
import io.ebean.migration.MigrationVersion;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

import static java.lang.System.Logger.Level.DEBUG;

/**
 * Loads the DB migration resources and sorts them into execution order.
 */
public class LocalMigrationResources {

  private static final System.Logger log = MigrationSchema.log;

  private final List<LocalMigrationResource> versions = new ArrayList<>();
  private final MigrationConfig migrationConfig;
  private final ClassLoader classLoader;
  private final boolean searchForJdbcMigrations;

  /**
   * Construct with configuration options.
   */
  public LocalMigrationResources(MigrationConfig migrationConfig) {
    this.migrationConfig = migrationConfig;
    this.classLoader = migrationConfig.getClassLoader();
    this.searchForJdbcMigrations = migrationConfig.getJdbcMigrationFactory() != null;
  }

  /**
   * Read the init migration resources (usually only 1) returning true if there are versions.
   */
  public boolean readInitResources() {
    return readResourcesForPath(migrationConfig.getMigrationInitPath());
  }

  /**
   * Read all the migration resources (SQL scripts) returning true if there are versions.
   */
  public boolean readResources() {
    return readResourcesForPath(migrationConfig.getMigrationPath());
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
  public List<LocalMigrationResource> getVersions() {
    return versions;
  }


  /**
   * Filter used to find the migration scripts.
   */
  private static class Match implements Predicate<String> {

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
  private static class JdbcOnly implements Predicate<String> {
    @Override
    public boolean test(String name) {
      return name.endsWith(".class") && !name.contains("$");
    }
  }
}
