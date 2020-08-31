CREATE TYPE user_role AS ENUM ('admin', 'member');
--;;
CREATE TABLE IF NOT EXISTS acls (
  user_id bigint references users(id) not null,
  organization_id bigint references organizations(id) not null,
  role user_role not null,
  created_at timestamp with time zone not null,
  updated_at timestamp with time zone not null
);
--;;
CREATE UNIQUE INDEX IF NOT EXISTS acls_user_id_organization_id_idx
ON acls (user_id, organization_id);
--;;
CREATE INDEX IF NOT EXISTS acls_organization_id_idx
ON acls (organization_id);
