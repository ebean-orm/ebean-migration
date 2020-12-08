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
  private String migrationInitPath = "dbinit";
  private String metaTable = "db_migration";
  private String runPlaceholders;
  private Map<String, String> runPlaceholderMap;

  private boolean skipMigrationRun;
  private boolean skipChecksum;
  private ClassLoader classLoader;

  private String dbUsername;
  private String dbPassword;
  private String dbUrl;
  private String dbSchema;

  private boolean createSchemaIfNotExists = true;
  private boolean setCurrentSchema = true;
  private boolean allowErrorInRepeatable;
  private String platformName;

  private JdbcMigrationFactory jdbcMigrationFactory = new DefaultMigrationFactory();

  /**
   * Versions that we want to insert into migration history without actually running.
   */
  private Set<String> patchInsertOn;

  /**
   * Versions that we want to update the checksum on without actually running.
   */
  private Set<String> patchResetChecksumOn;

  /**
   * The minimum version, that must be in the dbmigration table. If the current maxVersion
   * in the migration table is less than this version, the MigrationRunner will fail
   * with a {@link MigrationException} and an optional {@link #minVersionFailMessage}
   * to enforce certain migration paths.
   */
  private String minVersion;

  /**
   * The (customizable) fail message, if minVersion is not in database.
   * (e.g. "To perform an upgrade, you must install APP XY first")
   */
  private String minVersionFailMessage;

  /**
   * The database name we load the configuration properties for.
   */
  private String name;

  private Properties properties;

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
   * Return true if we continue running the migration when a repeatable migration fails.
   */
  public boolean isAllowErrorInRepeatable() {
    return allowErrorInRepeatable;
  }

  /**
   * Set to true to continue running the migration when a repeatable migration fails.
   */
  public void setAllowErrorInRepeatable(boolean allowErrorInRepeatable) {
    this.allowErrorInRepeatable = allowErrorInRepeatable;
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
   * Return true if the migration should NOT execute the migrations
   * but update the migration table.
   * <p>
   * This can be used to migrate from Flyway where all existing migrations
   * are treated as being executed.
   */
  public boolean isSkipMigrationRun() {
    return skipMigrationRun;
  }

  /**
   * Set to true if the migration should NOT execute the migrations
   * but update the migration table only.
   * <p>
   * This can be used to migrate from Flyway where all existing migrations
   * are treated as being executed.
   */
  public void setSkipMigrationRun(boolean skipMigrationRun) {
    this.skipMigrationRun = skipMigrationRun;
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
   * Return the path for containing init migration scripts.
   */
  public String getMigrationInitPath() {
    return migrationInitPath;
  }

  /**
   * Set the path containing init migration scripts.
   */
  public void setMigrationInitPath(String migrationInitPath) {
    this.migrationInitPath = migrationInitPath;
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
   * Deprecated - not required.
   * <p>
   * Used when creating a Connection to run the migration.
   * </p>
   */
  @Deprecated
  public void setDbDriver(String dbDriver) {
    // do nothing
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
   * Return true if the dbSchema should be set as current schema.
   */
  public boolean isSetCurrentSchema() {
    return setCurrentSchema;
  }

  /**
   * Set if the dbSchema should be set as current schema.
   * <p>
   * We want to set this to false for the case of Postgres where the dbSchema matches the DB username.
   * If we set the dbSchema that can mess up the Postgres search path so we turn this off in that case.
   * </p>
   */
  public void setSetCurrentSchema(boolean setCurrentSchema) {
    this.setCurrentSchema = setCurrentSchema;
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
   * Return the jdbcMigrationFactory.
   */
  public JdbcMigrationFactory getJdbcMigrationFactory() {
    return jdbcMigrationFactory;
  }

  /**
   * Set the jdbcMigrationFactory.
   */
  public void setJdbcMigrationFactory(JdbcMigrationFactory jdbcMigrationFactory) {
    this.jdbcMigrationFactory = jdbcMigrationFactory;
  }

  /**
   * Return the minVersion.
   */
  public String getMinVersion() {
    return minVersion;
  }

  /**
   * Set the minVersion.
   */
  public void setMinVersion(String minVersion) {
    this.minVersion = minVersion;
  }

  /**
   * Return the optional minVersionFailMessage.
   */
  public String getMinVersionFailMessage() {
    return minVersionFailMessage;
  }

  /**
   * Set the minVersionFailMessage
   */
  public void setMinVersionFailMessage(String minVersionFailMessage) {
    this.minVersionFailMessage = minVersionFailMessage;
  }

  /**
   * Load configuration from standard properties.
   */
  public void load(Properties props) {
    this.properties = props;
    dbUsername = getProperty("username", dbUsername);
    dbPassword = getProperty("password", dbPassword);
    dbUrl = getProperty("url", dbUrl);
    dbSchema = getProperty("schema", dbSchema);
    skipMigrationRun = getBool("skipMigrationRun", skipMigrationRun);
    skipChecksum = getBool("skipChecksum", skipChecksum);
    createSchemaIfNotExists = getBool("createSchemaIfNotExists", createSchemaIfNotExists);
    setCurrentSchema = getBool("setCurrentSchema", setCurrentSchema);
    platformName = getProperty("platformName", platformName);
    metaTable = getProperty("metaTable", metaTable);
    migrationPath = getProperty("migrationPath", migrationPath);
    migrationInitPath = getProperty("migrationInitPath", migrationInitPath);
    runPlaceholders = getProperty("placeholders", runPlaceholders);
    minVersion = getProperty("minVersion", minVersion);
    minVersionFailMessage = getProperty("minVersionFailMessage", minVersionFailMessage);

    String patchInsertOn = getProperty("patchInsertOn");
    if (patchInsertOn != null) {
      setPatchInsertOn(patchInsertOn);
    }
    String patchResetChecksumOn = getProperty("patchResetChecksumOn");
    if (patchResetChecksumOn != null) {
      setPatchResetChecksumOn(patchResetChecksumOn);
    }
    String runPlaceholders = getProperty("runPlaceholders");
    if (runPlaceholders != null) {
      setRunPlaceholders(runPlaceholders);
    }
  }

  private boolean getBool(String key, boolean value) {
    String val = getProperty(key);
    return val != null ? Boolean.parseBoolean(val) : value;
  }

  private String getProperty(String key) {
    return getProperty(key, null);
  }

  private String getProperty(String key, String defaultVal) {
    String val = properties.getProperty("ebean." + name + ".migration." + key);
    if (val != null) {
      return val;
    }
    val = properties.getProperty("ebean.migration." + key);
    if (val != null) {
      return val;
    }
    return properties.getProperty("dbmigration." + key, defaultVal);
  }

  /**
   * Create a Connection to the database using the configured driver, url, username etc.
   * <p>
   * Used when an existing DataSource or Connection is not supplied.
   */
  public Connection createConnection() {
    if (dbUsername == null) throw new MigrationException("Database username is null?");
    if (dbPassword == null) throw new MigrationException("Database password is null?");
    if (dbUrl == null) throw new MigrationException("Database connection URL is null?");
    try {
      Properties props = new Properties();
      props.setProperty("user", dbUsername);
      props.setProperty("password", dbPassword);
      return DriverManager.getConnection(dbUrl, props);

    } catch (SQLException e) {
      throw new MigrationException("Error trying to create Connection", e);
    }
  }

  /**
   * Set the name of the database to run the migration for.
   * <p>
   * This name is used when loading properties like:
   * <code>ebean.${name}.migration.migrationPath</code>
   */
  public void setName(String name) {
    this.name = name;
  }

  /**
   * Default factory. Uses the migration's class loader and injects the config if necessary.
   *
   * @author Roland Praml, FOCONIS AG
   */
  public class DefaultMigrationFactory implements JdbcMigrationFactory {

    @Override
    public JdbcMigration createInstance(String className) {
      try {
        Class<?> clazz = Class.forName(className, true, MigrationConfig.this.getClassLoader());
        JdbcMigration migration = (JdbcMigration) clazz.getDeclaredConstructor().newInstance();
        if (migration instanceof ConfigurationAware) {
          ((ConfigurationAware) migration).setMigrationConfig(MigrationConfig.this);
        }
        return migration;
      } catch (Exception e) {
        throw new IllegalArgumentException(className + " is not a valid JdbcMigration", e);
      }
    }
  }

}
