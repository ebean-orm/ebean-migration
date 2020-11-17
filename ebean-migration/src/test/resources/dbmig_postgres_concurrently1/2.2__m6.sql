create table m6 (id integer, acol varchar(20), bcol timestamp);

drop index concurrently ix_m5_acol;
create index concurrently ix_m5_acol2 on m5 (acol,id);
create index concurrently ix_m6_acol2 on m6 (bcol); -- junk