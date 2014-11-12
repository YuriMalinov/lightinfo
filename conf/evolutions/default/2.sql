begin;

alter table info alter column code drop not null;

create unique index "idx529307c5" on "info" ("project_id","code");

commit;