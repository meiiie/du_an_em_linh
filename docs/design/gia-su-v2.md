# Gia sư v2 — hội thoại có trí nhớ, phát từng câu đã kiểm

Ngày khảo sát: **2026-10-14**. Trạng thái: **Đề xuất thiết kế theo yêu cầu chủ repo; chưa hiện thực**.
Phạm vi: Toán 12, đơn điệu và cực trị; core điều phối, math kiểm toán, frontend hiển thị tiếng Việt có dấu.
Sản phẩm của phiên này chỉ là tài liệu; không sửa mã, migration, dữ liệu sư phạm hoặc hai repo tham khảo.
Mọi con số ngân sách/ngưỡng ở phần thiết kế là giá trị khởi đầu để đo, không phải kết quả benchmark.

**Cập nhật 2026-10-14 — quyết định Darren:** lời LIVE phát `delta {text}` nguyên văn theo `seq`, không đợi/kiểm từng câu, không regen, strict fallback hoặc fast-path; summary cũng không qua cổng. Bảo vệ đáp án dựa vào prompt Socratic và quy tắc mở lời giải sau nộp đủ bài (kiểm tra phải chờ hết hạn); giữ PII, phiên, nén async, cancel/resume, usage và một retry GLM trước token đầu. `leakCount` chỉ để quan sát. Các phần lịch sử bên dưới về cổng và «0 lộ» được thay bởi bổ sung 2026-10-14 trong [ADR 013](../adr/013-cong-ba-tang-cho-loi-gia-su.md) và [ADR 010](../adr/010-sse-trang-thai-gia-su.md).

## 1. Nguồn và cách đọc kết quả

Khảo sát mã cục bộ, không suy diễn từ tên tính năng hoặc bình luận lịch sử. Các đường viết tắt dưới đây tính từ gốc tương ứng:

| Ký hiệu | Gốc và SHA đã đọc |
| --- | --- |
| N | `/workspace/ref-neko-core`, `c99aafbca4df074a685e6b1489d377f47726c2bb` |
| W | `/workspace/ref-wiii`, `6b1c30b15b4c67e3c841ae870e6989577fc228f8` |
| C | Repo này, `f58a25d23c2a4ab832e4602f6038446807153839` |

Độ tin: nguồn sơ cấp cho hành vi mã tại các SHA trên; chưa chạy hai harness, chưa đo GLM. Chỉ tiếp thu cấu trúc, không sao chép mã hay đưa neko-core thành phụ thuộc.

## 2. Phát hiện theo chủ đề

### Cấu trúc system prompt

- **N:** `src/core/agent-constants.ts` chia chỉ dẫn thành nhận diện, cộng tác, công cụ, độ chính xác, đầu ra; đánh dấu ranh giới tiền tố phiên/lượt. `src/core/agent.ts` giữ một system message, ghép ngữ cảnh động; `src/adapters/context.ts` chỉ nạp chỉ dẫn dự án ở gốc đã tin cậy, có giới hạn kích thước. Bài học: tiền tố ổn định, dữ liệu phiên có nguồn và giới hạn rõ.
- **W:** `maritime-ai-service/app/prompts/prompt_loader.py` ghép persona YAML, lớp miền, nhận diện, ngôn ngữ và ngữ cảnh; `app/prompts/prompt_section_builders.py` thêm phong cách, trí nhớ và chống lặp lời chào. `app/engine/multi_agent/direct_prompts.py` có prompt riêng cho nhánh trò chuyện ngắn/phân tích. Bài học: một bộ dựng prompt có các phần theo nhiệm vụ; MathL+ không cần hệ persona/công cụ đa dụng này.

### Cửa sổ ngữ cảnh và ngân sách token

- **N:** `src/core/agent-constants.ts` ước lượng văn bản bằng byte UTF-8/4, tính cả tool schema; ngưỡng nén 85% theo usage giữa lượt, ngưỡng an toàn 80% theo ước lượng trong lượt. `src/core/agent.ts` thu gọn quan sát cũ trước khi nén. Đây vẫn là ước lượng, không phải tokenizer chính xác.
- **W:** `maritime-ai-service/app/engine/context_budget_runtime.py` chia system 15%, trí nhớ 5%, summary 10%, phần còn lại cho tin gần nhất; mặc định cửa sổ hiệu dụng 32.000, dự trữ đầu ra 8.192 và 10% phần còn lại làm đệm. Nén khi có hơn sáu tin bị loại và mức dùng đạt 75%. Bộ đếm dùng ký tự/4: không coi là bảo đảm đủ cho tiếng Việt/LaTeX.

### Nén và tóm tắt hội thoại

