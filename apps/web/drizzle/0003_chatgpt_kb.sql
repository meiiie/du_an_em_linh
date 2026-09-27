ALTER TABLE class_settings ADD COLUMN IF NOT EXISTS ai_openai_sub text;
ALTER TABLE class_settings ADD COLUMN IF NOT EXISTS ai_openai_email text;
ALTER TABLE class_settings ADD COLUMN IF NOT EXISTS ai_connected_at timestamptz;
