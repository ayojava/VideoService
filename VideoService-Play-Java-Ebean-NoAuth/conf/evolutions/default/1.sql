# --- Created by Ebean DDL
# To stop Ebean DDL generation, remove this comment and start using Evolutions

# --- !Ups

create table user_video_rating (
  id                        bigint not null,
  videoId                   bigint not null,
  rating                    double,
  user                      varchar(255),
  constraint pk_user_video_rating primary key (id))
;

create table video (
  id                        bigint not null,
  owner                     varchar(255),
  title                     varchar(255),
  duration                  bigint,
  content_type              varchar(255),
  url                       varchar(255),
  constraint pk_video primary key (id))
;

create sequence user_video_rating_seq;

create sequence video_seq;

alter table user_video_rating add constraint fk_user_video_rating_video_1 foreign key (videoId) references video (id) on delete restrict on update restrict;
create index ix_user_video_rating_video_1 on user_video_rating (videoId);



# --- !Downs

SET REFERENTIAL_INTEGRITY FALSE;

drop table if exists user_video_rating;

drop table if exists video;

SET REFERENTIAL_INTEGRITY TRUE;

drop sequence if exists user_video_rating_seq;

drop sequence if exists video_seq;

