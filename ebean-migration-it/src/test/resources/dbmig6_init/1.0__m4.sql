create table m1 (id integer, acol varchar(20), addcol varchar(10));

create table m2 (id integer, acol varchar(20), bcol timestamp);

create  table m3 (id integer, acol varchar(20), bcol timestamp);

create table m4 (id integer, acol varchar(20), bcol timestamp);

insert into m3 (id, acol) VALUES (1, 'text with ; sign'); -- plus some comment
