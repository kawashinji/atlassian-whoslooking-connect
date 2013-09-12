# --- !Ups

alter table ac_host rename column base_url to baseurl;

alter table ac_host rename column public_key to publickey;

# --- !Downs

alter table ac_host rename column baseurl to base_url;

alter table ac_host rename column publickey to public_key;
