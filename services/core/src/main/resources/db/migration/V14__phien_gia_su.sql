-- Only synthetic/demo data until the privacy pilot ADR is approved.
ALTER TABLE assignments ADD CONSTRAINT assignments_tutor_owner UNIQUE(id,student_id,class_id);
CREATE TABLE phien_gia_su (
 id uuid PRIMARY KEY,
 hoc_sinh_id uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
 giao_bai_id uuid NOT NULL,
 lop_id uuid NOT NULL REFERENCES classes(id) ON DELETE CASCADE,
 created_at timestamptz NOT NULL DEFAULT now(),
 updated_at timestamptz NOT NULL DEFAULT now(),
 tom_tat text NOT NULL DEFAULT '',
 summary_den_luot bigint NOT NULL DEFAULT 0,
 context_version text NOT NULL DEFAULT '',
 last_seq bigint NOT NULL DEFAULT 0 CHECK(last_seq>=0),
 revision bigint NOT NULL DEFAULT 0,
 token_count integer NOT NULL DEFAULT 0 CHECK(token_count>=0),
 UNIQUE(hoc_sinh_id,giao_bai_id),
 FOREIGN KEY(giao_bai_id,hoc_sinh_id,lop_id) REFERENCES assignments(id,student_id,class_id) ON DELETE CASCADE
);
-- Each row is one request and its checked answer; role/content lives in the linked message table.
CREATE TABLE yeu_cau_gia_su (
 id uuid PRIMARY KEY,
 phien_id uuid NOT NULL REFERENCES phien_gia_su(id) ON DELETE CASCADE,
 thu_tu bigint NOT NULL,
 request_id uuid NOT NULL,
 ma_buoc varchar(32) NOT NULL REFERENCES step_templates(step_code),
 cap integer NOT NULL CHECK(cap BETWEEN 1 AND 3),
 cau_hoi text NOT NULL CHECK(length(cau_hoi)<=2000),
 tra_loi text NOT NULL DEFAULT '',
 trang_thai varchar(16) NOT NULL DEFAULT 'generating' CHECK(trang_thai IN ('generating','completed','cancelled','error')),
 su_kien jsonb NOT NULL DEFAULT '[]',
 created_at timestamptz NOT NULL DEFAULT now(),
 lease_until timestamptz NOT NULL DEFAULT now()+interval '120 seconds',
 UNIQUE(phien_id,thu_tu), UNIQUE(phien_id,request_id), UNIQUE(id,phien_id)
);
CREATE UNIQUE INDEX yeu_cau_gia_su_mot_writer ON yeu_cau_gia_su(phien_id) WHERE trang_thai='generating';
CREATE TABLE luot_gia_su (
 id uuid PRIMARY KEY,
 phien_id uuid NOT NULL REFERENCES phien_gia_su(id) ON DELETE CASCADE,
 luot_id uuid NOT NULL,
 seq bigint NOT NULL CHECK(seq>0),
 role varchar(10) NOT NULL CHECK(role IN ('user','assistant')),
 content text NOT NULL,
 tokens integer NOT NULL CHECK(tokens>=0),
 created_at timestamptz NOT NULL DEFAULT now(),
 FOREIGN KEY(luot_id,phien_id) REFERENCES yeu_cau_gia_su(id,phien_id) ON DELETE CASCADE,
 UNIQUE(phien_id,seq)
);
CREATE TABLE gia_su_usage (
 id uuid PRIMARY KEY,
 hoc_sinh_id uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
 lop_id uuid NOT NULL REFERENCES classes(id) ON DELETE CASCADE,
 luot_id uuid NOT NULL REFERENCES yeu_cau_gia_su(id) ON DELETE CASCADE,
 provider varchar(32) NOT NULL,
 purpose varchar(20) NOT NULL,
 input_tokens integer NOT NULL CHECK(input_tokens>=0),
 output_tokens integer NOT NULL CHECK(output_tokens>=0),
 estimated boolean NOT NULL,
 created_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX gia_su_usage_lop ON gia_su_usage(lop_id,hoc_sinh_id,created_at);
