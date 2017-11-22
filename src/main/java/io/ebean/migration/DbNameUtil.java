package io.ebean.migration;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Derive well known database platform names from JDBC MetaData.
 */
class DbNameUtil implements DbPlatformNames {

  /**
   * Normalise the database product/platform name.
   *
   * At this point only sql server has platform specific handling required (create table and for update).
   */
  static String normalise(Connection connection) {

    try {
      String productName = connection.getMetaData().getDatabaseProductName().toLowerCase();
      if (productName.contains(POSTGRES)) {
        return POSTGRES;
      } else if (productName.contains(MYSQL)) {
        return MYSQL;
      } else if (productName.contains(ORACLE)) {
        return ORACLE;
      } else if (productName.contains("microsoft")) {
        return SQLSERVER;
      } else if (productName.contains(DB2)) {
        return DB2;
      } else if (productName.contains(H2)) {
        return H2;
      } else if (productName.contains(HSQL)) {
        return HSQL;
      } else if (productName.contains(SQLITE)) {
        return SQLITE;
      } else if (productName.contains("sql anywhere")) {
        return SQLANYWHERE;
      }
      return "";

    } catch (SQLException e) {
      return "";
    }
  }
}
