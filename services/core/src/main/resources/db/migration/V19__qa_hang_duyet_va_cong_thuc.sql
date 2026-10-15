-- Duyệt bài làm chỉ ghi nhận giáo viên đã xem; không tự đổi phán quyết CAS.
CREATE TABLE step_reviews (
    grading_id uuid PRIMARY KEY REFERENCES grading_results(id) ON DELETE CASCADE,
    teacher_id uuid REFERENCES users(id) ON DELETE SET NULL,
    decision varchar(10) NOT NULL CHECK (decision IN ('DA_XEM','BI_CHAN')),
    reason varchar(500) NOT NULL CHECK (btrim(reason) <> ''),
    reviewed_at timestamptz NOT NULL
);

-- Chỉ sửa mô tả seed sai trong bảng nháp. Bảng đã khóa là căn cứ bất biến.
UPDATE formulas f SET
    statement = 'Công thức góc nhân đôi: $\cos 2x = 1 - 2\sin^2 x$. Dùng để đổi cos của góc gấp đôi về biểu thức theo sin của góc ban đầu.',
    kind = NULL, tier1_status = NULL, tier2_status = NULL,
    tier1_detail = NULL, tier2_detail = NULL, citation_passage_id = NULL, checked_fingerprint = NULL
FROM formula_sheets s
WHERE s.id=f.formula_sheet_id AND s.status='NHAP' AND f.code='NT-CT-02'
    AND f.title='Góc nhân đôi' AND f.latex=E'\\cos 2x=1-2\\sin^2 x'
    AND f.statement = 'Trên đường tròn đơn vị, hoành độ là cos x, tung độ là sin x, nên tổng hai bình phương bằng 1. Dùng góc chuẩn, sau đó kiểm dấu theo góc phần tư. Phương trình lượng giác phải xét mọi nhánh nghiệm và giới hạn miền trước khi kết luận.';

-- Giữ nhận diện bảng do importer tạo; chỉ đổi ghi chú khớp nguyên dấu vân tay seed cũ.
UPDATE formula_sheets SET note =
    'Bảng chương seed Nguyễn Trãi · dòng 7331b1e834680a59a84766f28f1721664a77882dfe9cd469887358a0f4a0b094 · bảng ' || id
WHERE status='NHAP' AND note =
    'Bảng chương seed Nguyễn Trãi · dòng af33fe621843cb985bf7ef2caf8a406d43f5d645dfca10a5bf239dfbd3ff44db · bảng ' || id;
