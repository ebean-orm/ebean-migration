package io.ebean.migration.runner;


import io.ebean.ddlrunner.DdlDetect;

import javax.annotation.Nonnull;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Handle database platform specific locking on db migration table.
 */
public class MigrationPlatform {

  private static final String BASE_SELECT_ID = "select id from ";
  private static final String BASE_SELECT_ALL = "select id, mtype, mstatus, mversion, mcomment, mchecksum, run_on, run_by, run_time from ";

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

  void unlockMigrationTable(String sqlTable, Connection connection) throws SQLException {
    // do nothing by default for select for update case
  }

  /**
   * Lock the migration table. The base implementation uses row locking but lock table would be preferred when available.
   */
  void lockMigrationTable(String sqlTable, Connection connection) throws SQLException {
    for (int i = 0; i < 5; i++) {
      if (lockRows(sqlTable, connection) > 0) {
        return;
      }
      backoff();
    }
    throw new IllegalStateException("Failed to obtain row locks on migration table due to it being empty?");
  }

  private static void backoff() {
    try {
      Thread.sleep(100);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new RuntimeException("Interrupted while trying to obtain lock on migration table", e);
    }
  }

  private int lockRows(String sqlTable, Connection connection) throws SQLException {
    int rowCount = 0;
    final String selectSql = sqlSelectForUpdate(sqlTable);
    try (PreparedStatement query = connection.prepareStatement(selectSql)) {
      try (ResultSet resultSet = query.executeQuery()) {
        while (resultSet.next()) {
          resultSet.getInt(1);
          rowCount++;
        }
      }
    }
    return rowCount;
  }

  /**
   * Read the existing migrations from the db migration table.
   */
  @Nonnull
  List<MigrationMetaRow> readExistingMigrations(String sqlTable, Connection connection) throws SQLException {
    final String selectSql = sqlSelectForReading(sqlTable);
    List<MigrationMetaRow> rows = new ArrayList<>();
    try (PreparedStatement query = connection.prepareStatement(selectSql)) {
      try (ResultSet resultSet = query.executeQuery()) {
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
  @Nonnull
  String sqlSelectForUpdate(String table) {
    return BASE_SELECT_ID + table + forUpdateSuffix;
  }

  /**
   * Return the SQL to read the db migration table.
   */
  @Nonnull
  String sqlSelectForReading(String table) {
    return BASE_SELECT_ALL + table + forUpdateSuffix;
  }

  public static class Postgres extends MigrationPlatform {

    @Override
    DdlDetect ddlDetect() {
      return DdlDetect.POSTGRES;
    }

    @Override
    void lockMigrationTable(String sqlTable, Connection connection) throws SQLException {
      try (PreparedStatement query = connection.prepareStatement("lock table " + sqlTable)) {
        query.execute();
      }
    }
  }

  /**
   * MySql and MariaDB need to use named locks due to implicit commits with DDL.
   */
  public static class MySql extends MigrationPlatform {

    @Override
    void lockMigrationTable(String sqlTable, Connection connection) throws SQLException {
      while (!obtainNamedLock(connection)) {
        backoff();
      }
    }

    private boolean obtainNamedLock(Connection connection) throws SQLException {
      try (PreparedStatement query = connection.prepareStatement("select get_lock('ebean_migration', 10)")) {
        try (ResultSet resultSet = query.executeQuery()) {
          if (resultSet.next()) {
            return resultSet.getInt(1) == 1;
          }
        }
      }
      return false;
    }

    @Override
    void unlockMigrationTable(String sqlTable, Connection connection) throws SQLException {
      try (PreparedStatement query = connection.prepareStatement("select release_lock('ebean_migration')")) {
        query.execute();
      }
    }
  }

  public static class SqlServer extends MigrationPlatform {

    public SqlServer() {
      this.forUpdateSuffix = " with (updlock) order by id";
    }
  }

  public static class NoLocking extends MigrationPlatform {

    public NoLocking() {
      this.forUpdateSuffix = " order by id";
    }

    @Override
    void lockMigrationTable(String sqlTable, Connection connection) {
      // do nothing
    }
  }
}
