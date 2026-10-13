CREATE FUNCTION workspace_consume_recommendation() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
 UPDATE next_problem_overrides SET consumed_at=NEW.started_at WHERE student_id=NEW.student_id AND problem_id=NEW.problem_id AND consumed_at IS NULL;
 RETURN NEW;
END $$;
CREATE TRIGGER workspace_consume_recommendation AFTER INSERT ON submissions FOR EACH ROW EXECUTE FUNCTION workspace_consume_recommendation();
-- Teacher acknowledgement of an unverified explanation is never a bypass of the runtime tutor gates.
CREATE TABLE tutor_review_queue (
 id uuid PRIMARY KEY,
 class_id uuid NOT NULL REFERENCES classes(id) ON DELETE CASCADE,
 problem_id uuid NOT NULL REFERENCES problems(id) ON DELETE CASCADE,
 draft text NOT NULL CHECK(length(draft)<=10000),
 reason varchar(500) NOT NULL,
 created_at timestamptz NOT NULL,
 decision varchar(10) CHECK(decision IN ('DA_XEM','BI_CHAN')),
 reviewer_id uuid REFERENCES users(id) ON DELETE SET NULL,
 review_reason varchar(500),
 reviewed_at timestamptz,
 UNIQUE(class_id,problem_id,draft),
 CHECK((decision IS NULL)=(reviewed_at IS NULL)),
 CHECK(decision IS NULL OR btrim(review_reason)<>'')
);
