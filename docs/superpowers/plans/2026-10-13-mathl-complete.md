# MathL+ — hoàn thiện giao diện và vòng học tập

> Thực hiện liên tục trong working tree theo yêu cầu chủ repo. Không hỏi lại, không commit, không push. Áp dụng planning, parallel agents và verification; yêu cầu trực tiếp này thay các cổng xin duyệt của skill.

**Mục tiêu:** gỡ v0, hiện thực U0–U15 trên Angular + Spring Boot + dịch vụ toán hiện có.
**Kiến trúc:** giữ module ba tầng và cổng liên module; frontend chỉ hiển thị và điều phối tương tác; server kiểm quyền, chế độ bài, hạn nộp và an toàn gia sư. Chỉ thêm Flyway migration.
**Nguồn:** `/workspace/du_an_em_linh-notes/UX_REFERENCE_PLAN.md`, `mathl-frontend-ui.md`, `REQUIREMENTS_ANALYSIS.md`; tham khảo mẫu tương tác LMS và motion transitions.dev, giữ token MathL+.

## Ràng buộc chung

- Không đọc/ghi bí mật, `.env`, `.env.local`, PEM; không thay dữ liệu sư phạm và kiểm định.
- Không bóng, inset shadow, viền nhấn trái, dải màu mép, khối nổi; focus bằng outline.
- Tiếng Việt đầy đủ dấu; một hành động chính; 390px có bottom nav và safe-area.
- Không gia sư đưa đáp án; kiểm tra không chat bị khóa phía server; lọc CAS lỗi thì đóng.
- Không sửa luật ArchUnit để né lỗi. Mỗi gói Java mới có NullMarked.
- Các vùng độc lập được phân công song song; mọi hợp đồng API phải đồng bộ trước kiểm cuối.

## Task 1: Gỡ v0 và đồng bộ vận hành (root)

- [ ] Đọc package/workspace/compose/render/CI, phân loại mọi tham chiếu v0 có thực thi.
- [ ] Gỡ `apps/web` cùng script, cấu hình, image và job chỉ v0; giữ dữ liệu import `data/v0`.
- [ ] Thay deploy bằng frontend/core/math, đồng bộ lockfile, script phiên bản/phân loại CI và test.
- [ ] Kiểm script, YAML, manifest; build frontend và Maven verify sau tích hợp.

## Task 2: U0–U1 và tài liệu (agent flat_docs)

- [ ] Viết test hành vi scanner với box-shadow, inset, border-left/inline-start, token và utility, có ca sạch.
- [ ] Thêm `scripts/check-flat.mjs`, kiểm CSS và template inline ở `apps/frontend/src`.
- [ ] Cập nhật quy trình Codex viết mã có lead review, DESIGN/AGENTS/lab theo luật phẳng.
- [ ] Ghi mặc định trong ASSUMPTIONS, nguồn/giấy phép motion và giới hạn thực tế.
- [ ] Chạy test scanner; root nối lint và CI; frontend agent sửa CSS.

## Task 3: U5–U14 phía core (agent core)

- [ ] Công bố hợp đồng API/DTO để frontend hiện thực cùng lúc.
- [ ] Test use case quyền lớp, tutor xin đáp án/khóa kiểm tra/fail-closed, giao bài/hạn, CSV và admin.
- [ ] Hiện thực REST dashboard/progress/override, assignments, tutor SSE, planner/reminders, library/formula/bank/review, users/classes.
- [ ] Persistence thật qua adapter; migration mới; nhật ký cho thao tác quản trị/duyệt/ghi đè.
- [ ] Maven verify gồm ArchUnit; báo rõ test CSDL bị bỏ qua nếu thiếu Docker.

## Task 4: U2–U15 phía frontend (agent frontend)

- [ ] Đọc mẫu tham chiếu và giấy phép; viết lại mẫu phù hợp Angular.
- [ ] Dialog/sheet focus trap, toast gộp trùng 3/4/5 giây, states/stat card, nav vai trò.
- [ ] Trang Học mastery/+1, bài lọc chế độ, tutor desktop/mobile, kết quả, lịch, kho và flashcard.
- [ ] Màn GV dashboard/progress/bank/upload/formulas/review/assign/settings; admin users/classes.
- [ ] Route/dialog/list/state motion ngắn, reduced-motion; sửa mọi vi phạm flat và tương phản.
- [ ] Build + Vitest, bổ sung e2e vòng học và axe; ảnh responsive khi runtime cho phép.

## Tập trung rà soát

1. Người dùng lớp khác không đọc/ghi nội dung, giao bài hoặc duyệt của lớp đó.
2. Học sinh không dùng tutor khi kiểm tra kể cả gọi thẳng API.
3. Lỗi dịch vụ toán hoặc cổng kiểm không biến thành đạt hay gửi gợi ý chưa lọc.
4. CSV giữ dấu, thoát dấu nháy/xuống dòng và không thực thi công thức bảng tính.
5. 390px/theme tối/reduced-motion vẫn thao tác được và không che nút nộp.

## Kiểm chứng cuối (root)

- [ ] Đọc diff các vùng, giải quyết hợp đồng và lỗi tích hợp.
- [ ] `pnpm lint`, test script, frontend build/test, core verify; math nếu thay hợp đồng math.
- [ ] Kiểm trình duyệt/e2e nếu dựng được runtime; ghi chính xác phần chưa chạy.
- [ ] Đối chiếu U0–U15, cập nhật mặc định và hạn chế; trả tóm tắt tự đủ, không commit/push.
