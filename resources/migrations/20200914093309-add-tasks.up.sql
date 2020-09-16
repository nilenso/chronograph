CREATE TABLE IF NOT EXISTS tasks(
       id bigserial primary key,
       name text not null,
       description text,
       organization_id bigint REFERENCES organizations (id) not null,
       created_at timestamp with time zone,
       updated_at timestamp with time zone,
       archived_at timestamp with time zone
);
