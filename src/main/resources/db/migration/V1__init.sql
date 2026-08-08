-- Initial schema. Column types mirror the JPA entities exactly so that
-- Hibernate's ddl-auto=validate passes against this Flyway-managed schema.

create table users (
    id            uuid                        not null,
    username      varchar(255)                not null unique,
    email         varchar(255)                not null,
    password_hash varchar(255)                not null,
    role          varchar(255)                not null check (role in ('USER', 'ADMIN')),
    enabled       boolean                     not null,
    created_at    timestamp(6) with time zone not null,
    updated_at    timestamp(6) with time zone not null,
    primary key (id)
);

create table videos (
    id                uuid                        not null,
    user_id           uuid                        not null,
    original_filename varchar(255)                not null,
    bucket            varchar(255)                not null,
    object_key        varchar(255)                not null unique,
    thumbnail_key     varchar(255),
    status            varchar(255)                not null check (status in ('UPLOADED', 'PROCESSING', 'READY', 'FAILED')),
    size_bytes        bigint,
    duration_millis   bigint,
    content_type      varchar(255),
    created_at        timestamp(6) with time zone not null,
    updated_at        timestamp(6) with time zone not null,
    primary key (id),
    constraint fk_videos_user foreign key (user_id) references users (id)
);

create table processing_jobs (
    id            uuid                        not null,
    video_id      uuid                        not null,
    type          varchar(255)                not null check (type in ('TRANSCODE', 'GENERATE_THUMBNAIL')),
    status        varchar(255)                not null check (status in ('PENDING', 'RUNNING', 'COMPLETED', 'FAILED')),
    started_at    timestamp(6) with time zone,
    finished_at   timestamp(6) with time zone,
    error_message varchar(2000),
    retry_count   integer                     not null,
    created_at    timestamp(6) with time zone not null,
    primary key (id),
    constraint fk_processing_jobs_video foreign key (video_id) references videos (id)
);

create table workers (
    id             uuid         not null,
    instance_id    varchar(255) not null unique,
    hostname       varchar(255),
    status         smallint check (status between 0 and 3),
    last_heartbeat timestamp(6) with time zone,
    started_at     timestamp(6) with time zone,
    primary key (id)
);
