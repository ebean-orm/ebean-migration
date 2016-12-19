package io.ebean.dbmigration;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Derive well known database platform names from JDBC MetaData.
 */
class DbNameUtil {

  /**
   * Normalise the database product/platform name.
   *
   * At this point only sql server has platform specific handling required (create table and for update).
   */
  static String normalise(Connection connection) {

    try {
      String productName = connection.getMetaData().getDatabaseProductName().toLowerCase();
      if (productName.contains("postgres")) {
        return "postgres";
      } else if (productName.contains("mysql")) {
        return "mysql";
      } else if (productName.contains("oracle")) {
        return "oracle";
      } else if (productName.contains("microsoft")) {
        return "sqlserver";
      } else if (productName.contains("db2")) {
        return "db2";
      } else if (productName.contains("h2")) {
        return "h2";
      } else if (productName.contains("hsql")) {
        return "hsql";
      } else if (productName.contains("sqlite")) {
        return "sqlite";
      } else if (productName.contains("sql anywhere")) {
        return "sqlanywhere";
      }
      return "";

    } catch (SQLException e) {
      return "";
    }
  }
}
