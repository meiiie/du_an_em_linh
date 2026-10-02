-- Định danh (#55): người dùng, phiên đăng nhập, refresh token. Migration chỉ thêm, không sửa sau khi merge.
CREATE TABLE users (
    id            uuid         PRIMARY KEY,
    email         varchar(254) NOT NULL UNIQUE,
    password_hash varchar(255) NOT NULL,
    display_name  varchar(120) NOT NULL,
    role          varchar(20)  NOT NULL CHECK (role IN ('ADMIN', 'SCHOOL_ADMIN', 'TEACHER', 'STUDENT')),
    enabled       boolean      NOT NULL DEFAULT true,
    synthetic     boolean      NOT NULL DEFAULT false,
    created_at    timestamptz  NOT NULL,
    updated_at    timestamptz  NOT NULL
);

-- Một lần đăng nhập = một phiên; refresh token xoay vòng vẫn thuộc phiên đó. Đăng xuất thu hồi cả phiên.
CREATE TABLE auth_sessions (
    id         uuid        PRIMARY KEY,
    user_id    uuid        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    created_at timestamptz NOT NULL,
    revoked_at timestamptz
);

CREATE INDEX auth_sessions_user_id_idx ON auth_sessions (user_id);

-- Chỉ lưu băm SHA-256 (hex) của refresh token.
CREATE TABLE refresh_tokens (
    id         uuid        PRIMARY KEY,
    session_id uuid        NOT NULL REFERENCES auth_sessions (id) ON DELETE CASCADE,
    user_id    uuid        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token_hash varchar(64) NOT NULL UNIQUE,
    expires_at timestamptz NOT NULL,
    revoked_at timestamptz,
    created_at timestamptz NOT NULL
);

CREATE INDEX refresh_tokens_user_id_idx ON refresh_tokens (user_id);
CREATE INDEX refresh_tokens_session_id_idx ON refresh_tokens (session_id);
