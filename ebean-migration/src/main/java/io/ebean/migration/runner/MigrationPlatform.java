package io.ebean.migration.runner;

import io.ebean.ddlrunner.DdlDetect;
import io.ebean.migration.MigrationException;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import static java.lang.System.Logger.Level.*;

/**
 * Handle database platform specific locking on db migration table.
 */
@SuppressWarnings({"SqlDialectInspection", "SqlSourceToSinkFlow"})
class MigrationPlatform {

  private static final System.Logger log = MigrationTable.log;

  private static final String BASE_SELECT_ID = "select id from ";
  private static final String BASE_SELECT = "select id, mtype, mversion, mchecksum from ";
  private static final String SELECT_FAST_READ = "select mchecksum, mversion from ";

  /**
   * Standard row locking for db migration table.
   */
  String forUpdateSuffix = " order by id for update";

  /**
   * Return the DdlAutoCommit to use for this platform.
   */
  DdlDetect ddlDetect() {
    return DdlDetect.NONE;
  }

  void unlockMigrationTable(String sqlTable, Connection connection) {
    // do nothing by default for select for update case
  }

  /**
   * Lock the migration table. The base implementation uses row locking but lock table would be preferred when available.
   */
  void lockMigrationTable(String sqlTable, Connection connection) throws SQLException {
    for (int attempt = 0; attempt < 5; attempt++) {
      if (lockRows(sqlTable, connection) > 0) {
        // successfully holding row locks
        return;
      }
      backoff(attempt);
    }
    throw new IllegalStateException("Failed to obtain row locks on migration table due to it being empty?");
  }

  private static void backoff(int attempt) {
    try {
      if (attempt % 100 == 0) {
        log.log(WARNING, "In backoff loop attempting to obtain lock on DBMigration table ...");
      } else {
        log.log(TRACE, "in backoff loop obtaining lock...");
      }
      Thread.sleep(100);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new RuntimeException("Interrupted while trying to obtain lock on migration table", e);
    }
  }

  private int lockRows(String sqlTable, Connection connection) throws SQLException {
    int rowCount = 0;
    try (Statement query = connection.createStatement()) {
      try (ResultSet resultSet = query.executeQuery(sqlSelectForUpdate(sqlTable))) {
        while (resultSet.next()) {
          resultSet.getInt(1);
          rowCount++;
        }
      }
    }
    return rowCount;
  }

  List<MigrationMetaRow> fastReadMigrations(String sqlTable, Connection connection) throws SQLException {
    List<MigrationMetaRow> rows = new ArrayList<>();
    try (Statement query = connection.createStatement()) {
      try (ResultSet resultSet = query.executeQuery(sqlSelectForFastRead(sqlTable))) {
        while (resultSet.next()) {
          rows.add(MigrationMetaRow.fastRead(resultSet));
        }
      }
    } finally {
      if (!connection.getAutoCommit()) {
        connection.rollback();
      }
    }
    return rows;
  }

  /**
   * Read the existing migrations from the db migration table.
   */
  List<MigrationMetaRow> readExistingMigrations(String sqlTable, Connection connection) throws SQLException {
    final String selectSql = sqlSelectForReading(sqlTable);
    List<MigrationMetaRow> rows = new ArrayList<>();
    try (Statement query = connection.createStatement()) {
      try (ResultSet resultSet = query.executeQuery(selectSql)) {
        while (resultSet.next()) {
          rows.add(new MigrationMetaRow(resultSet));
        }
      }
    }
    return rows;
  }

  /**
   * Return the SQL to lock the rows in db migration table with row locking.
   */
  String sqlSelectForUpdate(String table) {
    return BASE_SELECT_ID + table + forUpdateSuffix;
  }

  /**
   * Return the SQL to read the db migration table.
   */
  String sqlSelectForReading(String table) {
    return BASE_SELECT + table + forUpdateSuffix;
  }

  String sqlSelectForFastRead(String table) {
    return SELECT_FAST_READ + table;
  }

  static final class LogicalLock extends MigrationPlatform {

    @Override
    void lockMigrationTable(String sqlTable, Connection connection) throws SQLException {
      int attempts = 0;
      while (!obtainLogicalLock(sqlTable, connection)) {
        backoff(++attempts);
      }
      log.log(TRACE, "obtained logical lock");
    }

    @Override
    void unlockMigrationTable(String sqlTable, Connection connection) {
      try {
        releaseLogicalLock(sqlTable, connection);
        connection.commit();
      } catch (SQLException e) {
        MigrationEngine.rollback(connection);
        throw new MigrationException("Error releasing logical lock for ebean migrations");
      }
    }

    private boolean obtainLogicalLock(String sqlTable, Connection connection) throws SQLException {
      try (PreparedStatement query = connection.prepareStatement("update " + sqlTable + " set mcomment=? where id=? and mcomment=?")) {
        query.setString(1, "locked");
        query.setInt(2, 0);
        query.setString(3, "<init>");
        if (query.executeUpdate() == 1) {
          connection.commit();
          return true;
        } else {
          connection.rollback();
          return false;
        }
      }
    }

    private void releaseLogicalLock(String sqlTable, Connection connection) throws SQLException {
      final String sql = "update " + sqlTable + " set mcomment='<init>' where id=0";
      try (Statement query = connection.createStatement()) {
        if (query.executeUpdate(sql) != 1) {
          log.log(ERROR, "Failed to release logical lock. Please review why [" + sql + "] didn't update the row?");
        } else {
          log.log(TRACE, "released logical lock");
        }
      }
    }
  }

  static final class Postgres extends MigrationPlatform {

    @Override
    DdlDetect ddlDetect() {
      return DdlDetect.POSTGRES;
    }

    @Override
    void lockMigrationTable(String sqlTable, Connection connection) throws SQLException {
      try (Statement query = connection.createStatement()) {
        query.executeUpdate("lock table " + sqlTable);
      }
    }
  }

  /**
   * MySql and MariaDB need to use named locks due to implicit commits with DDL.
   */
  static final class MySql extends MigrationPlatform {

    @Override
    void lockMigrationTable(String sqlTable, Connection connection) throws SQLException {
      int attempts = 0;
      while (!obtainNamedLock(connection)) {
        backoff(++attempts);
      }
    }

    private boolean obtainNamedLock(Connection connection) throws SQLException {
      String hash = Integer.toHexString(connection.getMetaData().getURL().hashCode());
      try (Statement query = connection.createStatement()) {
        try (ResultSet resultSet = query.executeQuery("select get_lock('ebean_migration-" + hash + "', 10)")) {
          if (resultSet.next()) {
            return resultSet.getInt(1) == 1;
          }
        }
      }
      return false;
    }

    @Override
    void unlockMigrationTable(String sqlTable, Connection connection) {
      try {
        String hash = Integer.toHexString(connection.getMetaData().getURL().hashCode());
        try (Statement query = connection.createStatement()) {
          query.execute("select release_lock('ebean_migration-" + hash + "')");
        }
      } catch (SQLException e) {
        throw new MigrationException("Error releasing lock for ebean_migration", e);
      }
    }
  }

  static final class SqlServer extends MigrationPlatform {

    SqlServer() {
      this.forUpdateSuffix = " with (updlock) order by id";
    }
  }

  static final class NoLocking extends MigrationPlatform {

    NoLocking() {
      this.forUpdateSuffix = " order by id";
    }

    @Override
    void lockMigrationTable(String sqlTable, Connection connection) {
      // do nothing
    }
  }
}
