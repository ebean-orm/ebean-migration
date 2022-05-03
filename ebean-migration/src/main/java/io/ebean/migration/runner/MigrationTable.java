package io.ebean.migration.runner;

import io.ebean.ddlrunner.ScriptTransform;
import io.ebean.migration.JdbcMigration;
import io.ebean.migration.MigrationConfig;
import io.ebean.migration.MigrationException;
import io.ebean.migration.MigrationVersion;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.lang.System.Logger.Level;
import java.net.URL;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

import static io.ebean.migration.MigrationVersion.BOOTINIT_TYPE;
import static io.ebean.migration.MigrationVersion.VERSION_TYPE;

/**
 * Manages the migration table.
 */
public class MigrationTable {

  private static final System.Logger log = System.getLogger("io.ebean.DDL");

  private static final String INIT_VER_0 = "0";

  private final Connection connection;
  private final boolean checkState;
  private final MigrationPlatform platform;
  private final MigrationScriptRunner scriptRunner;
  private final String catalog;
  private final String schema;
  private final String table;
  private final String sqlTable;
  private final String envUserName;
  private final String basePlatformName;
  private final String platformName;

  private final Timestamp runOn = new Timestamp(System.currentTimeMillis());

  private final ScriptTransform scriptTransform;

  private final String insertSql;
  private final String updateSql;
  private final String updateChecksumSql;

  private final LinkedHashMap<String, MigrationMetaRow> migrations;
  private final boolean skipChecksum;
  private final boolean skipMigrationRun;

  private final Set<String> patchInsertVersions;
  private final Set<String> patchResetChecksumVersions;
  private final boolean allowErrorInRepeatable;

  private final MigrationVersion minVersion;
  private final String minVersionFailMessage;

  private MigrationVersion currentVersion;
  private MigrationMetaRow lastMigration;
  private LocalMigrationResource priorVersion;

  private final List<LocalMigrationResource> checkMigrations = new ArrayList<>();

  /**
   * Version of a dbinit script. When set this means all migration version less than this are ignored.
   */
  private MigrationVersion dbInitVersion;

  /**
   * Construct with server, configuration and jdbc connection (DB admin user).
   */
  public MigrationTable(MigrationConfig config, Connection connection, boolean checkState, MigrationPlatform platform) {
    this.platform = platform;
    this.connection = connection;
    this.scriptRunner = new MigrationScriptRunner(connection, platform);
    this.checkState = checkState;
    this.migrations = new LinkedHashMap<>();
    this.catalog = null;
    this.allowErrorInRepeatable = config.isAllowErrorInRepeatable();
    this.patchResetChecksumVersions = config.getPatchResetChecksumOn();
    this.patchInsertVersions = config.getPatchInsertOn();
    this.minVersion = initMinVersion(config.getMinVersion());
    this.minVersionFailMessage = config.getMinVersionFailMessage();
    this.skipMigrationRun = config.isSkipMigrationRun();
    this.skipChecksum = config.isSkipChecksum();
    this.schema = config.getDbSchema();
    this.table = config.getMetaTable();
    this.basePlatformName = config.getBasePlatform();
    this.platformName = config.getPlatform();
    this.sqlTable = initSqlTable();
    this.insertSql = MigrationMetaRow.insertSql(sqlTable);
    this.updateSql = MigrationMetaRow.updateSql(sqlTable);
    this.updateChecksumSql = MigrationMetaRow.updateChecksumSql(sqlTable);
    this.scriptTransform = createScriptTransform(config);
    this.envUserName = System.getProperty("user.name");
  }

  private MigrationVersion initMinVersion(String minVersion) {
    return (minVersion == null || minVersion.isEmpty()) ? null : MigrationVersion.parse(minVersion);
  }

  private String initSqlTable() {
    if (schema != null) {
      return schema + "." + table;
    } else {
      return table;
    }
  }

  private String sqlPrimaryKey() {
    return "pk_" + table;
  }

  /**
   * Return the number of migrations in the DB migration table.
   */
  public int size() {
    return migrations.size();
  }

  /**
   * Returns the versions that are already applied.
   */
  @Nonnull
  public Set<String> getVersions() {
    return migrations.keySet();
  }

  /**
   * Create the ScriptTransform for placeholder key/value replacement.
   */
  private ScriptTransform createScriptTransform(MigrationConfig config) {
    return ScriptTransform.build(config.getRunPlaceholders(), config.getRunPlaceholderMap());
  }

