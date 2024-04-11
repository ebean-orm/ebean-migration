module io.ebean.migration.db {

  exports io.ebean.migration.db;

  requires transitive java.sql;
  requires transitive io.avaje.applog;
  requires transitive io.avaje.classpath.scanner;
  requires transitive io.ebean.ddl.runner;
  requires static io.ebean.api;
	requires io.ebean.migration;
  uses io.ebean.plugin.Plugin;
  provides io.ebean.plugin.Plugin with io.ebean.migration.db.MigrationPlugin;
}
