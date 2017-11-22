package io.ebean.migration;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * Configuration used to run the migration.
 */
public class MigrationConfig {

  private String migrationPath = "dbmigration";

  private String metaTable = "db_migration";

  private String applySuffix = ".sql";

  private String runPlaceholders;

  private boolean skipChecksum;

  private Map<String, String> runPlaceholderMap;

  private ClassLoader classLoader;

  private String dbUsername;
  private String dbPassword;
  private String dbDriver;
  private String dbUrl;

  private String dbSchema;
  private boolean createSchemaIfNotExists;
  private String platformName;

  /**
   * Versions that we want to insert into migration history without actually running.
   */
  private Set<String> patchInsertOn;

  /**
   * Versions that we want to update the checksum on without actually running.
   */
  private Set<String> patchResetChecksumOn;

  /**
   * Return the name of the migration table.
   */
  public String getMetaTable() {
    return metaTable;
  }

  /**
   * Set the name of the migration table.
   */
  public void setMetaTable(String metaTable) {
    this.metaTable = metaTable;
  }

  /**
   * Parse as comma delimited versions.
   */
  private Set<String> parseCommaDelimited(String versionsCommaDelimited) {
    if (versionsCommaDelimited != null) {
      Set<String> versions = new HashSet<>();
      String[] split = versionsCommaDelimited.split(",");
      for (String version : split) {
        if (version.startsWith("R__")) {
          version = version.substring(3);
        }
        versions.add(version);
      }
      return versions;
    }
    return null;
  }

  /**
   * Set the migrations that should have their checksum reset as a comma delimited list.
   */
  public void setPatchResetChecksumOn(String versionsCommaDelimited) {
    patchResetChecksumOn = parseCommaDelimited(versionsCommaDelimited);
  }

  /**
   * Set the migrations that should have their checksum reset.
   */
  public void setPatchResetChecksumOn(Set<String> patchResetChecksumOn) {
    this.patchResetChecksumOn = patchResetChecksumOn;
  }

  /**
   * Return the migrations that should have their checksum reset.
   */
  public Set<String> getPatchResetChecksumOn() {
    return patchResetChecksumOn;
  }

  /**
   * Set the migrations that should not be run but inserted into history as if they have run.
   */
  public void setPatchInsertOn(String versionsCommaDelimited) {
    patchInsertOn = parseCommaDelimited(versionsCommaDelimited);
  }

  /**
   * Set the migrations that should not be run but inserted into history as if they have run.
   * <p>
   * This can be useful when we need to pull out DDL from a repeatable migration that should really
   * only run once. We can pull out that DDL as a new migration and add it to history as if it had been
   * run (we can only do this when we know it exists in all environments including production).
   * </p>
   */
  public void setPatchInsertOn(Set<String> patchInsertOn) {
    this.patchInsertOn = patchInsertOn;
  }

  /**
   * Return the migrations that should not be run but inserted into history as if they have run.
   */
  public Set<String> getPatchInsertOn() {
    return patchInsertOn;
  }

  /**
   * Return true if checksum check should be skipped (during development).
   */
  public boolean isSkipChecksum() {
    return skipChecksum;
  }

  /**
   * Set to true to skip the checksum check.
   * <p>
   * This is intended for use during development only.
   * </p>
   */
  public void setSkipChecksum(boolean skipChecksum) {
    this.skipChecksum = skipChecksum;
  }

  /**
   * Return a Comma and equals delimited key/value placeholders to replace in DDL scripts.
   */
  public String getRunPlaceholders() {
    return runPlaceholders;
  }

  /**
   * Set a Comma and equals delimited key/value placeholders to replace in DDL scripts.
   */
  public void setRunPlaceholders(String runPlaceholders) {
    this.runPlaceholders = runPlaceholders;
  }

  /**
   * Return a map of name/value pairs that can be expressions replaced in migration scripts.
   */
  public Map<String, String> getRunPlaceholderMap() {
    return runPlaceholderMap;
  }

  /**
   * Set a map of name/value pairs that can be expressions replaced in migration scripts.
   */
  public void setRunPlaceholderMap(Map<String, String> runPlaceholderMap) {
    this.runPlaceholderMap = runPlaceholderMap;
  }

  /**
   * Return the root path used to find migrations.
   */
  public String getMigrationPath() {
    return migrationPath;
  }

  /**
   * Set the root path used to find migrations.
   */
  public void setMigrationPath(String migrationPath) {
    this.migrationPath = migrationPath;
  }

  /**
   * Return the suffix for migration resources (defaults to .sql).
   */
  public String getApplySuffix() {
    return applySuffix;
  }

  /**
   * Set the suffix for migration resources.
   */
  public void setApplySuffix(String applySuffix) {
    this.applySuffix = applySuffix;
  }

  /**
   * Return the DB username.
   * <p>
   * Used when a Connection to run the migration is not supplied.
   * </p>
   */
  public String getDbUsername() {
    return dbUsername;
  }

