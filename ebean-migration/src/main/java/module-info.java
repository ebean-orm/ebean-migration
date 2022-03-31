open module io.ebean.migration {

  exports io.ebean.migration;
  exports io.ebean.migration.runner;

  requires transitive java.sql;
  requires transitive org.slf4j;
  requires transitive io.avaje.classpath.scanner;
  requires transitive io.ebean.ddl.runner;
  requires io.ebean.migration.auto;
  requires static io.avaje.jsr305x;

  provides io.ebean.migration.auto.AutoMigrationRunner with io.ebean.migration.AutoRunner;
}
