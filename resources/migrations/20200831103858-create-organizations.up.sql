CREATE TABLE IF NOT EXISTS organizations (
  id bigserial primary key,
  name text not null,
  slug text not null unique,
  created_at timestamp with time zone not null,
  updated_at timestamp with time zone not null
);
