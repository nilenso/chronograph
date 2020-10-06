CREATE TABLE IF NOT EXISTS invites(
       id bigserial primary key,
       organization_id bigint not null references organizations(id),
       email text not null
);
--;;
CREATE UNIQUE INDEX IF NOT EXISTS invites_organization_id_email_idx
ON invites (organization_id, email);
