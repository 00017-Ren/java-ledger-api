-- Insert a default admin user so you can log in once the auth endpoints are built.
-- IMPORTANT: Replace the placeholder password_hash with a real BCrypt hash before using this in production.
-- You can generate one with Spring Security's BCryptPasswordEncoder or an online BCrypt generator.
INSERT INTO users (id, email, password_hash, role, created_at, updated_at)
VALUES (
    uuid_generate_v4(),
    'admin@ledger.com',
    '$2a$12$placeholder_replace_me',
    'ADMIN',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);
