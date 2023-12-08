package io.ebean.migration;

import java.sql.Connection;

/**
 * @author Roland Praml, FOCONIS AG
 */
public interface MigrationContext {
  Connection connection();

  String migrationPath();

  String platform();

}
