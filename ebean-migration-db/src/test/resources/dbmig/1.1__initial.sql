create table m1 (id integer, acol varchar(20));
-- Check with DB2:
-- call sysproc.admin_cmd('reorg table m1');
create table m2 (id integer, acol varchar(20), bcol timestamp);

