ALTER TABLE users DROP COLUMN google_profiles_id;
--;;
CREATE TABLE IF NOT EXISTS linked_profiles(
    id bigserial primary key,
    user_id bigint not null references users(id),
    profile_type text not null,
    profile_id bigint,
    created_at timestamp with time zone,
    updated_at timestamp with time zone
);
