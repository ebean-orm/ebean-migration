-- apply changes
create table foo (
  id                            integer generated by default as identity not null,
  assoc_one                     varchar(255),
  constraint pk_foo primary key (id)
);

create table bar (
  id                            integer generated by default as identity not null,
  something                     varchar(255),
  constraint pk_bar primary key (id)
);