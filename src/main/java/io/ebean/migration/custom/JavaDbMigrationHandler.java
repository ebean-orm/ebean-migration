/*
 * Licensed Materials - Property of FOCONIS AG
 * (C) Copyright FOCONIS AG.
 */

package io.ebean.migration.custom;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.function.Function;

import io.ebean.migration.custom.parse.JavaCall;

/**
 * Default implementation for JavaDbMigration. Runs the <code>migrate</code> method on a class instance (e.g. a Spring bean)
 *
 * @author Roland Praml, FOCONIS AG
 *
 */
public class JavaDbMigrationHandler implements CustomCommandHandler {

  private final Function<String, JavaDbMigration> factory;

  public JavaDbMigrationHandler(Function<String, JavaDbMigration> factory) {
    super();
    this.factory = factory;
  }

  @Override
  public void handle(Connection conn, String cmdString) throws SQLException {

    JavaCall call = JavaCall.parse(cmdString);

    JavaDbMigration migration = factory.apply(call.getName());

    if (migration == null) {
      throw new IllegalArgumentException(call.getName() + " no valid JavaDbMigration");
    }
    migration.migrate(conn, call.getArguments());
  }
}
