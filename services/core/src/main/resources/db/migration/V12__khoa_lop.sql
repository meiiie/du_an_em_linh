-- Khóa lớp giữ nguyên ghi danh và lịch sử. Quyền đọc/ghi theo lớp qua ClassMembership đóng khi enabled=false.
ALTER TABLE classes ADD COLUMN enabled boolean NOT NULL DEFAULT true;