  /**
   * Create the table is it does not exist.
   * <p>
   * Also holds DB lock on migration table and loads existing migrations.
   * </p>
   */
  public void createIfNeededAndLock() throws SQLException, IOException {
    SQLException sqlEx = null;
    if (!tableExists()) {
      try {
        createTable();
      } catch (SQLException e) {
        if (tableExists()) {
          sqlEx = e;
          log.log(Level.INFO, "Ignoring error during table creation, as an other process may have created the table", e);
        } else {
          throw e;
        }
      }
    }
    try {
      obtainLockWithWait();
    } catch (RuntimeException re) {
      // catch "failed to obtain row locks"
      if (sqlEx != null) {
        re.addSuppressed(sqlEx);
      }
      throw re;
    }
    readExistingMigrations();
  }

  /**
   * Obtain lock with wait, note that other nodes can insert and commit
   * into the migration table during the wait so this query result won't
   * contain all the executed migrations in that case.
   */
  private void obtainLockWithWait() throws SQLException {
    platform.lockMigrationTable(sqlTable, connection);
  }

  /**
   * Release a lock on the migration table (MySql, MariaDB only).
   */
  public void unlockMigrationTable() throws SQLException {
    platform.unlockMigrationTable(sqlTable, connection);
  }

  /**
   * Read the migration table with details on what migrations have run.
   * This must execute after we have completed the wait for the lock on
   * the migration table such that it reads any migrations that have
   * executed during the wait for the lock.
   */
  private void readExistingMigrations() throws SQLException {
    for (MigrationMetaRow metaRow : platform.readExistingMigrations(sqlTable, connection)) {
      addMigration(metaRow.getVersion(), metaRow);
    }
  }

  private void createTable() throws IOException, SQLException {
    scriptRunner.runScript(createTableDdl(), "create migration table");
    createInitMetaRow().executeInsert(connection, insertSql);
    connection.commit();
  }

  /**
   * Return the create table script.
   */
  @Nonnull
  String createTableDdl() throws IOException {
    String script = ScriptTransform.replace("${table}", sqlTable, getCreateTableScript());
    return ScriptTransform.replace("${pk_table}", sqlPrimaryKey(), script);
  }

  /**
   * Return the create table script.
   */
  private String getCreateTableScript() throws IOException {
    // supply a script to override the default table create script
    String script = readResource("migration-support/create-table.sql");
    if (script == null && platformName != null && !platformName.isEmpty()) {
      // look for platform specific create table
      script = readResource("migration-support/" + platformName + "-create-table.sql");
    }
    if (script == null && basePlatformName != null && !basePlatformName.isEmpty()) {
      // look for platform specific create table
      script = readResource("migration-support/" + basePlatformName + "-create-table.sql");
    }
    if (script == null) {
      // no, just use the default script
      script = readResource("migration-support/default-create-table.sql");
    }
    return script;
  }

  private String readResource(String location) throws IOException {
    Enumeration<URL> resources = getClassLoader().getResources(location);
    if (resources.hasMoreElements()) {
      URL url = resources.nextElement();
      return IOUtils.readUtf8(url);
    }
    return null;
  }

  private ClassLoader getClassLoader() {
    return Thread.currentThread().getContextClassLoader();
  }

  /**
   * Return true if the table exists.
   */
  private boolean tableExists() throws SQLException {
    String migTable = table;
    DatabaseMetaData metaData = connection.getMetaData();
    if (metaData.storesUpperCaseIdentifiers()) {
      migTable = migTable.toUpperCase();
    }
    String checkCatalog = (catalog != null) ? catalog : trim(connection.getCatalog());
    String checkSchema = (schema != null) ? schema : trim(connection.getSchema());
    try (ResultSet tables = metaData.getTables(checkCatalog, checkSchema, migTable, null)) {
      return tables.next();
    }
  }

  private String trim(String s) {
    return s == null ? null : s.trim();
  }

  /**
   * Return true if the migration ran successfully and false if the migration failed.
   */
  private boolean shouldRun(LocalMigrationResource localVersion, LocalMigrationResource prior) throws SQLException {
    if (prior != null && !localVersion.isRepeatable()) {
      if (!migrationExists(prior)) {
        log.log(Level.ERROR, "Migration {0} requires prior migration {1} which has not been run", localVersion.getVersion(), prior.getVersion());
        return false;
      }
    }

    MigrationMetaRow existing = migrations.get(localVersion.key());
    if (!runMigration(localVersion, existing)) {
      return false;
    }

    // migration was run successfully ...
    priorVersion = localVersion;
    return true;
  }

