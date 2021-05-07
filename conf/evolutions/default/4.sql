# --- !Ups

alter table ac_host add column encryptedSharedSecret varchar(2048);
