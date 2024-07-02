package io.ebean.migration.runner;

import io.avaje.applog.AppLog;
import io.ebean.ddlrunner.ScriptTransform;
import io.ebean.migration.*;

import java.io.IOException;
import java.net.URL;
import java.sql.*;
import java.util.*;

import static io.ebean.migration.MigrationVersion.BOOTINIT_TYPE;
import static io.ebean.migration.MigrationVersion.VERSION_TYPE;
import static java.lang.System.Logger.Level.*;

/**
 * Manages the migration table.
 */
final class MigrationTable {

  static final System.Logger log = AppLog.getLogger("io.ebean.DDL");

  private static final String INIT_VER_0 = "0";
  private static final int LEGACY_MODE_CHECKSUM = 0;
  private static final int EARLY_MODE_CHECKSUM = 1;
  private static final int AUTO_PATCH_CHECKSUM = -1;

  private final MigrationConfig config;
  private final MigrationContext context;
  private final boolean checkStateOnly;
  private boolean earlyChecksumMode;
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

  private final List<MigrationResource> checkMigrations = new ArrayList<>();

  /**
   * Version of a dbinit script. When set this means all migration version less than this are ignored.
   */
  private MigrationVersion dbInitVersion;

  private int executionCount;
  private boolean patchLegacyChecksums;
  private MigrationMetaRow initMetaRow;
  private final boolean tableKnownToExist;

  public MigrationTable(FirstCheck firstCheck, boolean checkStateOnly) {
    this.config = firstCheck.config;
    this.platform = firstCheck.platform;
    this.context = firstCheck.context;
    this.schema = firstCheck.schema;
    this.table = firstCheck.table;
    this.sqlTable = firstCheck.sqlTable;
    this.tableKnownToExist = firstCheck.tableKnownToExist;

    this.scriptRunner = new MigrationScriptRunner(context.connection(), platform);
    this.checkStateOnly = checkStateOnly;
    this.earlyChecksumMode = config.isEarlyChecksumMode();
    this.migrations = new LinkedHashMap<>();
    this.catalog = null;
    this.allowErrorInRepeatable = config.isAllowErrorInRepeatable();
    this.patchResetChecksumVersions = config.getPatchResetChecksumOn();
    this.patchInsertVersions = config.getPatchInsertOn();
    this.minVersion = initMinVersion(config.getMinVersion());
    this.minVersionFailMessage = config.getMinVersionFailMessage();
    this.skipMigrationRun = config.isSkipMigrationRun();
    this.skipChecksum = config.isSkipChecksum();
    this.basePlatformName = config.getBasePlatform();
    this.platformName = config.getPlatform();
    this.insertSql = MigrationMetaRow.insertSql(sqlTable);
    this.updateSql = MigrationMetaRow.updateSql(sqlTable);
    this.updateChecksumSql = MigrationMetaRow.updateChecksumSql(sqlTable);
    this.scriptTransform = createScriptTransform(config);
    this.envUserName = System.getProperty("user.name");
  }

  private MigrationVersion initMinVersion(String minVersion) {
    return (minVersion == null || minVersion.isEmpty()) ? null : MigrationVersion.parse(minVersion);
  }

  private String sqlPrimaryKey() {
    return "pk_" + table;
  }

  /**
   * Return the number of migrations in the DB migration table.
   */
  int size() {
    return migrations.size();
  }

