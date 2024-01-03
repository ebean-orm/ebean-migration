package io.ebean.migration;

import java.sql.Connection;

/**
 * The current context while a migration runs.
 * <p>
 * This is used to provide meta-informations in JDBC migrations and mainly provides a read-only access
 * to a subset of MigrationConfig.
 * <p>
 * It is possible to provide an extended implementation in <code>MigrationEngine.run(context)</code>,
 * which is accessible in JdbcMigration. So you can create a EbeanMigrationContext, so that you can
 * access the current ebean server in the JDBC migration.
 *
 * @author Roland Praml, FOCONIS AG
 */
public interface MigrationContext {
  /**
   * The current connection. Note: During migration, this connection is always the same.
   * You must not close this connection!
   */
  Connection connection();

  /**
   * The migration path of SQL migrations. You can use this, to load additional SQL resources
   * in your JDBC migration or determine, if this JDBC migration is for a particular path.
   * This can be used if you have multiple ebean servers for different databases.
   */
  String migrationPath();

  /**
   * The platform of the current migration run. (e.g. <code>sqlserver17</code>)
   */
  String platform();

  /**
   * The base platform of the current migration run. (e.g. <code>sqlserver</code>)
   */
  String basePlatform();

}
