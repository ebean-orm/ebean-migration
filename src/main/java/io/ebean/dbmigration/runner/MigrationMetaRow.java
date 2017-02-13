package io.ebean.dbmigration.runner;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

/**
 * Bean holding migration execution details stored in the migration table.
 */
class MigrationMetaRow {

  private static final String SQLSERVER = "sqlserver";

  private int id;

  private String status;

  private String type;

  private String version;

  private String comment;

  private int checksum;

  private Timestamp runOn;

  private String runBy;

  private long runTime;

  /**
   * Construct for inserting into table.
   */
  MigrationMetaRow(int id, String type, String version, String comment, int checksum, String runBy, Timestamp runOn, long runTime) {
    this.id = id;
    this.type = type;
    this.version = version;
    this.checksum = checksum;
    this.comment = comment;
    this.runBy = runBy;
    this.runOn = runOn;
    this.runTime = runTime;
  }

  /**
   * Construct from the SqlRow (read from table).
   */
  MigrationMetaRow(ResultSet row) throws SQLException {
    id = row.getInt(1);
    type = row.getString(2);
    status = row.getString(3);
    version = row.getString(4);
    comment = row.getString(5);
    checksum = row.getInt(6);
    runOn = row.getTimestamp(7);
    runBy = row.getString(8);
    runTime = row.getLong(9);
  }

  public String toString() {
    return "id:" + id + " type:" + type + " runVersion:" + version + " comment:" + comment + " runOn:" + runOn + " runBy:" + runBy;
  }

  /**
   * Return the id for this migration.
   */
  int getId() {
    return id;
  }

  /**
   * Return the normalised version for this migration.
   */
  String getVersion() {
    return version;
  }

  /**
   * Return the checksum for this migration.
   */
  int getChecksum() {
    return checksum;
  }

  /**
   * Bind to the insert statement.
   */
  void bindInsert(PreparedStatement insert) throws SQLException {
    insert.setInt(1, id);
    insert.setString(2, type);
    insert.setString(3, "SUCCESS");
    insert.setString(4, version);
    insert.setString(5, comment);
    insert.setInt(6, checksum);
    insert.setTimestamp(7, runOn);
    insert.setString(8, runBy);
    insert.setLong(9, runTime);
  }

  /**
   * Return the SQL insert given the table migration meta data is stored in.
   */
  static String selectSql(String catalog, String schema, String table, String platform) {
    if (schema != null && !schema.isEmpty()) {
      table = schema + "." + table;
    }
    if (catalog != null && !catalog.isEmpty()) {
      table = catalog + "." + table;
    }
    String sql = "select id, mtype, mstatus, mversion, mcomment, mchecksum, run_on, run_by, run_time from " + table;
    if (SQLSERVER.equals(platform)) {
      return sql + " with (updlock)";
    } else {
      return sql + " for update";
    }
  }

  /**
   * Return the SQL insert given the table migration meta data is stored in.
   */
  static String insertSql(String catalog, String schema, String table) {
    if (schema != null && !schema.isEmpty()) {
      table = schema + "." + table;
    }
    if (catalog != null && !catalog.isEmpty()) {
      table = catalog + "." + table;
    }
    return "insert into " + table
        + " (id, mtype, mstatus, mversion, mcomment, mchecksum, run_on, run_by, run_time)"
        + " values (?,?,?,?,?,?,?,?,?)";
  }

}
