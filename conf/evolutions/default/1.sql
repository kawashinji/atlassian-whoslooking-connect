# --- Created by Ebean DDL
# To stop Ebean DDL generation, remove this comment and start using Evolutions

# --- !Ups

create table ac_host (
  id                        bigint not null,
  key                       varchar(255) not null,
  public_key                varchar(512) not null,
  base_url                  varchar(512) not null,
  name                      varchar(255),
  description               varchar(255),
  constraint uq_ac_host_key unique (key),
  constraint uq_ac_host_base_url unique (base_url),
  constraint pk_ac_host primary key (id))
;

create sequence ac_host_seq;




# --- !Downs

drop table if exists ac_host cascade;

drop sequence if exists ac_host_seq;

