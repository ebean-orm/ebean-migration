create table m1
(
    id   integer,
    acol varchar(20)
);

create table m2
(
    id   integer,
    acol varchar(20),
    bcol timestamp
);


create table orp_master (
id                            varchar(100) not null,
name                          varchar(255),
version                       bigint not null,
constraint pk_orp_master primary key (id)
);

alter table orp_master add column sys_period_start datetime(6) default now();
alter table orp_master add column sys_period_end datetime(6);

create table orp_master_history (
id                            varchar(100) not null,
name                          varchar(255),
version                       bigint not null,
sys_period_start              datetime(6),
sys_period_end                datetime(6)
);

create view orp_master_with_history as
    select * from orp_master union all
    select * from orp_master_history;

delimiter $$
create or replace trigger orp_master_history_upd
  for orp_master
  before update for each row as
    NEW.sys_period_start = greatest(current_timestamp , date_add(OLD.sys_period_start,interval 1 microsecond));
    insert into orp_master_history (sys_period_start,sys_period_end,id, name, version)
    values (OLD.sys_period_start,  NEW.sys_period_start, OLD.id, OLD.name, OLD.version);
  end_trigger
$$
