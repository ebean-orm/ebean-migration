create table ${table} (
  id                           integer not null,
  mchecksum                    integer not null,
  mtype                        varchar(1) not null,
  mversion                     varchar(150) not null,
  mcomment                     varchar(150) not null,
  mstatus                      varchar(10) not null,
  run_on                       datetime2 not null,
  run_by                       varchar(30) not null,
  run_time                     integer not null,
  constraint ${pk_table} primary key (id)
);

