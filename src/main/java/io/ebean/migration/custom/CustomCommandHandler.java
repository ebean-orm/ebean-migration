/*
 * Licensed Materials - Property of FOCONIS AG
 * (C) Copyright FOCONIS AG.
 */

package io.ebean.migration.custom;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * A custom statement handler to handle custom migration code (e.g. calling Java routines).
 *
 * @author Roland Praml, FOCONIS AG
 *
 */
@FunctionalInterface
public interface CustomCommandHandler {

  /**
   * This method is called for each statement that starts with the registered prefix.
   */
  void handle(Connection conn, String cmd) throws SQLException;

}
