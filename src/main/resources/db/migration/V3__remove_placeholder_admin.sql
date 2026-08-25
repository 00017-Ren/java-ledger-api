-- V2 inserted an unusable BCrypt placeholder, not a valid admin credential.
-- Match all known placeholder fields so no legitimate user can be removed.
DELETE FROM users
WHERE email = 'admin@ledger.com'
  AND password_hash = '$2a$12$placeholder_replace_me'
  AND role = 'ADMIN';
