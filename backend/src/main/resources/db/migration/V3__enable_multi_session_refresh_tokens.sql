-- The original single-session design enforced one active session per user.
-- We moved to a multi-session design: a user may hold many refresh tokens (devices).
ALTER TABLE refresh_tokens DROP CONSTRAINT IF EXISTS refresh_tokens_user_id_key;

-- Keep a NON-unique index so "find all sessions for this user" stays fast
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_user_id ON refresh_tokens(user_id);