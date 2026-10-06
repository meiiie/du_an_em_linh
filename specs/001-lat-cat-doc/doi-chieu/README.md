# Đối chiếu với v0

Tệp vàng do chính mã của v0 sinh ra, để test của core v2 so khớp: v2 phải cho cùng kết quả với v0 trên cùng đầu vào.

| Tệp | Sinh bởi | Dùng ở |
| --- | --- | --- |
| `cong-phat-hanh-v0.py` | — (script) | Xuất bảng `cong_phat_hanh` của `services/math/app/verify.py` cho test trạng thái tổng của core |
| `xuat-v0.ts` | — (script, T013) | Sinh hai tệp dưới |
| `v0-bai.json` | `xuat-v0.ts` | T014: mỗi bài v0 nạp (mã, dấu vân tay kiểu v0, nguồn, dạng trả lời, trạng thái tổng, trạng thái phát hành, dữ kiện bảo vệ, trạng thái từng tầng, và `cot_v0`: mọi cột nội dung v0 ghi cho bài — kỹ năng, mức, Bloom, độ khó, đề, LaTeX, hàm, bước bắt đầu, lời giải, đáp án cuối, các cấp gợi ý đã lưu) |
| `phan-hoi-toan.json` | `xuat-v0.ts` | T014: mọi cặp yêu cầu → phản hồi của dịch vụ toán theo thứ tự v0 gọi, để dịch vụ toán giả phát lại |
| `khoa-bang-v0.py` | — (script) | Sinh tệp dưới |
| `loi-giai-v0.ts` | — (script, T020) | Chạy nguyên `loiGiaiHocSinh` của `apps/web/lib/loi-giai.ts` trên 11 ca, ghi `services/core/src/test/resources/content/loi-giai-v0.json` (kèm blob của tệp v0); `VietLoiGiaiTest` so từng chữ. Chạy: `node specs/001-lat-cat-doc/doi-chieu/loi-giai-v0.ts` |
| `chung.ts` | — (mô-đun) | Phần dùng chung của `xuat-v0.ts` và `cham-v0.ts`: dựng dịch vụ toán từ checkout, cắt mã v0 theo mốc, ghi nguồn git |
| `cham-v0.ts` | — (script, T023) | Sinh tệp dưới |
| `cham-v0.json` | `cham-v0.ts` | T023 (SC-006): `DoiChieuChamV0Test` cho 187 học sinh tổng hợp nộp từng bước trên 12 bài chấm được như lúc ghi; so yêu cầu `/v1/grade` (cây và byte Jackson đem băm), kết quả cho học sinh và hàng `grading_results` với v0 |
| `bkt-v0.ts` | — (script, T052) | Sinh tệp dưới |
| `bkt-v0.json` | `bkt-v0.ts` | T052 (research R7): `DoiChieuBktV0Test` cho 14 học sinh tổng hợp nộp 295 bài qua cổng `CapNhatMucHieu` thật, so thay đổi mức trả về, `mastery_states`, `mastery_events`, cảnh báo kẹt với v0 sau từng lần nộp |
| `khoa-bang-v0.json` | `khoa-bang-v0.py` | T014: phản hồi thật của job khóa bảng (`kiem_dong_cong_thuc`) cho 6 dòng của v0 và 5 tài liệu của lớp, gửi như importer v2 gửi (thứ tự tài liệu, NFC, mỗi câu một đoạn); id ổn định: mã tài liệu, «mã#vị trí» của đoạn. Job giả của test đổi sang id thật rồi phát lại; test so loại, hai tầng, trích dẫn chính và trích dẫn thêm đã ghi của từng dòng |

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

`khoa-bang-v0.py` dừng nếu `services/math`, `data/v0`, `data/supham` hay chính script có thay đổi chưa commit, và ghi cây git của ba thư mục cùng blob của script vào `nguon`.

**Tệp vàng phải đi cùng cây nguồn.** `TepVangDoiChieuTest` của core so cây git ghi trong `v0-bai.json` (`nguon.dich_vu_toan.cay_git`, `nguon.du_lieu`) và `khoa-bang-v0.json` (`nguon`) với checkout, và đỏ khi các thư mục đó có thay đổi chưa commit. Đổi `services/math`, `data/v0`, `data/supham` hay script sinh thì sinh lại tệp vàng trong cùng PR. Job Core của CI chạy cả khi chỉ `services/math` đổi (`scripts/ci-thay-doi.mjs`). Chạy bằng Python của môi trường `services/math` (cùng SymPy):

