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
    profile_type text not null, -- Prefix for the linked _profiles table: 'google', 'facebook', etc
    profile_id bigint, -- This is linked to the id column in _profiles tables, NOT the platform-specific ID
    created_at timestamp with time zone,
    updated_at timestamp with time zone
);
