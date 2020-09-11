CREATE TABLE join_requests (
  user_id bigint references users(id) not null,
  organization_id bigint references organizations(id) not null,
  accepted_at timestamp with time zone,
  created_at timestamp with time zone not null,
  updated_at timestamp with time zone not null
);
