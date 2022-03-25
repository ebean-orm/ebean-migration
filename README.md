[![Build](https://github.com/ebean-orm/ebean-migration/actions/workflows/build.yml/badge.svg)](https://github.com/ebean-orm/ebean-migration/actions/workflows/build.yml)
[![Maven Central : ebean](https://maven-badges.herokuapp.com/maven-central/io.ebean/ebean-migration/badge.svg)](https://maven-badges.herokuapp.com/maven-central/io.ebean/ebean-migration)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://github.com/ebean-orm/ebean-migration/blob/master/LICENSE)
[![JDK EA](https://github.com/ebean-orm/ebean-migration/actions/workflows/jdk-ea.yml/badge.svg)](https://github.com/ebean-orm/ebean-migration/actions/workflows/jdk-ea.yml)

# Ebean Migration
DB Migration runner (similar to FlywayDB) which can be used standalone or with Ebean (to run DB Migrations on EbeanServer start)

## Example use
To use firstly create a MigrationConfig and set properties, then create and run MigrationRunner.
```java
    MigrationConfig config = new MigrationConfig();
    config.setDbUsername("sa");
    config.setDbPassword("");
    config.setDbUrl("jdbc:h2:mem:db1");

    // or load from Properties
    Properties properties = ...
    config.load(properties);

    // run it ...
    MigrationRunner runner = new MigrationRunner(config);
    runner.run();
```
Then create a run a MigrationRunner. When running the database connection can either be:
- Passing a Connection explicitly
- Creating a connection (by specifying JDBC Driver and URL on MigrationConfig)
- Passing a DataSource (MigrationConfig dbUsername and dbPassword used)

### Run with explicit connection
```java
    Connection connection = ...;

    MigrationRunner runner = new MigrationRunner(config);

    // pass explicit connection
    runner.run(connection);
```

### Run with explicit DataSource
```java
    DataSource dataSource = ...;

    MigrationRunner runner = new MigrationRunner(config);

    // pass a dataSource
    runner.run(dataSource);
```

### Run creating a Connection (via MigrationConfig)
```java
    MigrationRunner runner = new MigrationRunner(config);
    runner.run();
```

## Notes:
MigrationConfig migrationPath is the root path (classpath or filesystem) where the migration scripts are searched for.

```java
    MigrationConfig config = createMigrationConfig();

    // load .sql migration resources from a file system location
    config.setMigrationPath("filesystem:my-directory/dbmigration");
```

DB Migration runner follows the FlywayDB conventions and supports running "Versioned" migrations and "Repeatable" migrations.

```console
dbmigration
dbmigration/1.1__initial.sql
dbmigration/1.2__add_some_stuff.sql
dbmigration/R__create_views_repeatable.sql
```
"Repeatable migrations" start with `R__` and execute if they have not been or if their content has changed (using a Md5 checksum).

"Version migrations" start with `V` (or not) and have a version number (1.2 etc) followed by double underscore `__` and then a comment.

