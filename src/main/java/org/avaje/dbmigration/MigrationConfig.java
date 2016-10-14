package org.avaje.dbmigration;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Map;
import java.util.Properties;

/**
 * Configuration used to run the migration.
 */
public class MigrationConfig {

  private String migrationPath = "dbmigration";

  private String metaTable = "db_migration";

  private String applySuffix = ".sql";

  private String runPlaceholders;

  private Map<String, String> runPlaceholderMap;

  private ClassLoader classLoader;

  private String dbUsername;
  private String dbPassword;
  private String dbDriver;
  private String dbUrl;

  private String dbSchema;
  private boolean createSchemaIfNotExists = false;

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
   * Return if Create Schema if not exits
   * <p>
   * Used when creating a Connection to run the migration.
   * </p>
   */
  public boolean getCreateSchemaIfNotExists() {
    return createSchemaIfNotExists;
  }

  /**
   * Set to create Schema if not exits
   * <p>
   * Used when creating a Connection to run the migration.
   * </p>
   */
  public void setCreateSchemaIfNotExists(boolean createSchemaIfNotExists) {this.createSchemaIfNotExists = createSchemaIfNotExists;  }

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
