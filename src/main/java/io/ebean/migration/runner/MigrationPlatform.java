package io.ebean.migration.runner;

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

  private static final String BASE_SELECT = "select id, mtype, mstatus, mversion, mcomment, mchecksum, run_on, run_by, run_time from ";

  /**
   * Standard row locking for db migration table.
   */
  String forUpdateSuffix = " order by id for update";

  /**
   * Lock the migration table. The base implementation uses row locking but lock table would be preferred when available.
   */
  void lockMigrationTable(String sqlTable, Connection connection) throws SQLException {

    final String selectSql = selectSql(sqlTable);

    try (PreparedStatement query = connection.prepareStatement(selectSql)) {
      try (ResultSet resultSet = query.executeQuery()) {
        while (resultSet.next()) {
          resultSet.getInt(1);
        }
      }
    }
  }

  /**
   * Read the existing migrations from the db migration table.
   */
  List<MigrationMetaRow> readExistingMigrations(String sqlTable, Connection connection) throws SQLException {

    final String selectSql = selectSql(sqlTable);

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
   * Return the SQL to select the existing rows in db migration table with row locking.
   */
  String selectSql(String table) {
    return BASE_SELECT + table + forUpdateSuffix;
  }

  public static class Postgres extends MigrationPlatform {

    @Override
    void lockMigrationTable(String sqlTable, Connection connection) throws SQLException {
      try (PreparedStatement query = connection.prepareStatement("lock table " + sqlTable)) {
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
