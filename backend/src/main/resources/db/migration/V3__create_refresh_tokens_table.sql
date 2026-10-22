-- Server-side refresh token registry. Only the JWT id (jti) is stored; the token itself is a
-- signed JWT, so a database leak does not yield usable tokens.
CREATE TABLE refresh_tokens (
    id VARCHAR(36) PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMP NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    revoked_at TIMESTAMP
);

CREATE INDEX refresh_tokens_user_idx ON refresh_tokens(user_id);
