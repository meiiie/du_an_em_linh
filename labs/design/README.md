# Lab Thiết kế (UI/UX)

**Sứ mệnh:** quyết định học sinh và giáo viên nhìn thấy, chạm vào, hiểu điều gì — dựa trên tham chiếu sản phẩm thật, nguyên lý có nguồn, và thử trên màn 390 px / 1280 px trước khi viết mã.

**Nguồn chuẩn hiện hành:** [`docs/DESIGN.md`](../../docs/DESIGN.md) — hệ «phiếu làm bài» của v0 (mực / giấy, IBM Plex, lưới 8 px, nút 40/44, tâm quang học 46 %). Lab không sửa trực tiếp file đó; lab đề xuất, chủ repo duyệt, rồi mới nâng lên.

## Cấu trúc

```text
design/
  references/   bóc tách sản phẩm tham chiếu: lấy gì, không lấy gì, ảnh chụp (không chép thương hiệu)
  studies/      nghiên cứu thiết kế có ngày: brief → phương án → nguyên mẫu → phê bình → quyết định
  prototypes/   nguyên mẫu HTML tĩnh, mở được trực tiếp trong trình duyệt
  audits/       audit giao diện đang chạy: a11y, chữ, nhịp, lỗi; mỗi phát hiện có ảnh + mức độ
```

## Quy trình một nghiên cứu thiết kế

1. **Brief:** người dùng, việc cần làm, ràng buộc (thiết bị, chữ Việt, KaTeX / MathLive, Angular 22 + `@angular/aria`), thước đo xong.
2. **Tham chiếu:** ít nhất 3 sản phẩm đang chạy cho đúng bài toán, ghi vào `references/`.
3. **Phương án:** 2–3 hướng khác nhau thật sự, mỗi hướng một câu nói rõ đánh đổi.
4. **Nguyên mẫu:** HTML tĩnh ở `prototypes/`, đo ở 390 px và 1280 px, có trạng thái rỗng / lỗi / đang chờ.
5. **Phê bình:** subagent `design-critic` + skill cấp người dùng (`better-interface`, `better-accessibility`, `better-typography`, `apple-design`); WCAG 2.2 AA.
6. **Quyết định:** ghi lựa chọn và lý do trong file nghiên cứu; đề xuất sửa `docs/DESIGN.md` bằng PR riêng.

## Thước chất lượng

- WCAG 2.2 AA; mục tiêu chạm 44 px với `pointer: coarse`; focus thấy được; `prefers-reduced-motion`.
- Chữ Việt: dấu chồng không bị cắt (kiểm `line-height` với «Ừ», «Ỗ», «ặ»); không viết hoa toàn bộ câu dài.
- Công thức: KaTeX cỡ đủ đọc trên 390 px; công thức dài cuộn ngang trong khung, không tràn trang.
- Chữ trên giao diện theo hiến chương V: mỗi chữ một việc, tiếng lớp 12, không thuật ngữ kỹ thuật.

## Đang mở

- [`studies/2026-10-01-brief-v2.md`](studies/2026-10-01-brief-v2.md) — brief cho giao diện v2 trên Angular.
- [`references/README.md`](references/README.md) — danh mục tham chiếu khởi đầu.
- [`audits/2026-10-02-dang-nhap-v2.md`](audits/2026-10-02-dang-nhap-v2.md) — `/dang-nhap` bản Angular so với `docs/DESIGN.md`: số đo 390 / 1280 px, 3 phát hiện mức thấp.
