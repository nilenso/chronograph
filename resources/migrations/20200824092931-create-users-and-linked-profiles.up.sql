CREATE TABLE IF NOT EXISTS users(
    id bigserial primary key,
    name text not null,
    email text not null,
    photo_url text,
    created_at timestamp with time zone,
    updated_at timestamp with time zone
);
--;;
CREATE TABLE IF NOT EXISTS linked_profiles(
    id bigserial primary key,
    user_id bigint not null references users(id),
    profile_type text not null,
    profile_id bigint,
    created_at timestamp with time zone,
    updated_at timestamp with time zone
);
