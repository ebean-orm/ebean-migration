create table baz (
  id                            integer generated by default as identity not null,
  something                     varchar(255),
  constraint pk_baz primary key (id)
);
