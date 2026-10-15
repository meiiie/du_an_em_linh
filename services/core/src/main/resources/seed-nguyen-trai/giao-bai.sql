-- Chỉ bài có phán quyết cổng thật và đúng lớp UUID seed; không sửa giao giáo viên đã thay đổi.
INSERT INTO assignments (id, class_id, problem_id, student_id, status, set_name, mode, due_at, assigned_by, assigned_at)
SELECT gen_random_uuid(), c.id, r.problem_id, hs.user_id, 'DA_GIAO',
       CASE WHEN p.code IN (:kiemTra) THEN 'Kiểm tra hàm số — 45 phút'
            WHEN p.code LIKE 'NT-LUYEN%' THEN 'Luyện tập hiện tại · có gia sư'
            ELSE 'Ôn tập ' || lower(topic.name) || ' – tuần 7' END,
       CASE WHEN p.code IN (:kiemTra) THEN 'KIEM_TRA' ELSE 'ON_TAP' END,
       CASE WHEN p.code LIKE 'NT-LUYEN%' THEN CURRENT_TIMESTAMP + interval '7 days'
            ELSE c.created_at + CASE WHEN p.code IN (:kiemTra) THEN interval '12 hours'
            ELSE interval '3 days' + interval '1 day' *
                (SELECT count(*) FROM topics siblings WHERE siblings.grade=c.grade AND siblings.code<topic.code) END END,
       (SELECT gv.user_id FROM enrollments gv JOIN users u ON u.id=gv.user_id
         WHERE gv.class_id=c.id AND gv.role_in_class='TEACHER' AND u.synthetic AND u.enabled AND u.role='TEACHER'
         ORDER BY gv.enrolled_at,gv.user_id LIMIT 1),
       CASE WHEN p.code LIKE 'NT-LUYEN%' THEN CURRENT_TIMESTAMP
            WHEN p.code IN (:kiemTra) THEN c.created_at ELSE c.created_at - interval '21 days' END
FROM classes c
JOIN problem_releases r ON r.class_id=c.id AND r.status='DA_PHAT_HANH'
JOIN problems p ON p.id=r.problem_id AND p.origin='GIAO_VIEN_NGUYEN_TRAI'
JOIN skills sk ON sk.code=p.skill_code
JOIN topics topic ON topic.code=sk.topic_code
JOIN enrollments hs ON hs.class_id=c.id AND hs.role_in_class='STUDENT'
JOIN users u ON u.id=hs.user_id AND u.synthetic AND u.role='STUDENT' AND u.enabled
WHERE c.id=:lop AND c.enabled AND topic.grade=c.grade AND p.code NOT IN (:tuHoc)
 AND NOT EXISTS (SELECT 1 FROM assignments a WHERE a.class_id=c.id AND a.problem_id=p.id AND a.student_id=hs.user_id)
ON CONFLICT (class_id,problem_id,student_id) DO NOTHING
