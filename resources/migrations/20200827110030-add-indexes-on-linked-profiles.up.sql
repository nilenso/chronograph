CREATE INDEX IF NOT EXISTS linked_profiles_user_id_idx ON linked_profiles (user_id);
--;;
CREATE UNIQUE INDEX IF NOT EXISTS linked_profiles_profile_id_user_id_profile_type_idx ON linked_profiles (profile_id, user_id, profile_type);
