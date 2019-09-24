package io.ebean.migration.runner;

import io.ebean.migration.JdbcMigration;
import io.ebean.migration.MigrationConfig;
import io.ebean.migration.MigrationVersion;
import org.avaje.classpath.scanner.Resource;
import org.avaje.classpath.scanner.ResourceFilter;
import org.avaje.classpath.scanner.core.Scanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Loads the DB migration resources and sorts them into execution order.
 */
public class LocalMigrationResources {

  private static final Logger logger = LoggerFactory.getLogger(LocalMigrationResources.class);

  private final MigrationConfig migrationConfig;

  private final List<LocalMigrationResource> versions = new ArrayList<>();

  /**
   * Construct with configuration options.
   */
  public LocalMigrationResources(MigrationConfig migrationConfig) {
    this.migrationConfig = migrationConfig;
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

    ClassLoader classLoader = migrationConfig.getClassLoader();

    Scanner scanner = new Scanner(classLoader);
    List<Resource> resourceList = scanner.scanForResources(path, new Match(migrationConfig));

    logger.debug("resources: {}", resourceList);

    for (Resource resource : resourceList) {
      String filename = resource.getFilename();
      if (filename.endsWith(migrationConfig.getApplySuffix())) {
        versions.add(createScriptMigration(resource, filename));
      } else if (migrationConfig.getJdbcMigrationFactory() != null && filename.endsWith(".class")) {
        versions.add(createJdbcMigration(resource, filename));
      }
    }

    Collections.sort(versions);
    return !versions.isEmpty();
  }

  /**
   * Return a programmatic JDBC migration.
   */
  private LocalMigrationResource createJdbcMigration(Resource resource, String filename) {
    int pos = filename.lastIndexOf(".class");
    String mainName = filename.substring(0, pos);
    MigrationVersion migrationVersion = MigrationVersion.parse(mainName);
    String className = resource.getLocation().replace('/', '.');
    className = className.substring(0, className.length()-6);
    JdbcMigration instance = migrationConfig.getJdbcMigrationFactory().createInstance(className);
    return new LocalJdbcMigrationResource(migrationVersion, resource.getLocation(), instance);
  }

  /**
   * Create a script based migration.
   */
  private LocalMigrationResource createScriptMigration(Resource resource, String filename) {
    int pos = filename.lastIndexOf(migrationConfig.getApplySuffix());
    String mainName = filename.substring(0, pos);
    MigrationVersion migrationVersion = MigrationVersion.parse(mainName);
    return new LocalDdlMigrationResource(migrationVersion, resource.getLocation(), resource);
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
  private static class Match implements ResourceFilter {

    private final MigrationConfig migrationConfig;

    Match(MigrationConfig migrationConfig) {
      this.migrationConfig = migrationConfig;
    }

    @Override
    public boolean isMatch(String name) {
      return name.endsWith(migrationConfig.getApplySuffix())
          || migrationConfig.getJdbcMigrationFactory() != null && name.endsWith(".class") && !name.contains("$");
    }
  }
}
