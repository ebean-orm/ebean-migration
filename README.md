# avaje-dbmigration
DB Migration runner (similar to FlywayDB) which can be used standalone or with Ebean (run migrations on EbeanServer start)

## Example use
To use firstly create a MigrationConfig and set properties (or load then from Properties).
```java

    MigrationConfig config = new MigrationConfig();
    config.setDbUsername("sa");
    config.setDbPassword("");
    config.setDbDriver("org.h2.Driver");
    config.setDbUrl("jdbc:h2:mem:db1");
    
    // or load from Properties
    Properties properties = ...
    
    config.load(properties);

```
Then create an run a MigrationRunner. When running the database connection can either be:
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

    DataSource dataSource = ...;
    
    MigrationRunner runner = new MigrationRunner(config);
    
    // pass a dataSource
    runner.run(dataSource);
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

