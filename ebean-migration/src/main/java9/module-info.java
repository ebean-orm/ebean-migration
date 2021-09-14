module io.ebean.migration {

  requires transitive java.sql;
  requires io.ebean.migration.auto;
  requires org.slf4j;

  exports io.ebean.migration;
  exports io.ebean.migration.runner;

  provides io.ebean.migration.auto.AutoMigrationRunner with io.ebean.migration.AutoRunner;
}
