package io.ebean.migration;

/**
 * Migration implementors that also implement this interface will be able to
 * specify their checksum (for validation), instead of having it automatically
 * computed or default to null (for Java Migrations).
 *
 * @author Roland Praml, FOCONIS AG
 */
public interface MigrationChecksumProvider {

  /**
   * Return the checksum for the given migration.
   */
  int getChecksum();
}
