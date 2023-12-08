package io.ebean.migration;

import java.sql.Connection;

/**
 * Interface to be implemented by Jdbc Java Migrations. By default the migration
 * version and description will be extracted from the class name. The checksum of this migration
 * will be 0 by default
 * <p>
 * Note: Instances of JdbcMigration should be stateless, as the <code>migrate</code> method may
 * run multiple times in multi-tenant setups.
 * <p>
 * There are several ways, how the JdbcMigrations are found.
 * <ul>
 *   <li><b>ServiceLoader</b> this is the default behaviour<br>
 *   in this case add all your migration class names in META-INF/services/io.ebean.migration.JdbcMigration and/or in your
 *   module info.</li>
 *   <li>Using <b>jdbcMigrations</b> property<br>
 *   you can specify all migrations in the jdbcMigrations property</li>
 *   <li>Using own <b>jdbcMigrationFactory</b<br>
 *   you can write your own jdbcMigrationFactory that provides JdbcMigrations</li>
 * </ul>
 *
 * @author Roland Praml, FOCONIS AG
 */
public interface JdbcMigration extends MigrationChecksumProvider {

  /**
   * Execute the migration using the connection.
   * <p>
   * Note: This API has changed with ebean-migration 13.12, as the initialization has changed.
   * See https://github.com/ebean-orm/ebean-migration/issues/90 for migration advice.
   */
  void migrate(MigrationContext context);

  @Override
  default int getChecksum() {
    return 0;
  }

  /**
   * Returns the name of the JdbcMigration. Note, the name is used to determine the version and comment,
   * that is written to the migration table, so the returned value must be a parseable {@link MigrationVersion}
   * (example: <code>V1_2_1__comment</code>)
   * <p>
   * By default, the simple classname will be returned, so the file name can be used.
   */
  default String getName() {
    return getClass().getSimpleName();
  }

  /**
   * Determines, if this migration can be used for that migrationContext.
   * Here, platform checks or other things can be implemented.
   * You should not write to database at this stage.
   * <p>
   * By default, <code>true</code> is returned.
   */
  default boolean matches(MigrationContext context) {
    return true;
  }
}
