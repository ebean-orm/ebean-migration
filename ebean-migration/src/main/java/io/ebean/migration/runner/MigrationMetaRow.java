package io.ebean.migration.runner;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

/**
 * Bean holding migration execution details stored in the migration table.
 */
@SuppressWarnings("SqlSourceToSinkFlow")
final class MigrationMetaRow {

  private int id;
  private String type;
  private final String version;
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
    version = row.getString(3);
    checksum = row.getInt(4);
  }

  private MigrationMetaRow(int checksum, String version) {
    this.checksum = checksum;
    this.version = version;
  }

  static MigrationMetaRow fastRead(ResultSet row) throws SQLException {
    final var checksum = row.getInt(1);
    final var version = row.getString(2);
    return new MigrationMetaRow(checksum, version);
  }

  @Override
  public String toString() {
    return "id:" + id + " type:" + type + " checksum:" + checksum + " version:" + version;
  }

  /**
   * Return the id for this migration.
   */
  int id() {
    return id;
  }

  /**
   * Return the normalised version for this migration.
   */
  String version() {
    return version;
  }

  /**
   * Return the checksum for this migration.
   */
  int checksum() {
    return checksum;
  }

  String type() {
    return type;
  }

  /**
   * Bind to the insert statement.
   */
  private void bindInsert(PreparedStatement insert) throws SQLException {
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
   * Bind to the insert statement.
   */
  private void bindUpdate(PreparedStatement update) throws SQLException {
    update.setInt(1, checksum);
    update.setTimestamp(2, runOn);
    update.setString(3, runBy);
    update.setLong(4, runTime);
    update.setInt(5, id);
  }

  /**
   * Return the SQL insert given the table migration meta data is stored in.
   */
  static String insertSql(String table) {
    return "insert into " + table
      + " (id, mtype, mstatus, mversion, mcomment, mchecksum, run_on, run_by, run_time) values (?,?,?,?,?,?,?,?,?)";
  }

  /**
   * Return the SQL insert given the table migration meta data is stored in.
   */
  static String updateSql(String table) {
    return "update " + table
      + " set mchecksum=?, run_on=?, run_by=?, run_time=? where id = ?";
  }

  /**
   * Return the SQL update for resetting the checksum of an existing migration.
   */
  static String updateChecksumSql(String table) {
    return "update " + table + " set mchecksum=? where id = ?";
  }

  void rerun(int checksum, long exeMillis, String envUserName, Timestamp runOn) {
    this.checksum = checksum;
    this.runTime = exeMillis;
    this.runBy = envUserName;
    this.runOn = runOn;
  }

  void executeUpdate(Connection connection, String updateSql) throws SQLException {
    try (PreparedStatement statement = connection.prepareStatement(updateSql)) {
      bindUpdate(statement);
      statement.executeUpdate();
    }
  }

  void executeInsert(Connection connection, String insertSql) throws SQLException {
    try (PreparedStatement statement = connection.prepareStatement(insertSql)) {
      bindInsert(statement);
      statement.executeUpdate();
    }
  }

  void resetChecksum(int newChecksum, Connection connection, String updateChecksumSql) throws SQLException {
    try (PreparedStatement statement = connection.prepareStatement(updateChecksumSql)) {
      statement.setInt(1, newChecksum);
      statement.setInt(2, id);
      statement.executeUpdate();
    }
  }
}
