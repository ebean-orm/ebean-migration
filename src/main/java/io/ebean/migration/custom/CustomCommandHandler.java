package io.ebean.migration.custom;

import java.sql.Connection;
import java.sql.SQLException;

import io.ebean.migration.MigrationConfig;
import io.ebean.migration.ddl.DdlRunner;

/**
 * A custom statement handler to handle custom migration code (e.g. calling Java routines).
 *
 * You must register the handlers in the {@link MigrationConfig} or {@link DdlRunner}.
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