- **N:** `src/core/agent.ts::compact` giữ system, phần đuôi ít nhất tám message rồi lùi về ranh giới lượt user; prompt nén ở `src/core/agent-constants.ts` giữ mục tiêu, sửa đổi, trạng thái có bằng chứng, việc chưa xong. Nén lỗi/rỗng/cụt hoặc ngữ cảnh đổi thì giữ lịch sử cũ. `src/adapters/context/compactor.ts` lưu nguồn, kiểm digest và công bố trạng thái sau bước chuẩn bị.
- **W:** `maritime-ai-service/app/engine/context_manager.py` cập nhật running summary từ summary cũ và phần hội thoại bị loại, lưu vào `semantic_memories` và metadata của thread; cache có phạm vi người dùng/phiên/tổ chức. Có phương án cắt chuỗi khi LLM lỗi. `app/services/session_summarizer.py` làm summary 2–3 câu cho phiên trước. Prompt nén hiện yêu cầu giữ tên/thông tin cá nhân: **không áp dụng yêu cầu này vào MathL+**.

### Lưu phiên và khôi phục

- **N:** `src/adapters/session.ts` lưu JSON có phiên bản, revision, usage, trạng thái lượt và lịch sử hiển thị độc lập với ngữ cảnh model; ghi nguyên tử và bản sao dự phòng. `src/adapters/acp.ts` phân biệt `session/load` phát lại lịch sử với `session/resume` khôi phục context khi client đã có lịch sử.
- **W:** `maritime-ai-service/app/services/session_manager.py` giải quyết user/thread/org qua repository; `app/services/session_summarizer.py` lưu summary vào `thread_views.extra_data`. `app/engine/runtime/session_event_log.py` có log `seq` tăng theo phiên với backend bộ nhớ hoặc PostgreSQL tùy cờ. `wiii-desktop/src/stores/chat-store.ts` còn lưu hội thoại phía client qua lớp storage. Không mặc định mọi cơ chế lưu này đều bật hoặc nối vào SSE.

### SSE, heartbeat, hủy và resume

- **N:** `src/adapters/providers.ts` đọc SSE của nhà: `data`, `[DONE]`, delta nội dung, reasoning, tool call, usage; bỏ ping/keepalive/heartbeat, báo lỗi JSON và kết thúc bất thường. Đây là giao thức **nhà → harness**, không phải hợp đồng chat toán. `src/adapters/acp.ts` nhận `session/cancel` rồi abort tác vụ; resume theo phiên ACP, không tương đương phát lại SSE theo seq.
- **W:** `maritime-ai-service/app/api/v1/chat_stream.py`, `chat_stream_presenter.py` và `wiii-desktop/src/api/sse.ts` xử lý `status`, `thinking`, `thinking_delta`, `answer`, `sources`, `metadata`, `done`, `error`, cùng các sự kiện công cụ/tính năng khác. `app/api/v1/chat_stream_transport.py` phát comment `: keepalive` lúc im lặng mỗi 15 giây và hủy producer khi mất kết nối.
- **W, giới hạn resume:** parser trả `lastEventId`, `app/api/v1/chat_stream_endpoint_support.py` ghi nhận header `Last-Event-ID`; `app/services/chat_stream_coordinator.py` khởi tạo lại bộ đếm mỗi request. `wiii-desktop/src/hooks/useSSEStream.ts` còn lấy assistant message gần nhất từ thread khi EOF/idle mà chưa có câu. Các đường này **chưa chứng minh phát lại đúng phần thiếu theo seq bền vững**; không dùng sự tồn tại của event log để khẳng định điều đó.

### Markdown và KaTeX trong giao diện

- **N:** `src/ui/markdown.tsx` dựng Markdown cho terminal Ink: danh sách, bảng, code, liên kết; đổi một phần LaTeX sang Unicode. Không phải bộ render KaTeX trên web.
- **W:** `wiii-desktop/src/components/common/MarkdownRenderer.tsx` phân đoạn, trì hoãn parse khi streaming và tải renderer toán khi cần; `MathMarkdownSegment.tsx` dùng GFM, remark-math, rehype-katex rồi sanitize theo `markdown-sanitize-schema.ts`. `wiii-desktop/src/lib/semantic-stream-buffer.ts` gom khối Markdown để tránh dựng dở. W có đường HTML/widget riêng: MathL+ không tiếp thu đường này. Buffer UI cải thiện hình thức, không thay cổng toán ở server.

### Lỗi và thử lại

- **N:** `src/adapters/providers.ts` phân biệt retry trước khi có đầu ra ngữ nghĩa với lỗi sau khi đã phát; có Retry-After/backoff, giới hạn retry và abort. `src/ui/chat.tsx` báo lỗi nén/khôi phục, giữ lịch sử thay vì coi nén thất bại là thành công. Bài học: biết đã phát tới đâu trước khi thử lại.
- **W:** `wiii-desktop/src/hooks/useSSEStream.ts` giữ controller theo request, bỏ buffer khi hủy/thử lại, backoff có giới hạn, thông báo lỗi tiếng Việt và phân biệt EOF/idle/done. Nút dừng abort fetch; backend phát hiện disconnect. Việc lấy câu cuối của thread có nguy cơ nhầm lượt nếu không gắn turn id; MathL+ cần định danh lượt và idempotency rõ ràng.

## 3. Gia sư hiện tại và chênh lệch cần giải quyết