```bash
services/math/.venv/Scripts/python specs/001-lat-cat-doc/doi-chieu/khoa-bang-v0.py   # Linux, macOS: .venv/bin/python
```

Kết quả (2026-10-05, cây `services/math` `2d833dd`): 6 dòng `DAT` hai tầng; d-1…d-3 `DANG_THUC` trích `sp-tai-lieu-0001`, d-4 `DINH_LI` trích `v0-don-dieu`, d-5 trích `v0-don-dieu` và thêm `sp-tai-lieu-0002`, d-6 trích `sp-tai-lieu-0002`, khớp `services/math/tests/test_dong_cong_thuc.py`.

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

## `cham-v0.ts` (T023)

Script chạy **nguyên** mã chấm từng bước của v0 trên toàn ngân hàng, với dịch vụ toán build từ checkout (`chung.ts`, như `xuat-v0.ts`). Không dòng nào của v0 dựng payload, đọc kết quả chấm hay ghi `grading_results` được gõ lại.

- **Cách lấy mã.** Cắt theo mốc, bỏ kiểu, chạy trong `vm`:
  - `components/solve-client.tsx`: `deHoiCucTri`, `ORDER`, và thân `SolveClient` từ chữ ký tới trước `const badStep =` (mọi hook, `payload()`, `submit()`), dưới một React tối thiểu: `useState`, `useRef` giữ ô theo thứ tự gọi như React, `useEffect` bỏ qua (không đụng `localStorage`).
  - `lib/actions/hs.ts`: `ORDER_BUOC`, `laDauU`, `nopBuoc`. Quyền, CSDL, mức hiểu, gợi ý bài kế, gia sư là giả lập; định danh tự do nào thiếu giả lập thì `ReferenceError`.
  - `app/hs/luyen/[id]/page.tsx`: điều kiện cho làm bài.
  - `lib/math.ts` (`mathJob`) và `lib/levels.ts` (`BUOC`): import thật.
- **Học sinh tổng hợp.** Gõ lời giải mẫu (`cot_v0.baiLam` của `v0-bai.json`) vào state qua setter của chính `SolveClient`, ở dạng màn hình nhận: TXĐ `\mathbb{R}`, `\mathbb{R}\setminus\{1\}`; đạo hàm `3x^{2}-12x+9` (như e2e); mỗi nghiệm một dòng `x = -1`; mốc, dấu `+ - 0 ||`, chiều `TANG GIAM` như nút của bảng; ô kết luận `(-∞;-1) và (3;+∞)`, `x = -1, y = 7` (như `ket_luan_o_nen` của kiemdinh).
- **Biến thể.** 21 dòng `BIEN_THE`, mỗi dòng một hàm thuần trên state, phủ `DAT`, `SAI`, `KHONG_KIEM_DUOC` ở cả năm bước (bảng dưới). Không sửa dữ liệu lab. Biến thể bỏ qua, kèm lý do, khi đụng bước đề cho, khi không áp dụng cho bài (không có nghiệm để bỏ, TXĐ là ℝ, ít hơn hai mốc, đề không hỏi cực trị, mỗi ô đơn điệu chỉ một khoảng), hay khi không đổi bài làm.
- **Chính sách học sinh.** Mỗi kịch bản một phiên `SolveClient` mới. Mỗi vòng: gõ các ô của bước đang mở (sai nếu bước thuộc biến thể và chưa sửa), rồi bấm «Kiểm tra» (`submit()` thật). `DAT` thì v0 tự sang bước kế. Không đạt lần đầu: biến thể `di_tiep` bấm sang bước kế một lần, như học sinh bỏ qua chỗ đỏ; không thì sửa mọi bước sai và quay về bước sai sớm nhất bằng thanh bước. Sửa rồi vẫn không đạt, hay lời giải mẫu không đạt, thì dừng sinh. Mỗi kịch bản đi tới khi xong bài. Bước nào gõ cũng được nộp ngay, nên các bước core lưu luôn trùng bộ nhớ máy học sinh của v0.
- **Tệp vàng.** `kich_ban`: mỗi (bài, biến thể) một dòng. Dòng chạy có `trang_thai` (ô học sinh gõ: `dung`, và `sai` cho các bước của biến thể) và `lan_nop` (`bam`, `nop_toi`, `ket_qua`, `buoc_sau`: bước `submit()` chuyển tới). Dòng bỏ qua có `bo_qua`. `lan_cham`: mỗi yêu cầu khác nhau một bản ghi, khóa là SHA-256 của thân `/v1/grade` đúng từng byte v0 gửi. Bản ghi gồm `yeu_cau`, `phan_hoi`, `tra_ve` (giá trị `nopBuoc` trả, bỏ các khóa do giả lập quyết định: `sub_id`, `tiep_theo`, `loi_giai`, `nghi_doan_mo`, `de_xuat_gui_gv`, `buoc_de_xuat`), `ghi` (hàng `gradingResults` v0 ghi, bỏ `id`, `submissionId`). `khong_cham`: bài cổng trang v0 không cho làm.
- **Bất biến lúc sinh.** Lời giải mẫu `DAT` mọi bước và xong bài ở mọi bài chấm được. Mỗi bước có đủ `DAT`, `SAI`, `KHONG_KIEM_DUOC`. Mỗi lần nộp gọi máy chấm đúng một lần, và thân chấm trùng payload máy học sinh. Không hai lần nộp trùng yêu cầu trong một kịch bản. Phong bì lỗi của sandbox hay lỗi HTTP là lỗi sinh, không ghi. Tệp không quá 2 MB.
- **Nguồn.** `nguon.git` ghi blob hay cây HEAD của 9 nguồn: năm tệp v0 ở trên, `v0-bai.json`, `cham-v0.ts`, `chung.ts`, `services/math`. Script dừng nếu nguồn nào có thay đổi chưa commit. `TepVangDoiChieuTest` so từng khóa với checkout. `scripts/ci-thay-doi.mjs` bật job Core khi một tệp v0 trong đó đổi.

