/*
 * Licensed Materials - Property of FOCONIS AG
 * (C) Copyright FOCONIS AG.
 */

package io.ebean.migration.custom;

import java.sql.Connection;
import java.util.List;

/**
 * Provides Java Db Migration capabilities to a class. This can be implemented in Spring Beans e.g.
 *
 * @author Roland Praml, FOCONIS AG
 *
 */
public interface JavaDbMigration {

  void migrate(Connection conn, List<Object> args);
}
