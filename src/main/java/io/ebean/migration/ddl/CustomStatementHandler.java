/*
 * Licensed Materials - Property of FOCONIS AG
 * (C) Copyright FOCONIS AG.
 */

package io.ebean.migration.ddl;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * A custom statement handler to handle custom migration code (e.g. calling Java routines).
 *
 * @author Roland Praml, FOCONIS AG
 *
 */
@FunctionalInterface
public interface CustomStatementHandler {

  /**
   * This method is called for each statement. If it returns true, it signalizes, that it has handeld the command.
   */
  boolean handle(String stmt, Connection c) throws SQLException;

}
