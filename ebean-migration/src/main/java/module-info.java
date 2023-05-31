open module io.ebean.migration {

  exports io.ebean.migration;

  requires transitive java.sql;
  requires transitive io.avaje.applog;
  requires transitive io.avaje.classpath.scanner;
  requires transitive io.ebean.ddl.runner;
  requires io.ebean.migration.auto;

  provides io.ebean.migration.auto.AutoMigrationRunner with io.ebean.migration.AutoRunner;
}
