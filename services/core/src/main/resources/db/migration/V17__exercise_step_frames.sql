-- Chỉ lưu metadata nhập và dữ kiện đề đáng tin cậy; không nhập dap_an/payload_dung.
CREATE TABLE exercise_step_frames (
    problem_id uuid NOT NULL REFERENCES problems(id) ON DELETE CASCADE,
    content_version integer NOT NULL CHECK (content_version > 0),
    frame_version integer NOT NULL CHECK (frame_version > 0),
    steps jsonb NOT NULL CHECK (jsonb_typeof(steps) = 'array' AND jsonb_array_length(steps) = 5),
    problem_data jsonb NOT NULL CHECK (jsonb_typeof(problem_data) = 'object'),
    PRIMARY KEY (problem_id, content_version)
);

CREATE FUNCTION exercise_step_frames_bat_bien() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF NEW IS DISTINCT FROM OLD THEN
        RAISE EXCEPTION 'Khung chấm theo phiên bản nội dung không được ghi đè' USING ERRCODE = 'check_violation';
    END IF;
    RETURN NEW;
END $$;
CREATE TRIGGER exercise_step_frames_bat_bien BEFORE UPDATE ON exercise_step_frames
    FOR EACH ROW EXECUTE FUNCTION exercise_step_frames_bat_bien();