  /**
   * Returns the versions that are already applied.
   */
  Set<String> versions() {
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
  void createIfNeededAndLock() throws SQLException, IOException {
    SQLException suppressedException = null;
    if (!tableKnownToExist) {
      MigrationSchema.createIfNeeded(config, context.connection());
      if (!tableExists()) {
        try {
          createTable();
        } catch (SQLException e) {
          if (tableExists()) {
            suppressedException = e;
            log.log(INFO, "Ignoring error during table creation, as an other process may have created the table", e);
          } else {
            throw e;
          }
        }
      }
    }
    try {
      obtainLockWithWait();
    } catch (RuntimeException re) {
      // catch "failed to obtain row locks"
      if (suppressedException != null) {
        re.addSuppressed(suppressedException);
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
    platform.lockMigrationTable(sqlTable, context.connection());
  }

  /**
   * Release a lock on the migration table (MySql, MariaDB only).
   */
  void unlockMigrationTable() {
    platform.unlockMigrationTable(sqlTable, context.connection());
  }

  /**
   * Read the migration table with details on what migrations have run.
   * This must execute after we have completed the wait for the lock on
   * the migration table such that it reads any migrations that have
   * executed during the wait for the lock.
   */
  private void readExistingMigrations() throws SQLException {
    for (MigrationMetaRow metaRow : platform.readExistingMigrations(sqlTable, context.connection())) {
      addMigration(metaRow.version(), metaRow);
    }
  }

  void createTable() throws IOException, SQLException {
    Connection connection = context.connection();
    try {
      scriptRunner.runScript(createTableDdl(), "create migration table");
      createInitMetaRow().executeInsert(connection, insertSql);
      connection.commit();
    } catch (SQLException e) {
      connection.rollback();
      throw e;
    }
  }

  /**
   * Return the create table script.
   */
  String createTableDdl() throws IOException {
    String script = ScriptTransform.replace("${table}", sqlTable, createTableScript());
    return ScriptTransform.replace("${pk_table}", sqlPrimaryKey(), script);
  }

  /**
   * Return the create table script.
   */
  private String createTableScript() throws IOException {
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
    Enumeration<URL> resources = classLoader().getResources(location);
    if (resources.hasMoreElements()) {
      URL url = resources.nextElement();
      return IOUtils.readUtf8(url);
    }
    return null;
  }

  private ClassLoader classLoader() {
    return Thread.currentThread().getContextClassLoader();
  }

  /**
   * Return true if the table exists.
   */
  boolean tableExists() throws SQLException {
    Connection connection = context.connection();
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
        log.log(ERROR, "Migration {0} requires prior migration {1} which has not been run", localVersion.version(), prior.version());
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
    int checksum2 = 0;
    if (local instanceof LocalUriMigrationResource) {
      checksum = ((LocalUriMigrationResource)local).checksum();
      checksum2 = patchLegacyChecksums ? AUTO_PATCH_CHECKSUM : 0;
      script = convertScript(local.content());
    } else if (local instanceof LocalDdlMigrationResource) {
      final String content = local.content();
      script = convertScript(content);
      // checksum on original content (NEW) or converted script content (LEGACY)
      checksum = Checksum.calculate(earlyChecksumMode ? content : script);
      checksum2 = patchLegacyChecksums ? Checksum.calculate(script) : 0;
    } else {
      checksum = ((LocalJdbcMigrationResource) local).checksum();
    }

    if (existing == null && patchInsertMigration(local, checksum)) {
      return true;
    }
    if (existing != null && skipMigration(checksum, checksum2, local, existing)) {
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
      log.log(INFO, "Patch migration, insert into history {0}", local.location());
      if (!checkStateOnly) {
        insertIntoHistory(local, checksum, 0);
      }
      return true;
    }
    return false;
  }

  /**
   * Return true if the migration should be skipped.
   */
  boolean skipMigration(int checksum, int checksum2, LocalMigrationResource local, MigrationMetaRow existing) throws SQLException {
    boolean matchChecksum = (existing.checksum() == checksum);
    if (matchChecksum) {
      log.log(TRACE, "skip unchanged migration {0}", local.location());
      return true;

    } else if (patchLegacyChecksums && (existing.checksum() == checksum2 || checksum2 == AUTO_PATCH_CHECKSUM)) {
      if (!checkStateOnly) {
        log.log(INFO, "Auto patch migration, set early mode checksum on {0} to {1,number} from {2,number}", local.location(), checksum, existing.checksum());
        existing.resetChecksum(checksum, context.connection(), updateChecksumSql);
      }
      return true;

    } else if (patchResetChecksum(existing, checksum)) {
      log.log(INFO, "Patch migration, reset checksum on {0} to {1,number} from {2,number}", local.location(), checksum, existing.checksum());
      return true;

    } else if (local.isRepeatable() || skipChecksum) {
      // re-run the migration
      return false;
    } else {
      throw new MigrationException("Checksum mismatch on migration " + local.location());
    }
  }

  /**
   * Return true if the checksum is reset on the existing migration.
   */
  private boolean patchResetChecksum(MigrationMetaRow existing, int newChecksum) throws SQLException {
    if (isResetOnVersion(existing.version())) {
      if (!checkStateOnly) {
        existing.resetChecksum(newChecksum, context.connection(), updateChecksumSql);
      }
      return true;
    } else {
      return false;
    }
  }

  private boolean isResetOnVersion(String version) {
    return patchResetChecksumVersions != null && (patchResetChecksumVersions.contains(version) || patchResetChecksumVersions.contains("*"));
  }

  /**
   * Run a migration script as new migration or update on existing repeatable migration.
   */
  private void executeMigration(LocalMigrationResource local, String script, int checksum, MigrationMetaRow existing) throws SQLException {
    if (checkStateOnly) {
      checkMigrations.add(local);
      // simulate the migration being run such that following migrations also match
      addMigration(local.key(), createMetaRow(local, checksum, 1));
      return;
    }

    long exeMillis = 0;
    try {
      if (skipMigrationRun) {
        log.log(DEBUG, "skip migration {0}", local.location());
      } else {
        exeMillis = executeMigration(local, script);
      }
      if (existing != null) {
        existing.rerun(checksum, exeMillis, envUserName, runOn);
        existing.executeUpdate(context.connection(), updateSql);
      } else {
        insertIntoHistory(local, checksum, exeMillis);
      }
    } catch (SQLException e) {
      if (allowErrorInRepeatable && local.isRepeatableLast()) {
        // log the exception and continue on repeatable migration
        log.log(ERROR, "Continue migration with error executing repeatable migration " + local.version(), e);
      } else {
        throw e;
      }
    }
  }

  private long executeMigration(LocalMigrationResource local, String script) throws SQLException {
    long start = System.currentTimeMillis();
    if (local instanceof LocalJdbcMigrationResource) {
      JdbcMigration migration = ((LocalJdbcMigrationResource) local).migration();
      log.log(INFO, "Executing jdbc migration version: {0} - {1}", local.version(), migration);
      migration.migrate(context);
    } else {
      log.log(DEBUG, "run migration {0}", local.location());
      scriptRunner.runScript(script, "run migration version: " + local.version());
    }
    executionCount++;
    return System.currentTimeMillis() - start;
  }

  private void insertIntoHistory(LocalMigrationResource local, int checksum, long exeMillis) throws SQLException {
    MigrationMetaRow metaRow = createMetaRow(local, checksum, exeMillis);
    metaRow.executeInsert(context.connection(), insertSql);
    addMigration(local.key(), metaRow);
  }

  private MigrationMetaRow createInitMetaRow() {
    final int mode = earlyChecksumMode ? EARLY_MODE_CHECKSUM : LEGACY_MODE_CHECKSUM;
    return new MigrationMetaRow(0, "I", INIT_VER_0, "<init>", mode, envUserName, runOn, 0);
  }

  /**
   * Create the MigrationMetaRow for this migration.
   */
  private MigrationMetaRow createMetaRow(LocalMigrationResource migration, int checksum, long exeMillis) {
    int nextId = 1;
    if (lastMigration != null) {
      nextId = lastMigration.id() + 1;
    }
    String type = migration.type();
    String runVersion = migration.key();
    String comment = migration.comment();
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
      if (metaRow.checksum() == EARLY_MODE_CHECKSUM && !earlyChecksumMode) {
        log.log(DEBUG, "automatically detected earlyChecksumMode");
        earlyChecksumMode = true;
      }
      initMetaRow = metaRow;
      patchLegacyChecksums = earlyChecksumMode && metaRow.checksum() == LEGACY_MODE_CHECKSUM;
      return;
    }
    lastMigration = metaRow;
    if (metaRow.version() == null) {
      throw new IllegalStateException("No runVersion in db migration table row? " + metaRow);
    }

    migrations.put(key, metaRow);
    if (VERSION_TYPE.equals(metaRow.type()) || BOOTINIT_TYPE.equals(metaRow.type())) {
      MigrationVersion rowVersion = MigrationVersion.parse(metaRow.version());
      if (currentVersion == null || rowVersion.compareTo(currentVersion) > 0) {
        currentVersion = rowVersion;
      }
      if (BOOTINIT_TYPE.equals(metaRow.type())) {
        dbInitVersion = rowVersion;
      }
    }
  }

  /**
   * Return true if there are no migrations.
   */
  boolean isEmpty() {
    return migrations.isEmpty();
  }

  /**
   * Run all the migrations in order as needed.
   *
   * @return the migrations that have been run (collected if checkState is true).
   */
  List<MigrationResource> runAll(List<LocalMigrationResource> localVersions) throws SQLException {
    checkMinVersion();
    for (LocalMigrationResource localVersion : localVersions) {
      if (!localVersion.isRepeatable() && dbInitVersion != null && dbInitVersion.compareTo(localVersion.version()) >= 0) {
        log.log(DEBUG, "migration skipped by dbInitVersion {0}", dbInitVersion);
      } else if (!shouldRun(localVersion, priorVersion)) {
        break;
      }
    }
    if (patchLegacyChecksums && !checkStateOnly) {
      // only patch the legacy checksums once
      initMetaRow.resetChecksum(EARLY_MODE_CHECKSUM, context.connection(), updateChecksumSql);
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
  List<MigrationResource> runInit(LocalMigrationResource initVersion, List<LocalMigrationResource> localVersions) throws SQLException {
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
  int runNonTransactional() {
    return scriptRunner.runNonTransactional();
  }

  /**
   * Return the count of migrations that were run.
   */
  int count() {
    return executionCount;
  }

  /**
   * Return the mode being used by this migration run.
   */
  String mode() {
    return !earlyChecksumMode ? "legacy" : (patchLegacyChecksums ? "earlyChecksum with patching" : "earlyChecksum");
  }
}
