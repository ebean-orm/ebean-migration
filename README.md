[![Build](https://github.com/ebean-orm/ebean-migration/actions/workflows/build.yml/badge.svg)](https://github.com/ebean-orm/ebean-migration/actions/workflows/build.yml)
[![Maven Central : ebean](https://maven-badges.herokuapp.com/maven-central/io.ebean/ebean-migration/badge.svg)](https://maven-badges.herokuapp.com/maven-central/io.ebean/ebean-migration)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://github.com/ebean-orm/ebean-migration/blob/master/LICENSE)
[![JDK EA](https://github.com/ebean-orm/ebean-migration/actions/workflows/jdk-ea.yml/badge.svg)](https://github.com/ebean-orm/ebean-migration/actions/workflows/jdk-ea.yml)
[![native image build](https://github.com/ebean-orm/ebean-migration/actions/workflows/native-image.yml/badge.svg)](https://github.com/ebean-orm/ebean-migration/actions/workflows/native-image.yml)

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


### Run from ebean with JDBC migrations

Migration supports two modes how to run automatically on ebean server start:

- Run as AutoRunner (controlled by `ebean.migration.run = true`)
- Run as Ebean plugin (controlled by `ebean.migration.plugin.run = true`)

To run as plugin, you need the additional dependency to `ebean-migration-db`.

In the AutoRunner mode, you only have a `MigrationContext` that provides access to the current connection.
When the plugin is used, you have a `MigrationContextDb` in your JDBC migrations and you can access the current transaction
and the ebean server. This is useful to make queries with the ebean server:
```java
    public void migrate(MigrationContext context) throws SQLException {
      Database db = ((MigrationContextDb) context).database();
      db.findDto(...)
    }
```


**Important**:
- do not use `DB.getDefault()` at this stage
- do not create new transactions (or committing existing one)
- be aware that the DB layout may not match to your entities (use `db.findDto` instead of `db.find`)

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

