-- Chỉ bài có phán quyết cổng thật và đúng lớp UUID seed; không sửa giao giáo viên đã thay đổi.
INSERT INTO assignments (id, class_id, problem_id, student_id, status, set_name, mode, due_at, assigned_by, assigned_at)
SELECT gen_random_uuid(), c.id, r.problem_id, hs.user_id, 'DA_GIAO',
       CASE WHEN p.code='NT-DH12-NB-02' THEN 'Kiểm tra hàm số — 45 phút'
            WHEN p.level4 IN ('NHAN_BIET','THONG_HIEU') THEN 'Củng cố đơn điệu và cực trị'
            WHEN p.level4='VAN_DUNG' THEN 'Luyện tập hàm số'
            ELSE 'Chuyên đề hàm số nâng cao' END,
       CASE WHEN p.code='NT-DH12-NB-02' THEN 'KIEM_TRA' ELSE 'ON_TAP' END,
       now() + CASE WHEN p.code='NT-DH12-NB-02' THEN interval '12 hours' ELSE interval '7 days' END,
       (SELECT gv.user_id FROM enrollments gv JOIN users u ON u.id=gv.user_id
         WHERE gv.class_id=c.id AND gv.role_in_class='TEACHER' AND u.synthetic AND u.enabled AND u.role='TEACHER'
         ORDER BY gv.enrolled_at,gv.user_id LIMIT 1),
       CASE WHEN p.code='NT-DH12-NB-02' THEN now() ELSE c.created_at - interval '21 days' END
FROM classes c
JOIN problem_releases r ON r.class_id=c.id AND r.status='DA_PHAT_HANH'
JOIN problems p ON p.id=r.problem_id AND p.origin='GIAO_VIEN_NGUYEN_TRAI' AND p.answer_form='TU_LUAN_5_BUOC'
JOIN enrollments hs ON hs.class_id=c.id AND hs.role_in_class='STUDENT'
JOIN users u ON u.id=hs.user_id AND u.synthetic AND u.role='STUDENT' AND u.enabled
WHERE c.id=:lop AND c.enabled
 AND NOT EXISTS (SELECT 1 FROM assignments a WHERE a.class_id=c.id AND a.problem_id=p.id AND a.student_id=hs.user_id)
ON CONFLICT (class_id,problem_id,student_id) DO NOTHING
