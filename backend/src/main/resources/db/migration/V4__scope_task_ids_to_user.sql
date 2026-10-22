-- Task ids are client-generated, so the primary key must be scoped to the owner. Rebuild the
-- table (portable across PostgreSQL and H2) instead of dropping a vendor-named constraint.
CREATE TABLE tasks_by_user (
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    id VARCHAR(36) NOT NULL,
    title VARCHAR(240) NOT NULL,
    completed BOOLEAN NOT NULL DEFAULT FALSE,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    updated_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT tasks_user_id_pk PRIMARY KEY (user_id, id)
);

INSERT INTO tasks_by_user (user_id, id, title, completed, deleted, updated_at, created_at)
SELECT user_id, id, title, completed, deleted, updated_at, created_at FROM tasks;

DROP TABLE tasks;

ALTER TABLE tasks_by_user RENAME TO tasks;

CREATE INDEX tasks_user_updated_idx ON tasks(user_id, updated_at);