```bash
# từ gốc repo, Node ≥ 23.6, Docker đang chạy; các nguồn phải đã commit
node specs/001-lat-cat-doc/doi-chieu/cham-v0.ts
# thử bảng biến thể trước khi commit: không kiểm nguồn, ghi ra tệp khác
THU=/tmp/cham-v0.json node specs/001-lat-cat-doc/doi-chieu/cham-v0.ts
```

### Kết quả (2026-10-06, cây `services/math` `2d833dd`, `cham-v0.ts` `8ce6215`)

- **12 bài chấm được.** 5 bài cổng v0 không cho làm: `DH12-01-TH-01`, `DH12-06-VDC-01` (chờ duyệt), `DH12-03-NB-02`, `DH12-05-NB-02` (trắc nghiệm), `DH12-DEMO-CHAN-01` (bị chặn).
- **21 biến thể, 187 kịch bản chạy, 65 bỏ qua có lý do.** 955 lần nộp, 221 yêu cầu khác nhau. Tệp 1 523 853 byte, sinh trong 297 s.
- **Chạy hai lần** (hai lần build ảnh): phần ngoài `nguon` giống từng byte.

| Bước | `DAT` | `SAI` | `KHONG_KIEM_DUOC` |
| --- | --- | --- | --- |
| `B.DH.TXD` | 110 | 6 | 6 |
| `B.DH.DAOHAM` | 110 | 6 | 6 |
| `B.DH.NGHIEM` | 167 | 40 | 10 |
| `B.DH.XETDAU` | 187 | 64 | 12 |
| `B.DH.KETLUAN` | 187 | 32 | 12 |

Kết quả không đạt của từng biến thể (số bài chạy):

