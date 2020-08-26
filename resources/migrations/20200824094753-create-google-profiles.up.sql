CREATE TABLE IF NOT EXISTS google_profiles(
    id bigserial primary key,
    google_id text unique not null,
    created_at timestamp with time zone,
    updated_at timestamp with time zone
);
