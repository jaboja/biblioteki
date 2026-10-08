-- Flyway migration: Create initial tables for User and LibraryAccount entities

-- Create app_users table
CREATE TABLE app_users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    full_name VARCHAR(255),
    enabled BOOLEAN DEFAULT TRUE,
    roles VARCHAR(255) DEFAULT 'USER'
);

-- Create library_account table (without user_id initially, will be added in V2)
CREATE TABLE library_account (
    id BIGSERIAL PRIMARY KEY,
    library VARCHAR(50) NOT NULL,
    username VARCHAR(255) NOT NULL,
    password VARCHAR(255) NOT NULL,
    enabled BOOLEAN DEFAULT TRUE
);

-- Create unique constraint for library_account
ALTER TABLE library_account ADD CONSTRAINT uk_library_username UNIQUE (library, username);