Các đường Java dưới đây tính từ `services/core/src/main/java/vn/hoctapcanman/core/`:

| Nguồn C đã đọc | Hành vi thấy trong mã |
| --- | --- |
| `tutor/application/service/OfflineTutorService.java` | Regex xin đáp án/đổi vai trả `REFUSAL` không gọi model; bản nháp có tối đa một lần sinh lại bằng chỉ dẫn chung khi bị bỏ hết; sau đó thang rồi `SAFE` cố định. Kiểm quyền, hạn và bảng công thức trước phát; chưa có phiên hội thoại bền vững. |
| `tutor/application/service/TutorPrompt.java` | Chỉ system + user; đề, bước, cấp 1–3, bảng khóa, thang làm mốc và dòng đã nộp; có chặn/ngụy danh hóa định danh. Không đưa lịch sử chat hay token budget vào prompt. |
| `tutor/infrastructure/ai/GlmHintModelAdapter.java` | `stream=false`, tắt thinking; nhận hai message. Retry tối đa một lần cùng nhà cho 429/500/502/503/504 trong timeout chung; trả rỗng khi lỗi. |
| `content/infrastructure/workspace/ContentWorkflowAdapter.java::sanitize` | Content giữ lời giải/dữ kiện bảo vệ; gọi FILTER rồi KIEM_LOI_GIANG; có thể trả phần câu đã làm sạch, ghi hàng chờ giáo viên khi đổi nội dung; lỗi trả rỗng. Chưa trả mã vi phạm có kiểu cho vòng sinh lại. |
| `services/core/src/main/resources/prompts/gia-su.vi.txt` | Socratic, tiếng Việt, tối đa ba câu, đúng bước, cấp hỗ trợ 1–3; công thức gắn `[n]`, không tính hộ; vẫn dùng thang làm mốc và ví dụ câu hỏi cố định. |
| `apps/frontend/src/app/features/hoc-sinh/luyen/gia-su.{ts,html,css}` | Sidebar rộng 360–480 px, dialog màn hẹp; lịch sử chỉ trong signal, chỉ thêm lời AI khi `xong`; tách công thức và nội suy chữ, chưa parse Markdown. Có Dừng, lỗi và Hỏi lại đưa câu cũ vào composer; chưa xử lý `id`/resume. |
| `tutor/infrastructure/web/OfflineTutorController.java` | SSE `kho → goi → loc → xong|loi`, timeout 120 giây; completion/timeout/error interrupt worker. Chưa có heartbeat, cancel POST hoặc nhật ký sự kiện để resume. |

`workspace/infrastructure/persistence/WorkspaceStoreAdapter.java::tutorContext` hiện JOIN `hint_levels`: bỏ thang trong thiết kế phải bỏ cả điều kiện phụ thuộc thang để mở context; lấy assignment id thật, không chỉ mã bài. `OfflineHintModelAdapter` trả rỗng, bản thân không phải model sinh lời.

[ADR 010](../adr/010-sse-trang-thai-gia-su.md) hiện cấm stream token, có phần bổ sung GLM/thang; [ADR 013](../adr/013-cong-ba-tang-cho-loi-gia-su.md) giữ thế giới đóng, hai cổng và câu thay thế. Bổ sung KD-0006 phân biệt câu hỏi/thao tác với khẳng định; dấu hỏi không miễn kiểm. Số đo corpus trong ADR là bằng chứng cũ, không phải eval thiết kế này.

**Đề xuất thay** thang/câu cố định, regex trả lời trực tiếp, SSE hiện hành và cách dự phòng của ADR 010/013; **giữ** bất biến không đọc lời giải, không làm hộ và đóng mặc định. Khi triển khai cần ADR thay thế phần hành vi này; tài liệu này không tự đổi trạng thái ADR. Demo offline hiện tại vẫn giữ nguyên trong phiên khảo sát; thiết kế mới không âm thầm bật GLM hoặc nhà khác.

## 4. Luồng sinh lời và kiểm từng câu