  /**
   * Set the DB username.
   * <p>
   * Used when a Connection to run the migration is not supplied.
   * </p>
   */
  public void setDbUsername(String dbUsername) {
    this.dbUsername = dbUsername;
  }

  /**
   * Return the DB password.
   * <p>
   * Used when creating a Connection to run the migration.
   * </p>
   */
  public String getDbPassword() {
    return dbPassword;
  }

  /**
   * Set the DB password.
   * <p>
   * Used when creating a Connection to run the migration.
   * </p>
   */
  public void setDbPassword(String dbPassword) {
    this.dbPassword = dbPassword;
  }

  /**
   * Return the DB Driver.
   * <p>
   * Used when creating a Connection to run the migration.
   * </p>
   */
  public String getDbDriver() {
    return dbDriver;
  }

  /**
   * Set the DB Driver.
   * <p>
   * Used when creating a Connection to run the migration.
   * </p>
   */
  public void setDbDriver(String dbDriver) {
    this.dbDriver = dbDriver;
  }

  /**
   * Return the DB connection URL.
   * <p>
   * Used when creating a Connection to run the migration.
   * </p>
   */
  public String getDbUrl() {
    return dbUrl;
  }

  /**
   * Set the DB connection URL.
   * <p>
   * Used when creating a Connection to run the migration.
   * </p>
   */
  public void setDbUrl(String dbUrl) {
    this.dbUrl = dbUrl;
  }

  /**
   * Return the DB connection Schema.
   * <p>
   * Used when creating a Connection to run the migration.
   * </p>
   */
  public String getDbSchema() {
    return dbSchema;
  }

  /**
   * Set the DB connection Schema.
   * <p>
   * Used when creating a Connection to run the migration.
   * </p>
   */
  public void setDbSchema(String dbSchema) {
    this.dbSchema = dbSchema;
  }

  /**
   * Return true if migration should create the schema if it does not exist.
   */
  public boolean isCreateSchemaIfNotExists() {
    return createSchemaIfNotExists;
  }

  /**
   * Set to create Schema if it does not exist.
   */
  public void setCreateSchemaIfNotExists(boolean createSchemaIfNotExists) {
    this.createSchemaIfNotExists = createSchemaIfNotExists;
  }

  /**
   * Return the DB platform name (used for platform create table and select for update syntax).
   */
  public String getPlatformName() {
    return platformName;
  }

  /**
   * Set a DB platform name (to load specific create table and select for update syntax).
   */
  public void setPlatformName(String platformName) {
    this.platformName = platformName;
  }

  /**
   * Return the ClassLoader to use to load resources.
   */
  public ClassLoader getClassLoader() {
    if (classLoader == null) {
      classLoader = Thread.currentThread().getContextClassLoader();
      if (classLoader == null) {
        classLoader = this.getClass().getClassLoader();
      }
    }
    return classLoader;
  }

  /**
   * Set the ClassLoader to use when loading resources.
   */
  public void setClassLoader(ClassLoader classLoader) {
    this.classLoader = classLoader;
  }

  /**
   * Load configuration from standard properties.
   */
  public void load(Properties props) {

    dbUsername = props.getProperty("dbmigration.username", dbUsername);
    dbPassword = props.getProperty("dbmigration.password", dbPassword);
    dbDriver = props.getProperty("dbmigration.driver", dbDriver);
    dbUrl = props.getProperty("dbmigration.url", dbUrl);

    String skip = props.getProperty("dbmigration.skipchecksum");
    if (skip != null) {
      skipChecksum = Boolean.parseBoolean(skip);
    }
    platformName = props.getProperty("dbmigration.platformName", platformName);
    applySuffix = props.getProperty("dbmigration.applySuffix", applySuffix);
    metaTable = props.getProperty("dbmigration.metaTable", metaTable);
    migrationPath = props.getProperty("dbmigration.migrationPath", migrationPath);
    runPlaceholders = props.getProperty("dbmigration.placeholders", runPlaceholders);
  }

  /**
   * Create a Connection to the database using the configured driver, url, username etc.
   * <p>
   * Used when an existing DataSource or Connection is not supplied.
   * </p>
   */
  public Connection createConnection() {

    if (dbUsername == null) throw new MigrationException("Database username is null?");
    if (dbPassword == null) throw new MigrationException("Database password is null?");
    if (dbDriver == null) throw new MigrationException("Database Driver is null?");
    if (dbUrl == null) throw new MigrationException("Database connection URL is null?");

    loadDriver();

    try {
      Properties props = new Properties();
      props.setProperty("user", dbUsername);
      props.setProperty("password", dbPassword);
      return DriverManager.getConnection(dbUrl, props);

    } catch (SQLException e) {
      throw new MigrationException("Error trying to create Connection", e);
    }
  }

  private void loadDriver() {
    try {
      ClassLoader contextLoader = getClassLoader();
      Class.forName(dbDriver, true, contextLoader);
    } catch (Throwable e) {
      throw new MigrationException("Problem loading Database Driver [" + dbDriver + "]: " + e.getMessage(), e);
    }
  }

}
