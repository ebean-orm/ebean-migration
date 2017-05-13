package io.ebean.dbmigration.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Utility for closing raw Jdbc resources.
 */
public class JdbcClose {

  private static final Logger logger = LoggerFactory.getLogger(JdbcClose.class);

  /**
   * Close the connection logging if an error occurs.
   */
  public static void close(Connection connection) {
    try {
      connection.close();
    } catch (SQLException e) {
      logger.warn("Error closing connection", e);
    }
  }

  /**
   * Rollback the connection logging if an error occurs.
   */
  public static void rollback(Connection connection) {
    try {
      connection.rollback();
    } catch (SQLException e) {
      logger.warn("Error on connection rollback", e);
    }
  }

  public static void close(Statement query) {
    try {
      query.close();
    } catch (SQLException e) {
      logger.warn("Error closing PreparedStatement", e);
    }
  }

  public static void close(ResultSet resultSet) {
    try {
      resultSet.close();
    } catch (SQLException e) {
      logger.warn("Error closing resultSet", e);
    }
  }
}