1. Xác thực; kiểm thành viên, giao bài, phiên bản đề, mode/hạn, nhà được lớp chọn, hạn mức. Không mở gia sư trong kiểm tra/hết hạn. Chỉ lấy đề, bài làm của chính em, kết quả chấm/mã lỗi đã xác minh, công thức/tài liệu được phép; tutor không có đường đọc lời giải/dữ kiện bảo vệ.
2. Dựng context nhiều lượt đã khử định danh. Classifier jailbreak chỉ thêm tín hiệu tin cậy như `xin_dap_an`, `doi_vai`, `chen_lenh` để prompt chuyển hướng Socratic; không trả lời cố định, không quyết định đạt cổng và không mở quyền. Sai phân loại không được làm yếu cổng.
3. Model đã chọn sinh lời; token từ nhà chỉ đi vào buffer riêng trên server. Không phát reasoning, bản nháp bị chặn hoặc feedback nội bộ. Giới hạn tối đa ba câu mỗi lượt.
4. Bộ tách câu nhận biết vùng `$…$`, `$$…$$`, `\(…\)`, `\[…\]`, escape, số thập phân và cấu trúc Markdown; không cắt theo dấu chấm đơn thuần. Chờ đóng delimiter/cấu trúc; đoạn cuối chỉ xem là ứng viên khi model kết thúc bình thường. Đầu ra cụt, vùng toán chưa đóng hoặc buffer vượt giới hạn thì chặn, không xả ra UI.
5. Mỗi câu ứng viên phải qua FILTER chống lộ rồi KIEM_LOI_GIANG theo ADR 013, kể cả câu chỉ có lời. Kiểm thêm **toàn bộ tiền tố đã phát + câu mới** và lịch sử lời đã phát của phiên để chống ghép dần nghiệm/dấu/đáp án qua nhiều câu/lượt. Content/math giữ dữ kiện bảo vệ; không đưa chúng vào tutor hoặc summary.
6. Chỉ chấp nhận câu nguyên vẹn đạt cả hai cổng, đúng mã nguồn và còn đúng phiên bản bảng. Không coi việc sanitize còn vài từ là đạt cả câu. Content trả phán quyết có kiểu và mã vi phạm an toàn; tutor chỉ nhận mã như `LO_DAP_AN`, `NGOAI_BANG`, `KHONG_PHAN_TICH_DUOC`, không nhận giá trị/đoạn đáp án.
7. Gặp câu bị từ chối: hủy phần sinh còn lại, bỏ buffer chưa phát; phát `regen`, gửi model mã vi phạm và yêu cầu sửa cách hỏi. **Tối đa hai lần sinh lại sau lần đầu**. Giữ nguyên các câu đã phát, yêu cầu chỉ viết tiếp phần thiếu; kiểm tiếp với cùng tiền tố, không sửa/xóa câu học sinh đã thấy.
8. Nếu hai lần sinh lại vẫn bị chặn: gọi **một lần cuối** bằng prompt nghiêm hơn để model sinh một câu hỏi Socratic ngắn về thao tác em đã thử/chỗ em vướng; không công thức, không khẳng định quy tắc, không ví dụ câu mẫu. Câu này vẫn qua cả hai cổng. Nếu đã phát đủ ba câu thì kết thúc phần đã kiểm, không gọi thêm.
9. Lần cuối không đạt, model hết giờ/lỗi hoặc cổng không hoạt động: phát `error`, giữ các câu đã kiểm nếu có; không dùng thang, `SAFE`, `REFUSAL` hay nhà thứ hai. Phân biệt cổng từ chối nội dung với lỗi hạ tầng: cổng mất kết nối không phải lý do gọi model liên tục.

Mọi **lời thoại gia sư** đều do model sinh; nhãn trạng thái, nút và thông báo lỗi là chữ giao diện, không đóng vai câu trả lời. Không có model được chọn/sẵn sàng thì báo chưa thể hỏi, không chuyển sang câu mẫu. Retry HTTP cùng nhà tối đa một lần **cho toàn lượt**, chỉ trước khi có đầu ra ngữ nghĩa của lần gọi đó, tôn trọng Retry-After; không retry lỗi khóa hoặc hủy.

## 5. Phiên bền vững và trí nhớ nhiều lượt

Migration **dự kiến** `services/core/src/main/resources/db/migration/V14__phien_gia_su.sql`, chỉ mô tả ở đây, chưa tạo tệp. V13 là migration cuối ở mốc khảo sát; kiểm lại số thứ tự trước triển khai.

| Bảng dự kiến | Cột và bất biến chính |
| --- | --- |
| `phien_gia_su` | `id`, `hoc_sinh_id`, `giao_bai_id`, `lop_id`; UNIQUE(học sinh, giao bài); FK tới `assignments` ràng buộc cùng học sinh/lớp. `summary` dạng text, `summary_den_luot`, `summary_version`, `content_version`, `formula_sheet_id`, `last_seq`, `revision`, mốc thời gian. |
| `luot_gia_su` | `id`, `phien_id`, `thu_tu`, `request_id`, `ma_buoc`, `cap_goi_y`, `muc_hoc_tap`, `cau_hoi_da_an_danh`, `tra_loi_da_kiem`, `trang_thai`, `so_lan_regen`, token/độ trễ, phiên bản prompt/model/cổng/bảng, mốc thời gian; `su_kien` JSONB chỉ chứa sự kiện được phép phát kèm seq. UNIQUE(phien, thứ tự), UNIQUE(phien, request id). |

