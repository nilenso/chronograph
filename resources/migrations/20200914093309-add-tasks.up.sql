CREATE TABLE tasks(
       id bigserial primary key,
       name text not null,
       description text,
       organization_id bigint REFERENCES organizations (id) not null,
       created_at timestamp with time zone not null,
       updated_at timestamp with time zone not null,
       archived_at timestamp with time zone
);
