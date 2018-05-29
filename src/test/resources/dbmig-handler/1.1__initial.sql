create table m1 (id integer, acol varchar(20));

-- call some static methods
runStatic:io.ebean.migration.MigrationRunnerTest.myMigration("Hello\nWorld");

runStatic:io.ebean.migration.MigrationRunnerTest.createTable("m2", "id integer", "acol varchar(20)");

-- have to use delimiter here, because the statement contains semicolons
delimiter $$
runStatic: io.ebean.migration.MigrationRunnerTest.demo(
    "This\tis\ta\tString\targ;", 
    ["ListElem1", "ListElem2", 3, 4, null],
    2012-01-01,
    3.45,
    2016-02-03T12:10:55+01:00
    );
$$

-- do logging
    logger:Hello This is line 15 from the 1.1__initial script;

-- call JavaDbMigration
run: mig1 (42);