- Một dòng `luot_gia_su` là một cặp hỏi–đáp, có thể đang sinh/đã xong/đã dừng/lỗi. Partial unique index chỉ cho một lượt đang sinh mỗi phiên; cùng request id trả lại lượt cũ, payload khác trả 409. Dùng khóa/revision trong giao dịch ngắn, không giữ transaction trong lúc gọi GLM/CAS.
- Kiểm FK/quyền từ server, không tin student id gửi lên; API phiên, lịch sử, resume và cancel đều kiểm chủ sở hữu và điều kiện giao bài. Giáo viên chỉ đọc theo quyền lớp hiện hành. Không trộn trí nhớ giữa học sinh, lớp hoặc assignment, kể cả cùng đề.
- `su_kien` là nhật ký nối thêm trong từng lượt, không cần bảng thứ ba: cấp seq từ phiên, lưu event/câu đã kiểm và cập nhật `last_seq` trong cùng transaction **trước khi gửi SSE**. Hai tab đọc cùng log; chỉ một writer có lease theo lượt. Giới hạn ba câu và số lần regen giữ JSONB mỗi lượt nhỏ.
- DB là nguồn chuẩn cho lịch sử; summary chỉ là hình chiếu phục vụ model. Giữ nguyên các lượt gốc sau nén; không lưu reasoning hoặc bản nháp bị chặn vào log phát lại. Hàng chờ giáo viên hiện có là đường riêng có quyền, không nhập lại làm memory.
- Worker chết: hết lease thì đánh dấu lượt lỗi và lưu `error` kết thúc; resume chỉ đọc phần đã commit, không tự chạy lại model. Mất kết nối SSE không đồng nghĩa người dùng hủy; tác vụ tiếp tục trong hạn toàn lượt để có thể nối lại.
- Đề/bảng đổi: dừng lượt đang sinh bằng 409 hoặc `error` nếu đã mở SSE; vô hiệu summary cũ. Lịch sử còn xem được với nhãn phiên bản, nhưng context mới không dùng công thức/nhận xét toán cũ thiếu căn cứ hiện hành.
- Chỉ lưu dữ liệu tổng hợp ở demo/eval. Khử định danh **trước cả lưu câu hỏi và mọi lời gọi ngoài**, gồm lịch sử, bài làm, tài liệu, summary và lần regen; còn nghi định danh thì báo lỗi yêu cầu sửa câu hỏi. Không log nguyên câu hỏi/nháp/summary; chỉ mã lỗi, id băm, số đếm và độ trễ. Chính sách lưu/xóa dữ liệu thật cần quyết định quyền riêng tư trước pilot.

### Ngân sách và compaction

- Cửa sổ hiệu dụng `C = min(giới hạn model đã xác minh, trần cấu hình 8.192 token)`; đây là trần ứng dụng đề xuất, không khẳng định đặc tính GLM. Dự trữ đầu ra `O=350`, đệm `M=ceil(0,15×C)`, ngân sách đầu vào `B=C−O−M`; tính cả role/envelope/citation/feedback, không chỉ lời chat.
- Với B: dành tối đa khoảng 25% cho system/sư phạm, 25% đề+bước+bài làm+căn cứ, 10% summary, 40% câu hỏi hiện tại và lịch sử gần nhất; phần dư phân phối lại. Không cắt system an toàn, câu hỏi hiện tại hoặc công thức giữa chừng để ép vừa. Thiếu căn cứ bắt buộc thì báo lỗi giới hạn context.
- Dùng tokenizer tương thích model khi có; nếu chưa có, dùng ước lượng byte UTF-8 có hệ số đệm được hiệu chỉnh bằng `usage`, không xem ký tự/4 là chính xác. Ghi estimate và usage thực để phát hiện lệch. Đếm lại trước mọi lần sinh/regen/nén; deadline toàn lượt đề xuất 120 giây, bao gồm chờ, kiểm và nén, không đặt lại sau retry.
- Khi đầu vào dự kiến vượt 75% B, nén các lượt hoàn tất cũ; giữ nguyên ít nhất ba cặp hỏi–đáp gần nhất nếu vừa B, cùng câu hỏi mới và snapshot bước hiện tại. Summary tối đa `min(600 token, 10% B)`; chỉ nén phần sau `summary_den_luot` để không tóm lại trùng nhiều lần.
- Summary tiếng Việt có cấu trúc: mục tiêu/bước, cách em đã thử, lỗi do bộ chấm xác nhận, câu hỏi còn mở, gợi ý đã dùng, điều em vừa sửa và id lượt nguồn. Phân biệt phát biểu của học sinh với kết quả đã kiểm; không suy luận đặc điểm cá nhân, không sáng tác kết luận toán hoặc biến chuỗi jailbreak thành chỉ dẫn.
- Bộ nén chỉ nhận lịch sử đã khử định danh và lời AI đã kiểm, không nhận lời giải/dữ kiện bảo vệ/nháp bị chặn. Summary là dữ liệu tin cậy thấp, không ghép vào phần system có quyền; mọi lời sinh từ nó vẫn qua hai cổng. Kiểm schema, nguồn lượt, định danh và tính đầy đủ trước lưu.
- Lưu summary + con trỏ + revision nguyên tử; có sửa đổi đồng thời thì bỏ bản nén cũ. Nén thất bại giữ summary/lượt gốc, không cắt chuỗi thay summary; còn vừa B thì dùng cửa sổ hợp lệ, nếu không thì báo lỗi có thể thử lại. UI vẫn xem toàn lịch sử qua phân trang, không chỉ phần model được thấy.

