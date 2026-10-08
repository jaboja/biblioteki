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

-- Create library_account table
CREATE TABLE library_account (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    library VARCHAR(50) NOT NULL,
    username VARCHAR(255) NOT NULL,
    password VARCHAR(255) NOT NULL,
    enabled BOOLEAN DEFAULT TRUE,
    CONSTRAINT fk_library_account_user FOREIGN KEY (user_id) REFERENCES app_users(id),
    CONSTRAINT uk_user_library_username UNIQUE (user_id, library, username)
);