  /**
   * Run the migration script.
   *
   * @param local    The local migration resource
   * @param existing The information for this migration existing in the table
   * @return True if the migrations should continue
   */
  private boolean runMigration(LocalMigrationResource local, MigrationMetaRow existing) throws SQLException {
    String script = null;
    int checksum;
    if (local instanceof LocalDdlMigrationResource) {
      script = convertScript(local.getContent());
      checksum = Checksum.calculate(script);
    } else {
      checksum = ((LocalJdbcMigrationResource) local).getChecksum();
    }

    if (existing == null && patchInsertMigration(local, checksum)) {
      return true;
    }
    if (existing != null && skipMigration(checksum, local, existing)) {
      return true;
    }
    executeMigration(local, script, checksum, existing);
    return true;
  }

  /**
   * Return true if we 'patch history' inserting a DB migration without running it.
   */
  private boolean patchInsertMigration(LocalMigrationResource local, int checksum) throws SQLException {
    if (patchInsertVersions != null && patchInsertVersions.contains(local.key())) {
      log.log(Level.INFO, "Patch migration, insert into history {0}", local.getLocation());
      if (!checkState) {
        insertIntoHistory(local, checksum, 0);
      }
      return true;
    }
    return false;
  }

  /**
   * Return true if the migration should be skipped.
   */
  boolean skipMigration(int checksum, LocalMigrationResource local, MigrationMetaRow existing) throws SQLException {
    boolean matchChecksum = (existing.getChecksum() == checksum);
    if (matchChecksum) {
      log.log(Level.TRACE, "skip unchanged migration {0}", local.getLocation());
      return true;

    } else if (patchResetChecksum(existing, checksum)) {
      log.log(Level.INFO, "Patch migration, reset checksum on {0}", local.getLocation());
      return true;

    } else if (local.isRepeatable() || skipChecksum) {
      // re-run the migration
      return false;
    } else {
      throw new MigrationException("Checksum mismatch on migration " + local.getLocation());
    }
  }

  /**
   * Return true if the checksum is reset on the existing migration.
   */
  private boolean patchResetChecksum(MigrationMetaRow existing, int newChecksum) throws SQLException {
    if (isResetOnVersion(existing.getVersion())) {
      if (!checkState) {
        existing.resetChecksum(newChecksum, connection, updateChecksumSql);
      }
      return true;
    } else {
      return false;
    }
  }

  private boolean isResetOnVersion(String version) {
    return patchResetChecksumVersions != null && patchResetChecksumVersions.contains(version);
  }

  /**
   * Run a migration script as new migration or update on existing repeatable migration.
   */
  private void executeMigration(LocalMigrationResource local, String script, int checksum, MigrationMetaRow existing) throws SQLException {
    if (checkState) {
      checkMigrations.add(local);
      // simulate the migration being run such that following migrations also match
      addMigration(local.key(), createMetaRow(local, checksum, 1));
      return;
    }

    long exeMillis = 0;
    try {
      if (skipMigrationRun) {
        log.log(Level.DEBUG, "skip migration {0}", local.getLocation());
      } else {
        exeMillis = executeMigration(local, script);
      }
      if (existing != null) {
        existing.rerun(checksum, exeMillis, envUserName, runOn);
        existing.executeUpdate(connection, updateSql);
      } else {
        insertIntoHistory(local, checksum, exeMillis);
      }
    } catch (SQLException e) {
      if (allowErrorInRepeatable && local.isRepeatableLast()) {
        // log the exception and continue on repeatable migration
        log.log(Level.ERROR, "Continue migration with error executing repeatable migration " + local.getVersion(), e);
      } else {
        throw e;
      }
    }
  }

  private long executeMigration(LocalMigrationResource local, String script) throws SQLException {
    long start = System.currentTimeMillis();
    if (local instanceof LocalDdlMigrationResource) {
      log.log(Level.DEBUG, "run migration {0}", local.getLocation());
      scriptRunner.runScript(script, "run migration version: " + local.getVersion());
    } else {
      JdbcMigration migration = ((LocalJdbcMigrationResource) local).getMigration();
      log.log(Level.INFO, "Executing jdbc migration version: {0} - {1}", local.getVersion(), migration);
      migration.migrate(connection);
    }
    return System.currentTimeMillis() - start;
  }

  private void insertIntoHistory(LocalMigrationResource local, int checksum, long exeMillis) throws SQLException {
    MigrationMetaRow metaRow = createMetaRow(local, checksum, exeMillis);
    metaRow.executeInsert(connection, insertSql);
    addMigration(local.key(), metaRow);
  }

  private MigrationMetaRow createInitMetaRow() {
    return new MigrationMetaRow(0, "I", INIT_VER_0, "<init>", 0, envUserName, runOn, 0);
  }

