-- Flyway migration: Add user_id column to library_account table
-- This migration adds the user_id foreign key to link library accounts to users

ALTER TABLE library_account ADD COLUMN user_id BIGINT NOT NULL;

ALTER TABLE library_account 
ADD CONSTRAINT fk_library_account_user 
FOREIGN KEY (user_id) REFERENCES app_users(id);

-- Update unique constraint to include user_id
ALTER TABLE library_account DROP CONSTRAINT IF EXISTS uk_library_username;
ALTER TABLE library_account ADD CONSTRAINT uk_user_library_username UNIQUE (user_id, library, username);

-- For existing data, assign to a default user (user with id=1)
-- This assumes there's at least one user in the system
-- In production, you should back up your data and assign accounts to appropriate users
UPDATE library_account SET user_id = 1 WHERE user_id IS NULL;

ALTER TABLE library_account ALTER COLUMN user_id SET NOT NULL;
