package io.ebean.migration.runner;

import io.ebean.migration.ConfigurationAware;
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
   * Read all the migration resources (SQL scripts) returning true if there are versions.
   */
  public boolean readResources() {

    String migrationPath = migrationConfig.getMigrationPath();

    ClassLoader classLoader = migrationConfig.getClassLoader();

    Scanner scanner = new Scanner(classLoader);
    List<Resource> resourceList = scanner.scanForResources(migrationPath, new Match(migrationConfig));

    logger.debug("resources: {}", resourceList);

    for (Resource resource : resourceList) {
      String filename = resource.getFilename();
      if (filename.endsWith(migrationConfig.getApplySuffix())) {
        int pos = filename.lastIndexOf(migrationConfig.getApplySuffix());
        String mainName = filename.substring(0, pos);

        MigrationVersion migrationVersion = MigrationVersion.parse(mainName);
        LocalMigrationResource res = new LocalDdlMigrationResource(migrationVersion, resource.getLocation(), resource);
        versions.add(res);
      } else if (migrationConfig.getJdbcMigrationFactory() != null && filename.endsWith(".class")) {
        int pos = filename.lastIndexOf(".class");
        String mainName = filename.substring(0, pos);
        MigrationVersion migrationVersion = MigrationVersion.parse(mainName);
        String className = resource.getLocation().replace('/', '.');
        className = className.substring(0, className.length()-6);
        JdbcMigration instance = migrationConfig.getJdbcMigrationFactory().createInstance(className);
        LocalMigrationResource res = new LocalJdbcMigrationResource(migrationVersion, resource.getLocation(), instance);
        versions.add(res);
      }
    }

    Collections.sort(versions);
    return !versions.isEmpty();
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
          || migrationConfig.getJdbcMigrationFactory() != null && name.endsWith(".class");
    }
  }
}
