create table author
(
    id          serial primary key,
    full_name   text  not null,
    created_at  timestamp(6) without time zone not null
);