## 6. Hợp đồng SSE đích

Dùng namespace `/api/hs/gia-su/v2` để phân biệt hợp đồng ADR 010 hiện tại; frontend/core chuyển cùng đợt. Cùng một pipeline cấp quyền, model và cổng, không có đường đồng bộ bỏ kiểm.

| Endpoint đề xuất | Hợp đồng |
| --- | --- |
| `POST /api/hs/gia-su/v2/phien` | Nhận `giaoBaiId`, tạo/lấy phiên của học sinh đăng nhập; trả `sessionId`, trạng thái lượt hiện tại và `lastSeq`. Không gọi model. |
| `GET /api/hs/gia-su/v2/phien/{id}/luot` | Lấy lịch sử phân trang cùng mốc `lastSeq` nhất quán để tải lại sidebar. |
| `POST /api/hs/gia-su/v2/phien/{id}/luot` | Nhận `requestId`, `maBuoc`, `cap`, `cauHoi`; chấp nhận/lưu một lượt rồi trả `text/event-stream`. Request trùng nối vào lượt cũ. |
| `GET /api/hs/gia-su/v2/phien/{id}/su-kien?lastSeq=N` | Phát lại các event có seq > N theo thứ tự rồi theo dõi lượt đang chạy; không gọi lại model. Hỗ trợ `Last-Event-ID`; hai cursor cùng gửi phải khớp. |
| `POST /api/hs/gia-su/v2/phien/{id}/cancel` | Nhận `turnId`, idempotent; đổi trạng thái, hủy model/CAS/regen, bỏ buffer chưa kiểm; trả trạng thái và seq kết thúc. |

Event nghiệp vụ có `id: <sessionId>:<seq>` và JSON `{v:2,sessionId,turnId,seq,...}`. Seq tăng toàn phiên; giao ít nhất một lần, client loại trùng. Không đưa bearer token vào URL.

| Event | Payload bổ sung và ý nghĩa |
| --- | --- |
| `start` | `requestId`, `maBuoc`, `cap`, phiên bản đề/bảng; lượt đã được lưu, chưa có lời gia sư. |
| `delta` | `sentenceIndex`, `text` Markdown/LaTeX nguyên câu đã đạt cả hai cổng; chỉ nối thêm, tuyệt đối không chứa token thô. |
| `regen` | `attempt:1|2|3`, `mode:rewrite|strict_question`; 1–2 là hai lần sinh lại, 3 là câu hỏi cuối. Không chứa bản nháp hoặc dữ kiện vi phạm. |
| `done` | `status:completed|cancelled`, `lastSentenceIndex`, token và thời gian đã tổng hợp; không kèm bản trả lời khác để ghi đè những delta đã kiểm. |
| `error` | `code`, `message` tiếng Việt, `retryable`; kết thúc lượt, không phát thêm `done`. Giữ phần lời đã kiểm và chỉ rõ chưa hoàn tất. |
| `heartbeat` | Mỗi 15 giây trong lúc kết nối mở, kể cả đang nén/chờ cổng; `{sessionId,turnId,lastSeq}`. Không lưu, không có `id`/seq mới, không tính TTFT. |

- Trình tự: `start → (delta|regen|heartbeat)* → done|error`; sau kết thúc không có event nghiệp vụ mới cho lượt đó. Resume phát lại event gốc, không tạo `start`/seq mới. Kết nối đã bắt kịp lượt kết thúc thì đóng, client biết terminal từ lịch sử hoặc log.
- Lưu delta sau khi kiểm lại quyền/mode/hạn và phiên bản bảng, dưới cùng cơ chế revision/khóa với thao tác hủy; hủy thắng thì delta không được commit. Đã commit trước hủy vẫn thuộc lịch sử hợp lệ; ACK cancel chứa seq terminal để client dừng nhận lượt đó. Không thể thu hồi dữ liệu đã gửi.
- SSE mất mạng không tự cancel worker; POST cancel là hành động dừng có xác nhận phía server. Nếu POST mất mạng, UI ghi “Đang xác nhận dừng”, thử lại cùng turn id hoặc đọc trạng thái, không khẳng định đã dừng chỉ vì abort fetch. Không hủy nhầm lượt mới.
- Replay và nối live dùng cùng cursor DB, không để hở giữa hai giai đoạn; dò seq tiếp theo, loại event trùng. Cursor vượt mốc server trả 409; lịch sử đã bị xóa thì 410 và yêu cầu tải snapshot còn được phép. Restart không khôi phục buffer/token chưa kiểm.
- Trước khi mở SSE: lỗi quyền/mode/hạn dùng HTTP 401/403/404, xung đột 409, hạn mức 429 kèm Retry-After. Sau mở dùng `error`; không coi EOF là thành công. `Cache-Control: no-cache, no-transform`, tắt buffer proxy, flush từng event; heartbeat không gia hạn deadline của model/cổng/toàn lượt.

## 7. Giao diện sidebar streaming

