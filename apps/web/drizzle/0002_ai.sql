ALTER TABLE class_settings ADD COLUMN IF NOT EXISTS ai_provider text NOT NULL DEFAULT 'offline';
ALTER TABLE class_settings ADD COLUMN IF NOT EXISTS ai_model text;
ALTER TABLE class_settings ADD COLUMN IF NOT EXISTS ai_allow_local boolean NOT NULL DEFAULT true;
ALTER TABLE class_settings ADD COLUMN IF NOT EXISTS ai_api_key text;

ALTER TABLE class_settings DROP CONSTRAINT IF EXISTS class_settings_ai_provider_chk;
ALTER TABLE class_settings
  ADD CONSTRAINT class_settings_ai_provider_chk
  CHECK (ai_provider IN ('offline', 'cloud', 'ollama', 'lmstudio'));
