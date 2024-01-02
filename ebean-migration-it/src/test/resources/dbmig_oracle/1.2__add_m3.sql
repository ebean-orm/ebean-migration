create table m3 (id integer, acol varchar(20));

alter table m1 add addcol varchar(10);

insert into m3 (id, acol) VALUES (1, 'text with ; sign'); -- plus some comment
