# --- !Ups
alter table info add column trash boolean default false;

# --- !Downs
alter table info drop column trash;