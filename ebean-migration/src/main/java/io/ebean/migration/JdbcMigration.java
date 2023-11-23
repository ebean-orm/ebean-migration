package io.ebean.migration;

import java.sql.Connection;

/**
 * Interface to be implemented by Jdbc Java Migrations. By default the migration
 * version and description will be extracted from the class name. The checksum of this migration
 * (for validation) will also be null, unless the migration also implements the
 * MigrationChecksumProvider, in which case it can be returned programmatically.
 * <p>
 * When the JdbcMigration implements ConfigurationAware, the master
 * {@link MigrationConfig} is automatically injected upon creation, which is
 * useful for getting placeholder and schema information.
 *
 * @author Roland Praml, FOCONIS AG
 */
public interface JdbcMigration extends MigrationChecksumProvider {

  /**
   * Execute the migration using the connection.
   */
  void migrate(Connection connection);

  @Override
  default int getChecksum() {
    return 0;
  }

  /**
   * Returns the name of the JdbcMigration. Note, the value must follow the naming conventions, for MigrationVersions.
   * (example: <code>V1_2_1__comment</code>)
   * <p>
   * By default, the simple classname will be returned.
   */
  default String getName() {
    return getClass().getSimpleName();
  }

  /**
   * Determines, if this migration can be used for that platform.
   * <p>
   * By default, <code>true</code> is returned.
   */
  default boolean isForPlatform(String platform) {
    return true;
  }
}
