ALTER TABLE class_settings DROP CONSTRAINT IF EXISTS class_settings_ai_provider_chk;
ALTER TABLE class_settings
  ADD CONSTRAINT class_settings_ai_provider_chk
  CHECK (ai_provider IN ('offline', 'cloud', 'openrouter', 'zai', 'ollama', 'lmstudio'));
