# Kiểm tra quyết định khách — 2026-10-14

Nền Git: `b9d50f3`, diff chưa commit. JDK 25.0.4.1+1; Node 22.23.3; pnpm 10.33.3. Không sửa `.github`, package tutor, component gia-su, prompt gia-su hoặc migration V14/V15. Chỉ thêm `V16__confirmed_client_decisions.sql`.

| Kiểm tra | Lệnh | Kết quả thật |
| --- | --- | --- |
| Frontend unit | `npx -y pnpm@10.33.3 --filter frontend test` (`ng test --watch=false`) | 223/223 test qua, 31 tệp; không lỗi ngoài test. |
| Frontend production | `npx -y pnpm@10.33.3 --filter frontend build` | Qua. |
| FLAT | `node scripts/check-flat.mjs`; `node --test scripts/check-flat.test.mjs` | Qua; 1/1 test cổng qua. |
| Tương phản | `node scripts/tuong-phan-token.mjs` | 33 token khớp, 57 cặp bắt buộc qua. |
| Lint nguyên lệnh | `npx -y pnpm@10.33.3 lint` | Bị môi trường chặn `spawnSync /bin/sh EPERM` trong bước lấy `git ls-files`. FLAT và tương phản chạy riêng qua. Cùng mã quét khóa chạy với danh sách index lấy trước bằng `git ls-files -z`: không có khóa trong git. |
| Core nguyên suite | `JAVA_HOME=… ./mvnw -B verify` | 552 test được liệt kê: 360 qua, 191 bỏ qua, 1 lỗi môi trường ở `MathServiceClientTest.start` (`Socket Operation not permitted`). Không có assertion thất bại. |
| Core trừ test cần mở HTTP socket | `./mvnw -B '-Dtest=*,!MathServiceClientTest' verify` | 551 test được liệt kê: 360 qua, 191 bỏ qua, 0 lỗi; BUILD SUCCESS. Gồm ArchUnit, test cổng xem lời giải, giải thích qua mô hình, đồng biên soạn, nhắc Push và chữ ký VAPID. Đây không thay kết quả suite đầy đủ. |
| Playwright | `npx -y pnpm@10.33.3 --filter frontend e2e client-decisions` | Không khởi động được SPA server: `listen EPERM 127.0.0.1:4300`. Chưa thực thi trình duyệt. |
| Playwright discovery | `… --filter frontend exec playwright test client-decisions --list` | Nhận đủ 8 ca ở 2 project; spec đặt desktop 1280 px, mobile 390 px. |
| Diff | `git diff --check` | Qua. |

Sandbox không cho truy cập Docker socket; toàn bộ test PostgreSQL/Testcontainers, gồm các ca persistence mới, tự bỏ qua theo cấu hình repo. Chưa xác minh migration trên PostgreSQL thật, gửi Push thật hoặc gọi nhà AI thật. Cần chạy lại suite đầy đủ và Playwright trên môi trường cho phép Docker và localhost trước khi nghiệm thu.

Maven dùng bản sao cache tại `/tmp/mathl-client-m2` qua `-Dmaven.repo.local=…` vì cache gốc chỉ đọc, và `-DargLine=-javaagent:…/mockito-core-5.23.0.jar` vì sandbox chặn tự attach Mockito. npm dùng cache tạm và `npx --offline -y --prefix /home/box/.npm/_npx/a2581990a6675e57 pnpm@10.33.3` để dùng đúng pnpm đã cài khi mạng không phân giải được registry. Không đổi mã kiểm tra để che lỗi môi trường.
