create table users (
    id uuid primary key,
    email varchar(255) not null unique,
    username varchar(100) not null unique,
    first_name varchar(100) not null,
    last_name varchar(100) not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null
);