| Biến thể | Kết quả đo |
| --- | --- |
| `dung` | `DAT` mọi bước, xong bài (12) |
| `txd_sai`, `txd_trong` | TXD `SAI_TXD` ERR.DH.02; TXD `KHONG_KIEM_DUOC` (6, 6) |
| `dao_ham_hai_dong`, `dao_ham_trong` | ĐẠO HÀM `SAI_BIEN_DOI` ERR.DH.26; `KHONG_KIEM_DUOC` (6, 6) |
| `thieu_nghiem`, `thua_nghiem`, `nghiem_chu` | NGHIỆM `DIEM_THIEU` ERR.DH.03; `DIEM_THUA` ERR.DH.21; `KHONG_KIEM_DUOC` (9, 10, 10) |
| `diem_ngoai_txd` | NGHIỆM `DIEM_THUA` ERR.DH.24 (2) |
| `thua_diem_xuyen_buoc` | NGHIỆM `DIEM_THUA` ERR.DH.21, rồi XÉT DẤU `DIEM_THUA` ERR.DH.24 có `dong_lien_quan` (10) |
| `thieu_diem_xuyen_buoc` | NGHIỆM `DIEM_THIEU`, rồi XÉT DẤU `DIEM_THIEU` ERR.DH.03 kèm ô hệ quả (9) |
| `doi_dau`, `o_dau_trong` | XÉT DẤU `SAI_DAU`; `SAI_GIA_TRI` (ô trống), cả hai ERR.DH.06 (12, 12) |
| `dao_moc`, `thieu_moc` | XÉT DẤU `SAI_THU_TU_MOC` ERR.DH.30 (9); `DIEM_THIEU` ERR.DH.03 (11) hay ERR.DH.02 (1) |
| `doc_hai` | XÉT DẤU `KHONG_KIEM_DUOC` (12) |
| `dao_db_nb`, `gop_U` | KẾT LUẬN `SAI_KET_LUAN` ERR.DH.08 (12); ERR.DH.07, luật dấu U (10) |
| `cuc_tri_trong`, `chi_tung_do` | KẾT LUẬN `SAI_KET_LUAN` ERR.DH.12; ERR.DH.11 (5, 5) |
| `ket_luan_chu` | KẾT LUẬN `KHONG_KIEM_DUOC` (12) |

### Giới hạn (phán quyết độc lập #144)

- **`oSai` so với mô hình của `lineBad`, không với mã v0 chạy thật.** Đoạn cắt thân `SolveClient` dừng ở `const badStep =`, nên `vanDe`, `hien`, `lineBad` của v0 không chạy. Kỳ vọng `oSai` trong `DoiChieuChamV0Test` là quy tắc `lineBad` chép tay, cùng dạng với quy tắc trong `DocKetQuaCham.choHocSinh`: lỗi giống nhau ở cả hai thì test không thấy. v0 đóng băng nên rủi ro thấp; nâng lên thì kéo đoạn cắt qua `lineBad` và ghi tập ô tô cho từng lần chấm.
- **Byte được chứng minh là byte đem băm.** Test so cây yêu cầu và SHA-256 của byte Jackson (cùng bộ ghi `YeuCauCham` dùng để băm) với `bam` của v0; byte thật trên đường truyền đi qua bộ chuyển của RestClient (`MathServiceClient`) chưa có test riêng.
- **Kiểm «bước đã gõ mà chưa nộp» trong script** so thân yêu cầu với `payload()` của cùng một lần vẽ, nên không bắt được ca đó; chặn thật là phép so byte bên Java.
- Các cột chỉ core có (`unfinished`, `normalizer_version`, `normalization`, `step_code`) được so với chính yêu cầu và phản hồi đã ghi: là kiểm ghi đọc của core, không phải so với v0.
- Hai chốt của quy tắc dòng liên quan (vấn đề có `nguyen_nhan`, kết quả không `SAI`) chỉ có test đơn vị (`DocKetQuaChamTest`): trong tệp vàng, cả 10 lần chấm có `dong_lien_quan` đều là vấn đề gốc `SAI`.

## `bkt-v0.ts` (T052)

Script chạy **nguyên** mã mức hiểu của v0 trên kịch bản dựng sẵn. Không dòng nào của BKT, mức sau bài, chọn kỹ năng, đếm kẹt hay cảnh báo được gõ lại.