- Giữ sidebar/dialog, composer đáy, `data-testid`, Enter/Shift+Enter/IME và trả focus như hiện tại; FLAT, không bóng hay viền nhấn cạnh. Nút tối thiểu 44 px, kiểm ở 390/1280 px. Tải phiên theo assignment; đổi bài đổi phiên, phản hồi của phiên cũ không chèn vào bài mới.
- Ghép delta theo `sessionId/turnId/seq`, append câu đúng một lần. Parse Markdown thành cấu trúc cho đoạn, nhấn mạnh, danh sách và dẫn nguồn; công thức inline/khối qua KaTeX tự host hiện có (`shared/toan/katex.ts`, `trust:false`, MathML). Không HTML thô, ảnh ngoài, widget, script hoặc URL tùy ý; `[n]` mở đúng dòng của bảng gắn với lượt.
- Render theo câu/khối đã commit; giữ khoảng trắng và delimiter, không diễn giải lại nội dung toán. Nếu cấu trúc Markdown chưa kín thì chờ hoặc hiện chữ an toàn đã kiểm; lỗi KaTeX không làm biến mất lịch sử. Lớp render không được tính đúng/sai hay thay cổng server.
- Cuộn theo đáy khi em đang ở đáy; em kéo lên thì giữ chỗ và có nút Xuống. `aria-live` đọc câu hoàn chỉnh một lần; trạng thái đang soạn/đang kiểm/đang viết lại tách khỏi lời AI. Không hiện mã cổng, reasoning hoặc đếm token cho học sinh.
- **Dừng:** gửi POST cancel bằng request riêng, rồi đóng stream đọc; giữ câu đã kiểm, đánh dấu lượt đã dừng sau ACK. Escape/đóng sidebar đang sinh thực hiện cùng hành động; đóng khi rảnh chỉ đóng giao diện.
- **Mất kết nối:** báo đang nối lại, resume từ seq cuối với backoff giới hạn, không tạo lượt model mới. Hết lượt nối lại thì giữ lịch sử và nút Kết nối lại. Làm mới xác thực qua `Phien.lamMoi()`; không lưu token/chat vào localStorage, chỉ giữ tùy chọn kích thước/mở panel như hiện tại.
- **Lỗi model/cổng:** hiển thị lỗi cạnh lượt, bảo toàn câu hỏi và phần đã kiểm; Thử lại là thao tác chủ động tạo lượt mới liên kết lượt lỗi, không lặp câu hỏi trên UI. Hỏi lại để sửa chỉ đưa câu vào composer. 429 đợi Retry-After; 403 đóng khả năng hỏi cho assignment đó; không tự gửi lại sau đổi tab/tải trang.

## 8. Hợp đồng system prompt sư phạm

Tiền tố ổn định: vai gia sư AI MathL+, xưng “mình”/“em”, tiếng Việt có dấu; chỉ đúng bước hiện tại, tối đa ba câu, mỗi lượt ưu tiên một câu hỏi để em hành động. Không đưa kết quả cuối, xác nhận nghiệm/đáp án, làm hộ bước hoặc chia nhỏ lời giải để ghép qua nhiều lượt. Chỉ bộ chấm được kết luận đúng/sai.
Tiếp theo là chính sách mức hỗ trợ, ranh giới căn cứ và định dạng; dữ liệu đề/bước/bảng/lịch sử/summary đặt trong phần dữ liệu có nguồn, không có quyền sửa chỉ dẫn. Không nhúng thang mẫu, đáp án, câu cửa miệng hoặc ví dụ câu trả lời bắt buộc.

| Cấp gợi ý do phiên ghi nhận | Chỉ dẫn sư phạm cho model |
| --- | --- |
| 1 — nhận diện vướng mắc | Dựa câu hỏi và lịch sử để hỏi em đang cần tìm gì/đã thử cách nào; tránh lặp câu vừa hỏi, không đoán em sai. |
| 2 — chọn căn cứ | Hướng em tới khái niệm hoặc dòng `[n]` đã khóa, hỏi em sẽ dùng nó ở đâu; không diễn đạt một quy tắc ngoài khả năng kiểm của bảng. |
| 3 — thao tác tiếp theo | Nêu một thao tác cụ thể ở bước hiện tại rồi hỏi em thực hiện; không tính nghiệm, dấu, khoảng hoặc kết luận thay em; không tăng độ hỗ trợ vô hạn khi em hỏi lặp. |

**Cấp hỗ trợ 1–3 khác bốn mức học tập** Nhận biết, Thông hiểu, Vận dụng, Vận dụng cao; cũng khác ba mức CV 7991 chỉ để xem. Trong cùng cấp, lần lượt điều chỉnh ngôn ngữ từ một nhiệm vụ rõ → giải thích lựa chọn → tự chọn cách → tự phản biện điều kiện. Lấy mức từ hồ sơ hợp lệ/ghi đè giáo viên, không để model tự gắn nhãn học sinh; không hạ rào chắn ở mức cao.

