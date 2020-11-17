create table m5 (id integer, acol varchar(20), bcol timestamp);

create index concurrently ix_m5_acol on m5 (acol);