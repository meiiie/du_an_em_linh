# Nguồn của data/v0

Các tệp JSON ở đây chép **nguyên văn** hằng nội dung của v0 trong `apps/web/scripts/seed.ts`, không sửa chữ (#85, T011b). Tệp do
`data/v0/trich-tu-seed.cjs` sinh. Script chạy nguyên đoạn mã nạp nội dung của seed.ts (dòng 519–631) bằng `vm`,
với một `db` giả chỉ ghi lại đối tượng truyền vào `db.insert(<bảng>).values(...)`. Vì vậy tên trường là tên v0 dùng khi
ghi, và giá trị là giá trị lúc chạy (`\\ge` trong mã TS thành `\ge`). Chạy lại: `node data/v0/trich-tu-seed.cjs` từ gốc
repo. Script dừng nếu seed.ts thêm hay bớt trường, hay đổi số lần ghi.

Nguồn: `apps/web/scripts/seed.ts`, commit cuối đổi tệp `4f7a886deb2e414983d8b8e0b19cd5155d1a00fa`, blob `028b8dd42ec05976523a48465936c1d49e05bf60`.

| Tệp | Nội dung | Bảng v0 | Dòng trong seed.ts |
| --- | --- | --- | --- |
| `khung-buoc.json` | Khung 5 bước `B.DH.*` | `stepTemplates` | 519–535 |
| `bkt.json` | Cấu hình BKT, khóa `bkt` | `masteryConfig` | 537–549 |
| `tai-lieu.json` | Ba tài liệu tự soạn của lớp | `documents` | 552–599 |
| `bang-cong-thuc.json` | Bảng công thức: phiên bản, ghi chú, 6 dòng | `formulaSheets`, `formulas` | 602–631 |

Khác với v0:
- Bỏ trường lúc chạy: `id`, `uploadedBy`, `createdAt` của tài liệu; `id`, `classId`, `ownerTeacherId`, `lockedAt` của
  bảng; `id`, `formulaSheetId` của dòng công thức.
- Bỏ `status: "locked"` của bảng: v2 chỉ khóa bảng khi mọi dòng `DAT` qua job `kiem-dong-cong-thuc` (ADR 013, T012).
- Thêm mã ổn định để importer nhận lại khi nạp lần hai (v0 dùng UUID ngẫu nhiên):
  - tài liệu: `v0-don-dieu`, `v0-de-mau`, `v0-phuong-phap`;
  - dòng công thức: `d-1` … `d-6` theo thứ tự trong seed.ts, cùng mã với test `services/math/tests/test_dong_cong_thuc.py`.
- Khóa của tài liệu xếp như bản vá `sp-tai-lieu-0001` của lab Sư phạm, để importer đọc một định dạng cho cả tài liệu của
  v0 và của lab.
