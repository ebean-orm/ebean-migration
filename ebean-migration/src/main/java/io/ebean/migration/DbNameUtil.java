package io.ebean.migration;

import io.ebean.migration.runner.MigrationPlatform;

import java.sql.*;

import static java.lang.System.Logger.Level.WARNING;

/**
 * Derive well known database platform names from JDBC MetaData.
 */
class DbNameUtil implements DbPlatformNames {

  /**
   * Normalise the database product/platform name.
   * <p>
   * At this point only sql server has platform specific handling required (create table and for update).
   */
  static String normalise(Connection connection) {
    try {
      final String productName = connection.getMetaData().getDatabaseProductName().toLowerCase();
      if (productName.contains(POSTGRES)) {
        return readPostgres(connection);
      } else if (productName.contains(MYSQL)) {
        return MYSQL;
      } else if (productName.contains(MARIADB)) {
        return MARIADB;
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

  private static String readPostgres(Connection connection) {
    // PostgreSQL driver use a non-trustable hardcoded product name.
    // The following block try to retrieve DBMS version to determine
    // if used DBMS is PostgreSQL or Cockroach.
    try (Statement statement = connection.createStatement()) {
      try (ResultSet resultSet = statement.executeQuery("SELECT version()")) {
        if (resultSet.next()) {
          String productVersion = resultSet.getString(1).toLowerCase();
          if (productVersion.contains("cockroach")) {
            return COCKROACH;
          }
        }
      }
    } catch (SQLException e) {
      MigrationRunner.log.log(WARNING, "Error running detection query on Postgres", e);
    }
    return POSTGRES;
  }

  static MigrationPlatform platform(String platformName) {
    switch (platformName) {
      case MYSQL:
      case MARIADB:
        return new MigrationPlatform.MySql();
      case ORACLE:
      case H2:
      case DB2:
        return new MigrationPlatform.LogicalLock();
      case POSTGRES:
        return new MigrationPlatform.Postgres();
      case SQLSERVER:
        return new MigrationPlatform.SqlServer();
      case SQLITE:
        return new MigrationPlatform.NoLocking();
      default:
        return new MigrationPlatform();
    }
  }
}
