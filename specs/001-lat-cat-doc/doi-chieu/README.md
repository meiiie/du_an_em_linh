# Đối chiếu với v0

Tệp vàng do chính mã của v0 sinh ra, để test của core v2 so khớp: v2 phải cho cùng kết quả với v0 trên cùng đầu vào.

| Tệp | Sinh bởi | Dùng ở |
| --- | --- | --- |
| `cong-phat-hanh-v0.py` | — (script) | Xuất bảng `cong_phat_hanh` của `services/math/app/verify.py` cho test trạng thái tổng của core |
| `xuat-v0.ts` | — (script, T013) | Sinh hai tệp dưới |
| `v0-bai.json` | `xuat-v0.ts` | T014: mỗi bài v0 nạp (mã, dấu vân tay kiểu v0, nguồn, dạng trả lời, trạng thái tổng, trạng thái phát hành, dữ kiện bảo vệ, trạng thái từng tầng, và `cot_v0`: mọi cột nội dung v0 ghi cho bài — kỹ năng, mức, Bloom, độ khó, đề, LaTeX, hàm, bước bắt đầu, lời giải, các cấp gợi ý đã lưu) |
| `phan-hoi-toan.json` | `xuat-v0.ts` | T014: mọi cặp yêu cầu → phản hồi của dịch vụ toán theo thứ tự v0 gọi, để dịch vụ toán giả phát lại |

## `xuat-v0.ts` (T013)

Script chạy **nguyên** mã nạp bài của `apps/web/scripts/seed.ts`: dựng bài từ `data/supham`, gọi `/v1/solve`, `/v1/generate`, `/v1/verify`, rồi ghi bài và lượt kiểm. Không có dòng logic nào của v0 được gõ lại.

- **Cách lấy mã.** Các đoạn được cắt theo mốc trong seed.ts: `MUC4`, `MUC3`, `contentHash`, `math`, `hintText`, `napBai`, và đoạn nạp từ `const steps = [` tới trước `const levels`.
- **Cách chạy.** Kiểu TypeScript được bỏ bằng `stripTypeScriptTypes` của Node. Mã chạy trong `vm`, với một `db` giả chỉ ghi lại `db.insert(<bảng>).values(...)`.
- **Chỗ thay duy nhất:** hằng `corpus`. Ở v0, kho lớp chỉ có một tài liệu. Script thay bằng kho của lớp v2, đọc từ đúng các tệp importer v2 đọc:
  - 5 tài liệu: `data/v0/tai-lieu.json` (3 tài liệu), `data/supham/tai-lieu/sp-tai-lieu-0001.json`, `sp-tai-lieu-0002.json`;
  - 6 dòng của `data/v0/bang-cong-thuc.json`;
  - id là mã ổn định (`v0-don-dieu`, `d-1`…), không phải UUID ngẫu nhiên.
- **Ghi lại để phát lại.** Yêu cầu `verify` trong `phan-hoi-toan.json` bỏ trường `tai_lieu`, `cong_thuc`, vì kho chỉ ghi một lần, dưới dạng danh sách mã và băm, ở `v0-bai.json` → `nguon.kho_lop`.
- **Dừng khi seed.ts đổi.** Script dừng nếu `seed.ts` có thay đổi chưa commit, hoặc thiếu một mốc. `nguon.seed` ghi commit và blob của seed.ts đã chạy.
- **Dừng khi dữ liệu hay chính script đổi.** Script dừng nếu `data/supham`, `data/v0` (bài ví dụ, bài khung ngắn, tài liệu, bảng công thức) hay chính `xuat-v0.ts` có thay đổi chưa commit. `nguon.du_lieu` ghi cây git của hai thư mục và blob của script.
- **Dịch vụ toán do script tự dựng.** Script không lấy dịch vụ toán từ một URL có sẵn, vì không biết nó build từ mã nào (`/health` luôn báo `0.1.0`). Nó tự build ảnh `services/math` từ chính checkout và dừng nếu `services/math` có thay đổi chưa commit. Ngữ cảnh build là `git archive HEAD services/math`, chỉ gồm tệp đã commit, nên tệp bị `.gitignore` (khóa, `.env`) không bao giờ vào ảnh. Ảnh chạy chỉ đọc ở một cổng ngẫu nhiên của 127.0.0.1 và bị xóa khi xong. `nguon.dich_vu_toan` ghi cây git của `services/math` (cố định mã), digest bất biến của ảnh gốc trong `FROM` và phiên bản Python (cố định runtime, vì tag `python:3.12-slim` có thể đổi), và `pip freeze` của ảnh (cố định thư viện, như SymPy).

```bash
# từ gốc repo, Node ≥ 23.6, Docker đang chạy
node specs/001-lat-cat-doc/doi-chieu/xuat-v0.ts
# tách ảnh hưởng của kho: giữ kho một tài liệu của seed.ts, chỉ in trạng thái, không ghi tệp
KHO_LOP=v0 node specs/001-lat-cat-doc/doi-chieu/xuat-v0.ts
```

### Kết quả (2026-10-04, cây `services/math` `2d833dd`, sympy theo `nguon.dich_vu_toan.goi_python`)

- **17 bài:** 14 `DA_PHAT_HANH`, 2 `CHO_GIAO_VIEN_DUYET` (`DH12-01-TH-01`, `DH12-06-VDC-01`: không có hàm, cả ba tầng `KHONG_KIEM_DUOC`), 1 `BI_CHAN` (`DH12-DEMO-CHAN-01`: tầng 1 `SAI`).
- **32 lần gọi dịch vụ toán:** 12 `solve`, 3 `generate`, 17 `verify`.
- **Chạy hai lần**, cả hai tệp giống nhau từng byte.
- **Kho 5 tài liệu so với kho 1 tài liệu của v0** (`KHO_LOP=v0`): cùng trạng thái cho cả 17 bài. Hai tài liệu của lab không đổi phán quyết nào của bộ bài v0.
- **So với CSDL của stack v0 đang chạy:**
  - dấu vân tay `content_hash` khớp 17/17;
  - lượt kiểm mới nhất khớp nguyên văn 15/17;
  - 2 bài còn lại là hai bài `KHONG_KIEM_DUOC` ở trên, đã được giáo viên duyệt trong CSDL đó (2 bản ghi `content_reviews`; v0 ghi đè tầng thành `GV_DUYET`). Phán quyết tự động trước khi duyệt là như nhau.
