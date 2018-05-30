package io.ebean.migration;

import java.sql.Connection;

/**
 * Interface to be implemented by Jdbc Java Migrations. By default the migration
 * version and description will be extracted from the class name. The checksum of this migration
 * (for validation) will also be null, unless the migration also implements the
 * MigrationChecksumProvider, in which case it can be returned programmatically.
 *
 * When the JdbcMigration implements ConfigurationAware, the master
 * {@link MigrationConfig} is automatically injected upon creation, which is
 * especially useful for getting placeholder and schema information.
 *
 * @author Roland Praml, FOCONIS AG
 *
 */
public interface JdbcMigration {
  void migrate(Connection connection);
}
