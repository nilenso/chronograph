DROP TABLE IF EXISTS linked_profiles;
--;;
ALTER TABLE users ADD COLUMN google_profiles_id bigint REFERENCES google_profiles(id);
