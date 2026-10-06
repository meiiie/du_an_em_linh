-- Giao bài thử cho profile dev (GiaoBaiThuSeeder, chạy sau khi nhập nội dung theo lớp).
-- Mọi bài đang phát hành ở lớp «12A1 thử» (tên và năm học như LopThuSeeder) được giao cho từng học sinh tổng hợp của lớp,
-- bộ «Đơn điệu và cực trị», hạn 7 ngày, người giao là giáo viên tổng hợp ghi danh sớm nhất của lớp.
-- Chỉ tài khoản tổng hợp (users.synthetic): học sinh thật không được giao, giáo viên thật không đứng tên người giao.
-- Chạy lại không đổi gì: dòng đã có, kể cả dòng giáo viên đã hủy, giữ nguyên hạn, lúc giao, người giao.
-- Trigger của V7 kiểm lại lúc ghi: học sinh của lớp, bài DA_PHAT_HANH ở lớp.
INSERT INTO assignments (id, class_id, problem_id, student_id, status, set_name, due_at, assigned_by, assigned_at)
SELECT gen_random_uuid(), c.id, r.problem_id, hs.user_id, 'DA_GIAO', 'Đơn điệu và cực trị', now() + interval '7 days',
       (SELECT gv.user_id
          FROM enrollments gv
          JOIN users ugv ON ugv.id = gv.user_id AND ugv.synthetic
         WHERE gv.class_id = c.id AND gv.role_in_class = 'TEACHER'
         ORDER BY gv.enrolled_at, gv.user_id
         LIMIT 1),
       now()
  FROM classes c
  JOIN problem_releases r ON r.class_id = c.id AND r.status = 'DA_PHAT_HANH'
  JOIN enrollments hs ON hs.class_id = c.id AND hs.role_in_class = 'STUDENT'
  JOIN users uhs ON uhs.id = hs.user_id AND uhs.synthetic
 WHERE c.name = '12A1 thử' AND c.school_year = '2026-2027'
ON CONFLICT (class_id, problem_id, student_id) DO NOTHING