  /**
   * Create the MigrationMetaRow for this migration.
   */
  private MigrationMetaRow createMetaRow(LocalMigrationResource migration, int checksum, long exeMillis) {

    int nextId = 1;
    if (lastMigration != null) {
      nextId = lastMigration.getId() + 1;
    }

    String type = migration.getType();
    String runVersion = migration.key();
    String comment = migration.getComment();

    return new MigrationMetaRow(nextId, type, runVersion, comment, checksum, envUserName, runOn, exeMillis);
  }

  /**
   * Return true if the migration exists.
   */
  private boolean migrationExists(LocalMigrationResource priorVersion) {
    return migrations.containsKey(priorVersion.key());
  }

  /**
   * Apply the placeholder key/value replacement on the script.
   */
  private String convertScript(String script) {
    return scriptTransform.transform(script);
  }

  /**
   * Register the successfully executed migration (to allow dependant scripts to run).
   */
  private void addMigration(String key, MigrationMetaRow metaRow) {
    if (INIT_VER_0.equals(key)) {
      // ignore the version 0 <init> row
      return;
    }
    lastMigration = metaRow;
    if (metaRow.getVersion() == null) {
      throw new IllegalStateException("No runVersion in db migration table row? " + metaRow);
    }

    migrations.put(key, metaRow);
    if (VERSION_TYPE.equals(metaRow.getType()) || BOOTINIT_TYPE.equals(metaRow.getType())) {
      MigrationVersion rowVersion = MigrationVersion.parse(metaRow.getVersion());
      if (currentVersion == null || rowVersion.compareTo(currentVersion) > 0) {
        currentVersion = rowVersion;
      }
      if (BOOTINIT_TYPE.equals(metaRow.getType())) {
        dbInitVersion = rowVersion;
      }
    }
  }

  /**
   * Return true if there are no migrations.
   */
  public boolean isEmpty() {
    return migrations.isEmpty();
  }

  /**
   * Run all the migrations in order as needed.
   *
   * @return the migrations that have been run (collected if checkstate is true).
   */
  @Nonnull
  public List<LocalMigrationResource> runAll(List<LocalMigrationResource> localVersions) throws SQLException {

    checkMinVersion();
    for (LocalMigrationResource localVersion : localVersions) {
      if (!localVersion.isRepeatable() && dbInitVersion != null && dbInitVersion.compareTo(localVersion.getVersion()) >= 0) {
        log.log(Level.DEBUG, "migration skipped by dbInitVersion {0}", dbInitVersion);
      } else if (!shouldRun(localVersion, priorVersion)) {
        break;
      }
    }
    return checkMigrations;
  }

  private void checkMinVersion() {
    if (minVersion != null && currentVersion != null && currentVersion.compareTo(minVersion) < 0) {
      StringBuilder sb = new StringBuilder();
      if (minVersionFailMessage != null && !minVersionFailMessage.isEmpty()) {
        sb.append(minVersionFailMessage).append(' ');
      }
      sb.append("MigrationVersion mismatch: v").append(currentVersion).append(" < v").append(minVersion);
      throw new MigrationException(sb.toString());
    }
  }

  /**
   * Run using an init migration.
   *
   * @return the migrations that have been run (collected if checkstate is true).
   */
  @Nonnull
  public List<LocalMigrationResource> runInit(LocalMigrationResource initVersion, List<LocalMigrationResource> localVersions) throws SQLException {

    runRepeatableInit(localVersions);

    initVersion.setInitType();
    if (!shouldRun(initVersion, null)) {
      throw new IllegalStateException("Expected to run init migration but it didn't?");
    }

    // run any migrations greater that the init migration
    for (LocalMigrationResource localVersion : localVersions) {
      if (localVersion.compareTo(initVersion) > 0 && !shouldRun(localVersion, priorVersion)) {
        break;
      }
    }
    return checkMigrations;
  }

  private void runRepeatableInit(List<LocalMigrationResource> localVersions) throws SQLException {
    for (LocalMigrationResource localVersion : localVersions) {
      if (!localVersion.isRepeatableInit() || !shouldRun(localVersion, priorVersion)) {
        break;
      }
    }
  }

  /**
   * Run non transactional statements (if any) after migration commit.
   * <p>
   * These run with auto commit true and run AFTER the migration commit and
   * as such the migration isn't truely atomic - the migration can run and
   * complete and the non-transactional statements fail.
   */
  public void runNonTransactional() {
    scriptRunner.runNonTransactional();
  }
}
