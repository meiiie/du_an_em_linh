-- Explicit solution consent, co-authoring review and opt-in push; V14/V15 belong to another lane.
CREATE TABLE solution_reveals (
 submission_id uuid PRIMARY KEY REFERENCES submissions(id) ON DELETE CASCADE,
 revealed_at timestamptz NOT NULL
);
CREATE TABLE content_drafts (
 id uuid PRIMARY KEY,
 class_id uuid NOT NULL REFERENCES classes(id) ON DELETE CASCADE,
 document_id uuid NOT NULL REFERENCES documents(id),
 author_id uuid REFERENCES users(id) ON DELETE SET NULL,
 problem jsonb NOT NULL,
 solution text NOT NULL,
 created_at timestamptz NOT NULL,
 decision varchar(12) CHECK(decision IN ('DA_DUYET','BI_CHAN')),
 reviewer_id uuid REFERENCES users(id) ON DELETE SET NULL,
 review_reason varchar(500),
 reviewed_at timestamptz,
 CHECK ((decision IS NULL)=(reviewed_at IS NULL))
);
CREATE TABLE ai_usage (
 id uuid PRIMARY KEY,
 class_id uuid NOT NULL REFERENCES classes(id) ON DELETE CASCADE,
 purpose varchar(30) NOT NULL,
 provider varchar(60) NOT NULL,
 input_tokens integer CHECK(input_tokens>=0),
 output_tokens integer CHECK(output_tokens>=0),
 created_at timestamptz NOT NULL
);
CREATE TABLE push_subscriptions (
 id uuid PRIMARY KEY,
 student_id uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
 endpoint text NOT NULL UNIQUE,
 p256dh varchar(200) NOT NULL,
 auth varchar(100) NOT NULL,
 created_at timestamptz NOT NULL
);
CREATE TABLE push_deliveries (
 subscription_id uuid NOT NULL REFERENCES push_subscriptions(id) ON DELETE CASCADE,
 reminder_key varchar(250) NOT NULL,
 delivered_at timestamptz NOT NULL,
 PRIMARY KEY(subscription_id,reminder_key)
);
