# ---!Ups

begin;

-- table declarations :
create table "project" (
    "name" text not null,
    "description" text not null,
    "code" text not null,
    "id" integer primary key not null,
    "allow_request_for_access" boolean not null,
    "project_type" integer not null
  );
create sequence "project_id_seq";
alter table "project" alter column "id" set default nextval('"project_id_seq"');
create table "info" (
    "name" text not null,
    "last_modified" timestamp not null,
    "project_id" integer not null,
    "text" text not null,
    "code" text not null,
    "id" integer primary key not null,
    "children_count" integer not null,
    "parent_info_id" integer,
    "keywords" text not null,
    "is_private" boolean not null
  );
create sequence "info_id_seq";
alter table "info" alter column "id" set default nextval('"info_id_seq"');
create table "info_image" (
    "data" bytea not null,
    "info_id" integer not null,
    "id" bigint primary key not null,
    "content_type" text not null
  );
-- indexes on info_image
create index "idx43ec0714" on "info_image" ("info_id");
create table "info_revision" (
    "name" text not null,
    "revision_date" timestamp not null,
    "project_id" integer not null,
    "text" text not null,
    "code" text not null,
    "info_id" integer not null,
    "id" integer primary key not null,
    "parent_info_id" integer,
    "keywords" text not null,
    "is_private" boolean not null,
    "user_id" integer not null
  );
create sequence "info_revision_id_seq";
alter table "info_revision" alter column "id" set default nextval('"info_revision_id_seq"');
-- indexes on info_revision
create index "idx5ed00880" on "info_revision" ("info_id");
create table "app_user" (
    "email" text,
    "avatar_url" text,
    "provider_user_id" text not null,
    "hasher" text,
    "last_name" text not null,
    "first_name" text not null,
    "provider_id" text not null,
    "id" integer primary key not null,
    "salt" text,
    "password" text
  );
create sequence "app_user_id_seq";
alter table "app_user" alter column "id" set default nextval('"app_user_id_seq"');
create table "token" (
    "is_sign_up" boolean not null,
    "expiration_time" timestamp not null,
    "email" text not null,
    "uuid" text primary key not null,
    "creation_time" timestamp not null
  );
create table "authenticator_holder" (
    "expiration_date" timestamp not null,
    "last_used" timestamp not null,
    "provider_user_id" text not null,
    "provider_id" text not null,
    "id" text primary key not null,
    "creation_date" timestamp not null
  );
create table "user_in_project" (
    "project_id" integer not null,
    "user_status" integer not null,
    "id" integer primary key not null,
    "user_id" integer not null
  );
create sequence "user_in_project_id_seq";
alter table "user_in_project" alter column "id" set default nextval('"user_in_project_id_seq"');
-- foreign key constraints :
alter table "info_image" add constraint "info_imageFK1" foreign key ("info_id") references "info"("id");
alter table "info" add constraint "infoFK2" foreign key ("project_id") references "project"("id");
alter table "info" add constraint "infoFK3" foreign key ("parent_info_id") references "info"("id");
alter table "info_revision" add constraint "info_revisionFK4" foreign key ("info_id") references "info"("id");
alter table "user_in_project" add constraint "user_in_projectFK5" foreign key ("user_id") references "app_user"("id");
alter table "user_in_project" add constraint "user_in_projectFK6" foreign key ("project_id") references "project"("id");
alter table "info_revision" add constraint "info_revisionFK7" foreign key ("user_id") references "app_user"("id");
-- column group indexes :
create unique index "idx1d110f06" on "app_user" ("provider_id","provider_user_id");
create unique index "idxf2d60db4" on "user_in_project" ("user_id","project_id");

commit;