- **Cách lấy mã.** Cắt theo mốc, bỏ kiểu, chạy trong `vm`: `DEFAULT_CFG`, `loadConfig`, `bktNext`, `clamp`, `applyMastery`, `IDX`, `mucSauBai` của `lib/learning.ts`; `laDauU`, cờ `finished` và điều kiện gọi `applyMastery` trong `nopBuoc` của `lib/actions/hs.ts`. `MUC4`, `mucFromMastery` của `lib/levels.ts` là import thật.
- **CSDL giả.** Chỉ các lệnh `applyMastery` và `loadConfig` dùng. `mastery_config` là dòng seed.ts ghi (`data/v0/bkt.json`); `error_types`, `step_templates` như seed.ts nạp từ `data/supham/ma-loi-DH.csv` (kỹ năng chính) và `data/v0/khung-buoc.json`. Cột `real` của v0 (`mastery`, `delta`, `doTinCay`) làm tròn về float4 rồi đọc lại bằng chữ số ngắn nhất, như PostgreSQL và postgres.js: lượt sau tính trên đúng số v0 đọc lại.
- **Kịch bản.** Mỗi kịch bản một học sinh mới nộp lần lượt các bài (kỹ năng, mức, phán quyết của bước kết luận, cờ nghi đoán mò), có bước «giáo viên xử lý cảnh báo». 8 kịch bản viết tay: lên từng nấc tới Vận dụng cao (SC-010), bài dễ hơn không đẩy lên, kẹt và cảnh báo (không ghi trùng khi đang mở, ghi lại sau khi xử lý), chọn kỹ năng theo mã lỗi (kể cả đúng ngưỡng 0,65), theo bước, mã lạ, bước ngoài khung, mã rỗng, không tính (`KHONG_KIEM_DUOC`, lỗi trình bày dấu U), nghi đoán mò, 8 mã lỗi cuối, tụt mức. 6 dãy 40 lượt giả ngẫu nhiên (mulberry32, hạt cố định).
- **Tệp vàng.** `cau_hinh` (dòng `bkt`), `danh_muc` (mã lỗi và bước → kỹ năng), `dem` (số lần nộp, được tính, cảnh báo, đổi mức), `kich_ban`: mỗi lần nộp có đầu vào, `su_kien` (dòng `masteryEvents` v0 ghi, hay `null`), `muc_truoc`, `canh_bao` vừa ghi, `trang_thai` (mọi dòng `masteryStates` của em sau lần nộp).
- **Nguồn.** `nguon.git` ghi blob HEAD của 8 nguồn (ba tệp v0, ba tệp dữ liệu, script, `chung.ts`); script dừng nếu nguồn nào có thay đổi chưa commit. `TepVangDoiChieuTest` so với checkout; `scripts/ci-thay-doi.mjs` bật job Core khi `learning.ts` đổi.

```bash
# từ gốc repo, Node ≥ 23.6; các nguồn phải đã commit
node specs/001-lat-cat-doc/doi-chieu/bkt-v0.ts
# thử kịch bản trước khi commit: không kiểm nguồn, ghi ra tệp khác
THU=/tmp/bkt-v0.json node specs/001-lat-cat-doc/doi-chieu/bkt-v0.ts
```

### Kết quả (2026-10-06)

- **14 kịch bản, 295 lần nộp:** 274 được tính (153 theo kỹ năng của bài, 57 theo mã lỗi, 49 theo bước, 15 nghi đoán mò), 21 không tính. 17 cảnh báo kẹt, 78 lần đổi mức. Tệp 458 543 byte.
- **Chạy hai lần:** giống từng byte.
- **Core:** `DoiChieuBktV0Test` khớp mọi lần nộp: thay đổi mức trả về, mọi dòng trạng thái (mastery tới từng bit sau khi đọc lại cột real, mức, số lượt, kẹt, mã lỗi), dòng sự kiện, cảnh báo. Gửi lại cùng bài làm trả y hệt, không ghi gì thêm.

### Khác v0 có chủ đích

- **Lúc tính.** v0 gọi `applyMastery` ở mỗi lần nộp bước, kể cả bước sai giữa bài. v2 tính một lần khi nộp bài, với phán quyết của bước kết luận làm căn cứ (cổng `CapNhatMucHieu` của practice), nên `DAT` luôn là xong bài. Kịch bản vì thế luôn nộp tới `B.DH.KETLUAN`.
- **Cảnh báo trùng.** v0 không ghi `KET` khi em đang có bất kỳ cảnh báo mở nào ở kỹ năng đó (kể cả «Gửi thầy cô»); cổng `CanhBaoGiaoVien` của v2 chỉ chặn cảnh báo mở cùng loại (#81). Kịch bản chỉ có `KET`, nên hai bên trùng.
- **Làm tròn khi ghi.** v0 gửi số thực dạng chữ thập phân ngắn nhất, PostgreSQL làm tròn chữ đó về float4; script và core làm tròn chính số thực về float4. Hai cách chỉ có thể khác khi số thực nằm sát điểm giữa hai số float4 (làm tròn kép); chưa đo trên CSDL của v0.
