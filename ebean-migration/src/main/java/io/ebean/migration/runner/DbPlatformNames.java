package io.ebean.migration.runner;

/**
 * Known database platform names.
 */
interface DbPlatformNames {

  String SQLSERVER = "sqlserver";
  String SQLITE = "sqlite";
  String POSTGRES = "postgres";
  String MARIADB = "mariadb";
  String MYSQL = "mysql";
  String ORACLE = "oracle";
  String DB2 = "db2";
  String H2 = "h2";
  String HSQL = "hsql";
  String SQLANYWHERE = "sqlanywhere";
  String COCKROACH = "cockroach";
}
