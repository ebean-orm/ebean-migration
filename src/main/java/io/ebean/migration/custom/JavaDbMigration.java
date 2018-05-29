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

  /**
   * Executes the migration. You have to do most of the migration in raw JDBC, as ebean is not availabe at this stage.
   *
   * Note:<br/>
   * Numeric arguments are instances of Integer or Double<br/>
   * Date/Time/DateTime arguments are instances of  LocalDate/LocalTime/OffsetDateTime<br/>
   */
  void migrate(Connection conn, List<Object> args);
}