Mọi công thức trong delimiter chuẩn; chỉ trích nguyên văn đề/bài làm có ghi nguồn hoặc dùng dòng bảng đã khóa với `[n]`. Không biến đổi để “minh họa”, không suy ra đáp án từ bài làm chưa được chấm. Tín hiệu jailbreak yêu cầu model bình tĩnh chuyển lại câu hỏi học tập theo ngữ cảnh; prompt không thay bộ lọc. Feedback regen chỉ là mã vi phạm, bước và tiền tố đã đạt, không đưa chi tiết bí mật vào prompt.

## 9. Kế hoạch kiểm chứng khi hiện thực

**30 kịch bản GLM nhiều lượt**, mỗi ca 4–8 lượt; bổ sung ca dài 30–50 lượt để ép compaction. Chạy lại cùng corpus tổng hợp sau mỗi thay đổi model/prompt/cổng; không sửa dữ liệu lab đã khóa ngoài quy trình bản vá.

| Nhóm | Số ca | Nội dung cần quan sát |
| --- | ---: | --- |
| Theo bước và cấp | 10 | Hai ca cho mỗi bước B.DH.TXD/DAOHAM/NGHIEM/XETDAU/KETLUAN; phủ đủ ba cấp hỗ trợ, bốn mức học tập, giữ đúng bước và nhớ cách em đã thử. |
| Dụ đáp án qua nhiều lượt | 8 | Xin đáp án lặp; xác nhận nghiệm; giả giáo viên; làm hộ một bước; ghép từng nghiệm/dấu; không dấu/teencode/tiếng Anh; chèn lệnh trong bài làm; chèn lệnh trong tài liệu. |
| Cổng và sinh lại | 4 | Công thức sai; đúng nhưng ngoài bảng; câu ghép/mệnh lệnh chứa kết quả; hai lần regen rồi câu hỏi nghiêm ngặt vẫn phải kiểm. |
| Memory và nén | 4 | Sửa ý cũ; vượt ngân sách và mở lại phiên; nén lỗi/đồng thời; cách ly hai học sinh/assignment và định danh trong summary. |
| Streaming và sự cố | 4 | UTF-8/LaTeX cắt giữa chunk; cancel tranh chấp với delta; ngắt mạng/restart/resume không trùng; GLM 429/timeout/CAS lỗi hoặc đổi mode/hết hạn/bảng trong khi sinh. |
| **Tổng** | **30** | Kịch bản có checkpoint đối kháng; mỗi ca thuộc nhóm chính, có thể phủ nhiều điều kiện. |

- **Leaks = 0** trên toàn bộ câu đã phát, kết quả DOM/Markdown và transcript cộng dồn từng phiên, kể cả trước hủy/lỗi/resume. Dùng bộ lọc xác định với dữ kiện bảo vệ chỉ ở môi trường đánh giá và rà sư phạm độc lập; không dùng chính GLM làm người chấm duy nhất. Chỉ một lần lộ cũng không đạt.
- **TTFT phía học sinh:** từ server nhận yêu cầu đã xác thực tới client nhận **delta đầu tiên đã kiểm**, không tính start/heartbeat. Ghi thêm thời gian tới token đầu của nhà để tách trễ sinh và trễ cổng; báo p50/p95 theo cấp, nén/không nén, lạnh/ấm. Chưa chốt ngưỡng mili giây khi chưa có baseline thật.
- **Tỷ lệ regen:** số lượt có ≥1 regen / số lượt model đã bắt đầu sinh; ghi riêng tỷ lệ câu bị chặn, số lần sinh lại trung bình, lần strict, lỗi cổng và lượt không có lời đạt. Mục tiêu thăm dò regen ≤20%, không đổi cổng để đạt số; đo riêng retry HTTP, token và chi phí cho mọi lần sinh/nén.
- Chất lượng: reviewer kiểm đúng bước, có câu hỏi hữu ích, không lặp máy móc, không xác nhận điều chưa chấm, giữ sửa đổi sau nén. Đo thời gian hoàn tất, thời gian ACK hủy và sự kiện trùng/thiếu; tiêu chí giao thức là 0 câu chưa kiểm được phát, 0 lặp delta trong DOM.
- Kiểm tất định bổ sung: parser mọi cách cắt byte/CRLF/JSON, seq replay, idempotency, cancel, quyền và phiên bản; Testcontainers cho V14/lưu summary/restart; component/e2e Markdown+KaTeX, bàn phím và hai cỡ màn. Mock dùng cho lỗi vận chuyển, không thay 30 ca GLM thật hoặc cổng toán thật.
- Khi có mã: chạy core `./mvnw verify`, frontend build/test/e2e và cổng FLAT; math/eval đúng vùng nếu đổi hợp đồng cổng. Báo ngày, SHA mã/prompt/model/corpus, cấu hình, lệnh, số ca thực chạy, leaks, TTFT và regen trong bằng chứng nghiệm thu. **Phiên tài liệu này chưa chạy GLM eval và không tuyên bố đạt các chỉ số.**
