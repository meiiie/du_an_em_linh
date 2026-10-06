# Changelog

Mọi thay đổi đáng chú ý của sản phẩm được ghi ở đây.

Định dạng theo [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
phiên bản theo [Semantic Versioning](https://semver.org/spec/v2.0.0.html).
Phát hành tự động: [release-please](https://github.com/googleapis/release-please) đọc [Conventional Commits](https://www.conventionalcommits.org/).

## [0.2.0](https://github.com/meiiie/du_an_em_linh/compare/v0.1.0...v0.2.0) (2026-10-06)


### Features

* badge [n] trên phiếu, benchmark SOTA 12 tiêu chí ([9960f30](https://github.com/meiiie/du_an_em_linh/commit/9960f30974542b7e8b21885d1774b8fdf1fef56e))
* **bảo mật:** F-08 phân quyền theo lớp + RLS, F-05 test, F-10 mã hoá khoá / khoá đăng nhập / hạn mức ([#35](https://github.com/meiiie/du_an_em_linh/issues/35)) ([16ff958](https://github.com/meiiie/du_an_em_linh/commit/16ff9587928aec7c8f0c2c0f10000faf1cfd7f76))
* chau chuốt đăng nhập hai bước ([025c865](https://github.com/meiiie/du_an_em_linh/commit/025c86535847b7f9a92e569171e79a0f294eb55f))
* chia lộ trình học sinh thành phiếu và sổ ([3b10b63](https://github.com/meiiie/du_an_em_linh/commit/3b10b63f7dab4ada80f8e859a10127981dd4f85d))
* **core:** API học sinh /api/hs/bai (danh sách, đề và bài làm, nộp bước, nộp bài) + giao bài thử bằng SQL (T021) ([#148](https://github.com/meiiie/du_an_em_linh/issues/148)) ([ebbf0f9](https://github.com/meiiie/du_an_em_linh/commit/ebbf0f94e507d5e59df5dd9a33eb39eacc135375))
* **core:** client dịch vụ toán đóng mặc định ([#102](https://github.com/meiiie/du_an_em_linh/issues/102)) ([76634e5](https://github.com/meiiie/du_an_em_linh/commit/76634e53bb05687bed1ecdf440646d0079a4a80c))
* **core:** content — cổng đọc đề cho học sinh không có lời giải, luật ArchUnit cho DTO học sinh (T015, một phần [#85](https://github.com/meiiie/du_an_em_linh/issues/85)) ([#133](https://github.com/meiiie/du_an_em_linh/issues/133)) ([8268044](https://github.com/meiiie/du_an_em_linh/commit/8268044167ba2a9035167a6e9114fde1f1a44f09))
* **core:** content — importer nội dung chung từ data/supham và data/v0 (T012a, một phần [#85](https://github.com/meiiie/du_an_em_linh/issues/85)) ([#122](https://github.com/meiiie/du_an_em_linh/issues/122)) ([1536ec4](https://github.com/meiiie/du_an_em_linh/commit/1536ec47f4edd70b58a312e9d5d9efe24452c17e))
* **core:** content — nhập nội dung theo lớp: tài liệu, khóa bảng, kiểm 3 tầng, phát hành (T012b, một phần [#85](https://github.com/meiiie/du_an_em_linh/issues/85)) ([#134](https://github.com/meiiie/du_an_em_linh/issues/134)) ([4d43cb1](https://github.com/meiiie/du_an_em_linh/commit/4d43cb1ba696e5b1c84f316ea0e6758403818054))
* **core:** content — persistence bảng công thức, lượt kiểm, duyệt, phát hành; V6 lượt kiểm mới nhất ([#85](https://github.com/meiiie/du_an_em_linh/issues/85)) ([#121](https://github.com/meiiie/du_an_em_linh/issues/121)) ([592850b](https://github.com/meiiie/du_an_em_linh/commit/592850b90a021b6371720839624f1b270237eccc))
* **core:** content — persistence JdbcClient cho danh mục, bài, lời giải, thang gợi ý, tài liệu; căn cứ bất biến và phiên bản nội dung ([#120](https://github.com/meiiie/du_an_em_linh/issues/120)) ([6d34484](https://github.com/meiiie/du_an_em_linh/commit/6d34484d36712ce2371b41a1bfb84a68cb37fa86))
* **core:** giới hạn đăng nhập sai (F-10) như v0 ([#74](https://github.com/meiiie/du_an_em_linh/issues/74)) ([60d90e3](https://github.com/meiiie/du_an_em_linh/commit/60d90e3d723dcfba3a67eb24d89dbb1c9b641c0c))
* **core:** module classroom — lớp, ghi danh, cài lớp, cảnh báo cho giáo viên ([#109](https://github.com/meiiie/du_an_em_linh/issues/109)) ([0ce3749](https://github.com/meiiie/du_an_em_linh/commit/0ce3749c9d063f1c0554770e250e23cd6cafccaa))
* **core:** module content — V4__content và domain cổng 3 tầng, phát hành theo lớp, bảng công thức có phiên bản ([#115](https://github.com/meiiie/du_an_em_linh/issues/115)) ([8f4e3ca](https://github.com/meiiie/du_an_em_linh/commit/8f4e3ca5c3a8bd917f7b23eb5867907a896eda44))
* **core:** module mastery — mức hiểu BKT như v0 sau khi nộp bài, GET /api/hs/trang-hoc, cổng mức của lớp cho giáo viên (T049–T052) ([#152](https://github.com/meiiie/du_an_em_linh/issues/152)) ([d8ea096](https://github.com/meiiie/du_an_em_linh/commit/d8ea0962bee6f3828a1e98ccdc57e8cccf51946c))
* **core:** port identity từ LMS — JWT, phiên đăng nhập, 4 vai trò ([#68](https://github.com/meiiie/du_an_em_linh/issues/68)) ([9fbb9c1](https://github.com/meiiie/du_an_em_linh/commit/9fbb9c1d3623decff038d0e461e9d2e153f12366))
* **core:** practice — nộp bài ghim căn cứ, mở lời giải sau khi nộp qua một cửa (T020, một phần [#87](https://github.com/meiiie/du_an_em_linh/issues/87)) ([#142](https://github.com/meiiie/du_an_em_linh/issues/142)) ([c8bbb60](https://github.com/meiiie/du_an_em_linh/commit/c8bbb60e22a8c7e847980bd66186bfd15f784ba6))
* **core:** practice — nộp bước và chấm qua /v1/grade như v0 (T020, một phần [#87](https://github.com/meiiie/du_an_em_linh/issues/87)) ([#140](https://github.com/meiiie/du_an_em_linh/issues/140)) ([467ca38](https://github.com/meiiie/du_an_em_linh/commit/467ca386f131ef33b45d37a1f75344d7b587ddfe))
* **core:** practice — V7 bài làm, bước, bảng xét dấu, kết quả chấm; model và lưu trữ (T019, một phần [#87](https://github.com/meiiie/du_an_em_linh/issues/87)) ([#136](https://github.com/meiiie/du_an_em_linh/issues/136)) ([6e2df18](https://github.com/meiiie/du_an_em_linh/commit/6e2df188af6950345309b06fbb67dcfaf35d4be8))
* **core:** refresh token qua cookie HttpOnly cho SPA ([#72](https://github.com/meiiie/du_an_em_linh/issues/72)) ([2b37fb8](https://github.com/meiiie/du_an_em_linh/commit/2b37fb8b8f24b785f79dfaf3f06898cedab7b27a))
* cột gia sư SSE trạng thái, Dừng abort thật ([9358c09](https://github.com/meiiie/du_an_em_linh/commit/9358c09a3f2e42450d69e2a53d9f4dfcd9cdd292))
* dán khóa lập trình OpenRouter và khóa coding Z.AI ([6dc7505](https://github.com/meiiie/du_an_em_linh/commit/6dc7505c6a29210d9ca87617f32dd5c0cc78b4fb))
* đặt tên mục học sinh theo việc thật ([8c7933a](https://github.com/meiiie/du_an_em_linh/commit/8c7933a35785a65f2daee463fafb4d8b423fe3c0))
* đo độ chính xác 5 bước + UX điện thoại, đẩy bản online ([#21](https://github.com/meiiie/du_an_em_linh/issues/21)) ([6510fa3](https://github.com/meiiie/du_an_em_linh/commit/6510fa384285b456e17ba01ca37c171c3fb220df))
* **frontend:** đăng nhập hai cột, trang Học, Đề bài và màn Luyện bài năm bước trên API thật ([#149](https://github.com/meiiie/du_an_em_linh/issues/149)) ([fafc6d3](https://github.com/meiiie/du_an_em_linh/commit/fafc6d32f8fc775acc336f0db42a381072671dec))
* **frontend:** đăng nhập thật, guard vai trò, khung /hs /gv ([#73](https://github.com/meiiie/du_an_em_linh/issues/73)) ([10a1551](https://github.com/meiiie/du_an_em_linh/commit/10a15515f0b2349836479fac5f870d1b615453c3))
* **frontend:** giao diện kiểu Wiii pha bảng toán 3b1b — sáng / tối, phông hệ thống, khung 260 px (A + B) ([#146](https://github.com/meiiie/du_an_em_linh/issues/146)) ([896b67b](https://github.com/meiiie/du_an_em_linh/commit/896b67b36288ab302cf88c358560287d2e8679fd))
* **frontend:** khung P2 — ray mực như v0, ngăn kéo hộp thoại, KaTeX, ô công thức MathLive, route theo vai trò ([#112](https://github.com/meiiie/du_an_em_linh/issues/112)) ([2083a33](https://github.com/meiiie/du_an_em_linh/commit/2083a33be42fe3db46fe3099127207bea4e48ed8))
* **frontend:** trang công khai / theo trang Wiii của meiiie-design-kit, bảng toán 3b1b ([#147](https://github.com/meiiie/du_an_em_linh/issues/147)) ([23c16eb](https://github.com/meiiie/du_an_em_linh/commit/23c16ebf3c38f411c0a8a77027f7ffe4d2834bb3))
* **goi-y:** thang gợi ý mẫu Sư phạm (52 thang) theo (bước, loại kết quả); cấp trống → bài dễ hơn / Gửi thầy cô ([#40](https://github.com/meiiie/du_an_em_linh/issues/40)) ([d8bc163](https://github.com/meiiie/du_an_em_linh/commit/d8bc16355505d0b89c9a6b90703c2af2f31e5bc1))
* harness gia sư SOTA — không gọi lại SSE, Escape, Hỏi lại ([e299833](https://github.com/meiiie/du_an_em_linh/commit/e2998339d845bbea23f23be547c1aab23a1ec84a))
* **khung-ngan:** 8 bài khung ngắn NB/TH, buoc_bat_dau, toan_dung, câu nhận dấu U ([#38](https://github.com/meiiie/du_an_em_linh/issues/38)) ([b11fe4b](https://github.com/meiiie/du_an_em_linh/commit/b11fe4b2c53881a801b089abc0099fed3ff55bf8))
* letterhead gọn và tab sổ gạch chân ([eb2a030](https://github.com/meiiie/du_an_em_linh/commit/eb2a0308e20a4b16b1f208f7a7d3b948a5078546))
* Lịch học dạng sổ tuần — gáy 7 ngày và phiếu buổi ([4d29e0f](https://github.com/meiiie/du_an_em_linh/commit/4d29e0f592c185d51d52e87f7c2b07744d97d0d7))
* Lịch học là bảng tuần T2–CN theo giờ ([116f4ca](https://github.com/meiiie/du_an_em_linh/commit/116f4ca73c0b8418a5e35153f83a00c11a044e72))
* lời gia sư Markdown + KaTeX như phiếu ([f1d4d84](https://github.com/meiiie/du_an_em_linh/commit/f1d4d84e07c5329e5ce5ed67a844ad089a6697e5))
* **math:** job kiem_loi_giang — cổng thế giới đóng cho câu gia sư theo ADR 013 (T028, T030) ([#151](https://github.com/meiiie/du_an_em_linh/issues/151)) ([008b499](https://github.com/meiiie/du_an_em_linh/commit/008b499d60c7986c4ca8c9b5cae7c86e71a833d1))
* **math:** job kiem-dong-cong-thuc kiểm dòng bảng công thức khi khóa (ADR 013) ([#101](https://github.com/meiiie/du_an_em_linh/issues/101)) ([07f05c2](https://github.com/meiiie/du_an_em_linh/commit/07f05c21441ad3b8ed3f15f4f296555a2dc70407))
* **seed:** tài khoản chỉ dùng cho test (hs.build, khoa.test F-10, lớp 12B F-08 d) ([#45](https://github.com/meiiie/du_an_em_linh/issues/45)) ([abbda49](https://github.com/meiiie/du_an_em_linh/commit/abbda49d7cc4df088d63d650bb7277e94ec83ed6))
* trang làm bài là một trang toán ([#25](https://github.com/meiiie/du_an_em_linh/issues/25)) ([6eb8b06](https://github.com/meiiie/du_an_em_linh/commit/6eb8b064dedb803fa271aa30ffc39d23b93d9f44))
* trích dẫn cùng [n], chip gọn, nhớ tài liệu vừa mở ([b172595](https://github.com/meiiie/du_an_em_linh/commit/b172595593d2900bc772e33c28249b99b1deafc4))
* trích dẫn gia sư có đoạn, neo kho, nhớ trên tin ([a02aaf0](https://github.com/meiiie/du_an_em_linh/commit/a02aaf0b39de44d08d2ef870f40183597bb4149a))
* **ux-07/09:** kẹt theo bước → Gửi thầy cô; cảnh báo GV gắn bài/bước + Đã xử lý ([#46](https://github.com/meiiie/du_an_em_linh/issues/46)) ([acde866](https://github.com/meiiie/du_an_em_linh/commit/acde8660070fd390280d91da9f32bd809760054c))
* **ux-lan7:** lưu tiến trình, nháp, bảng xét dấu truy cập được, chỉ báo cấp gợi ý, MathLive ô cực trị ([#42](https://github.com/meiiie/du_an_em_linh/issues/42)) ([1bb2e52](https://github.com/meiiie/du_an_em_linh/commit/1bb2e5223e11745e3d0159be8898ad2a6707945e))
* Việt Hóa chữ học sinh, cắt từ thừa ([25035df](https://github.com/meiiie/du_an_em_linh/commit/25035dfef0a7ad80ac6189ea8e41cf9396520c10))
* Việt Hóa GV, bảng Lịch, dán khóa OpenRouter và Z.AI ([0de03b5](https://github.com/meiiie/du_an_em_linh/commit/0de03b54ea1054f77668b6e17f59a4fd01797972))
* Việt Hóa mặt giáo viên và làm rõ lỗi đăng nhập thử ([dbd9c5b](https://github.com/meiiie/du_an_em_linh/commit/dbd9c5bb8926ff579a49eb0483c8ca7c270fcef0))


### Bug Fixes

* canh KaTeX trái phiếu và đánh dấu kỹ năng yếu ([00ebe32](https://github.com/meiiie/du_an_em_linh/commit/00ebe32b29695b8479d44c17e599f41040218844))
* cắt chữ và UI không có việc trên mặt học sinh ([421335b](https://github.com/meiiie/du_an_em_linh/commit/421335ba6800908059a08a61acb2b89c954919a4))
* CD chỉ xanh khi sức khỏe đúng commit Render ([#24](https://github.com/meiiie/du_an_em_linh/issues/24)) ([2cdfafd](https://github.com/meiiie/du_an_em_linh/commit/2cdfafd73f15ae8fd14c9f28344a12efdad083f7))
* chấm v1.3, sư phạm SP-01..06, gia sư AI-1..5, bảo mật F-01 từng phần ([#28](https://github.com/meiiie/du_an_em_linh/issues/28)) ([4e4af19](https://github.com/meiiie/du_an_em_linh/commit/4e4af19b37d6f840c08e98150b32cf75fbaeeb3b))
* cho Postgres nhận nhà Z.AI/OpenRouter, mặc định glm-5.3-flashx ([3fd9450](https://github.com/meiiie/du_an_em_linh/commit/3fd945021e964efd52a484808e9a185853185556))
* chờ tab bài giao trước khi bấm hàng bài ([f02f87c](https://github.com/meiiie/du_an_em_linh/commit/f02f87c675cb86986f8fe0715678c0860de4b1fa))
* Công thức một cột, tab gạch, bỏ hộp xám và cột lặp ([ae22aa2](https://github.com/meiiie/du_an_em_linh/commit/ae22aa23afc2baa6270e153fcca4c6bee45a65ad))
* cột bảng Lịch rộng đều, ngày trống vẫn giữ ô ([d5d554d](https://github.com/meiiie/du_an_em_linh/commit/d5d554d3f56b14ef58c853732094177310c2a4f1))
* cuộn tới Đã đọc, kẻ nguồn dưới lời gia sư ([b846272](https://github.com/meiiie/du_an_em_linh/commit/b846272bdf8b972884d892208b0c3235a23effd2))
* Đã đọc bám tin nguồn cuối, không mất khi lượt lỗi ([8af0079](https://github.com/meiiie/du_an_em_linh/commit/8af0079b272257e485ab82dbfb387015af96ae94))
* duyệt bỏ chữ Tầng, lịch một lưới giờ ([ab540a9](https://github.com/meiiie/du_an_em_linh/commit/ab540a9be689fa8c33144454b99f14e2c8d7f0da))
* duyệt chỉ hiện tên công thức, cắt đoạn giữa từ ([44461a7](https://github.com/meiiie/du_an_em_linh/commit/44461a7ed8d29777b5a91910b89f51efb596e5a1))
* e2e Kho bắt heading Công thức và tài liệu ([7705f1a](https://github.com/meiiie/du_an_em_linh/commit/7705f1adfaf988ce0cfc6194dae77f37e4643d7b))
* **f-10:** thông báo khóa đăng nhập rõ ràng ([#47](https://github.com/meiiie/du_an_em_linh/issues/47)) ([1b9b8f5](https://github.com/meiiie/du_an_em_linh/commit/1b9b8f500ce9411c171e0dbd166620b8e11bae0a))
* gáy tuần Lịch gọn như sổ, chấm buổi đủ đọc ([7485721](https://github.com/meiiie/du_an_em_linh/commit/74857216617e688341b5d3b6721ecd8445302c46))
* giữ f′ trên phiếu và cắt chữ cổng kho khớp ([caa074d](https://github.com/meiiie/du_an_em_linh/commit/caa074d48e2e091d9f61fd20f3e620b9b4514a27))
* giữ số bài giao trên một dòng tab ([bb17d9a](https://github.com/meiiie/du_an_em_linh/commit/bb17d9abf7b3f91aca490cbe8002834620833f64))
* gỡ lời chào gia sư, che khóa API, quét khóa trong git ([#27](https://github.com/meiiie/du_an_em_linh/issues/27)) ([b457090](https://github.com/meiiie/du_an_em_linh/commit/b457090d0fa47bd66dcf554ed59e52d23890d04d))
* gợi ý gia sư thành phiếu — đoạn, công thức $, không tường chữ ([95595be](https://github.com/meiiie/du_an_em_linh/commit/95595bec1991a88253914456dfab6cdf253a5c22))
* KaTeX công thức canh trái như phiếu, trích ngắn hơn ([94e3eef](https://github.com/meiiie/du_an_em_linh/commit/94e3eefcae163f52fd55b1f640c209f921d23691))
* **kd-0002c:** luật dấu U + toan_dung, buoc_bat_dau (bản dựng lại trên be60d79) ([#36](https://github.com/meiiie/du_an_em_linh/issues/36)) ([b58a6ee](https://github.com/meiiie/du_an_em_linh/commit/b58a6ee3e96e1719145d493b5560f9868e5d7874))
* **kd-0002d:** chỉ số SGK x_CT / x_{CĐ} / x_1 không bị coi là code ([#37](https://github.com/meiiie/du_an_em_linh/issues/37)) ([5874faf](https://github.com/meiiie/du_an_em_linh/commit/5874fafa153943c39e9c1470ffb46c5a6a4c947e))
* **kd-0002e:** ô cực trị chỉ tung độ → ERR.DH.11 / ERR.DH.22 (bản vá Kiểm định nguyên văn) ([#39](https://github.com/meiiie/du_an_em_linh/issues/39)) ([c129c91](https://github.com/meiiie/du_an_em_linh/commit/c129c91c429fbdfdfde46d1ff9dbc397e468445c))
* **kd-0002:** su_kien_bao_ve giữ dấu +, fmt_domain R \ {a}, lớp lọc chuỗi độc hại ([#34](https://github.com/meiiie/du_an_em_linh/issues/34)) ([be60d79](https://github.com/meiiie/du_an_em_linh/commit/be60d79da1664765707e39386c4291f8c91e6676))
* **kd-0003:** lọc câu điều kiện / điểm thử / phủ định — bản vá Kiểm định nguyên văn ([#41](https://github.com/meiiie/du_an_em_linh/issues/41)) ([cb57afd](https://github.com/meiiie/du_an_em_linh/commit/cb57afd82372d5cc9d7efa3ad452c530aa137939))
* **kd-0004b:** ngoại lệ trích nguyên văn câu học sinh trong bộ lọc (bản vá Kiểm định nguyên văn) ([#51](https://github.com/meiiie/du_an_em_linh/issues/51)) ([3a91f46](https://github.com/meiiie/du_an_em_linh/commit/3a91f4686bf0cceb746945c2ba3e74cedae833da))
* **kd-0004:** lọc câu viết biểu thức y′ bằng đạo hàm thật (bản vá Kiểm định nguyên văn) ([#49](https://github.com/meiiie/du_an_em_linh/issues/49)) ([4f7a886](https://github.com/meiiie/du_an_em_linh/commit/4f7a886deb2e414983d8b8e0b19cd5155d1a00fa))
* khóa lớp theo đúng nhà, timeout 30s, không redact giáo viên ([ebb72f6](https://github.com/meiiie/du_an_em_linh/commit/ebb72f6e5dad0ec13874fc40725f1513be2173c3))
* không hiện mã bước trên lời gia sư ([8a2fae2](https://github.com/meiiie/du_an_em_linh/commit/8a2fae2da17849e17b9106c7eed3505782b5cc69))
* Lịch chỉ giữ một lời cách học ([1b33549](https://github.com/meiiie/du_an_em_linh/commit/1b33549f2c990a1d16e5e3bcfee78d860f36338f))
* lọc gia sư dùng M3, không chặn «giảm số mũ đi 1» ([0c40874](https://github.com/meiiie/du_an_em_linh/commit/0c408742a4744fd1c397f28d4cda1b9db09a848b))
* **mathlive:** đặt vùng chọn vào ô nhận phím sau khi chạm (UXT-01-b) ([#44](https://github.com/meiiie/du_an_em_linh/issues/44)) ([536661e](https://github.com/meiiie/du_an_em_linh/commit/536661e0d5c147457bff6f9b5a0eded0d65120ee))
* **math:** sandbox dùng một hạn chót tuyệt đối cho chờ suất, chạy và dọn job ([#106](https://github.com/meiiie/du_an_em_linh/issues/106)) ([de4bc20](https://github.com/meiiie/du_an_em_linh/commit/de4bc20f53b545fb5cb8a2b070318b662069319e))
* phiếu Công thức — KaTeX lớn, gáy bước, tài liệu một câu ([dfd03f1](https://github.com/meiiie/du_an_em_linh/commit/dfd03f14d4a4ea06ca167cf79535c881ab47f9c3))
* phiếu sạch hơn — công thức là KaTeX, gỡ khối chữ thừa ([a30df51](https://github.com/meiiie/du_an_em_linh/commit/a30df512bfa077b3fcfb039ecd5065d1185dea21))
* serve a public home page and complete CD plus SEO ([85824c6](https://github.com/meiiie/du_an_em_linh/commit/85824c6145f1995b246dcc98d1922d4f6c41f4de))
* **sp-sua-thang-0001:** cấp 3 xét dấu không nêu điểm thử cụ thể — bản vá Sư phạm nguyên văn ([#43](https://github.com/meiiie/du_an_em_linh/issues/43)) ([3f9a09f](https://github.com/meiiie/du_an_em_linh/commit/3f9a09f3853b5d553a64bcfc8e8514d1990b4425))
* tài liệu trên Công thức lấy câu có việc, không câu khung ([21725cc](https://github.com/meiiie/du_an_em_linh/commit/21725cca9f0c3786e67c54cea46f56d20e353606))
* thanh Nộp+Hỏi trên điện thoại, Z.AI thinking enabled+low ([2c39dec](https://github.com/meiiie/du_an_em_linh/commit/2c39dec0cf09d68221b773e969464093211a6780))
* tờ gia sư full màn trên điện thoại, Z.AI tắt thinking ([6351931](https://github.com/meiiie/du_an_em_linh/commit/6351931766426363f25fc134711b2e271d027212))
* **ux-03/04/05/06/10, ai-5:** bảng xét dấu chỉ ô sai + nút quay lại bước; đủ lỗi kết luận (HS thu gọn, GV xem đủ); phiếu việc tiếp trỏ bài đang dở; logo 44px; cấp gợi ý k/n thật sau tải lại ([#50](https://github.com/meiiie/du_an_em_linh/issues/50)) ([20c3393](https://github.com/meiiie/du_an_em_linh/commit/20c33935befa06d723421bd7a6d442976afd3d28))

## [Unreleased]

### Added

- Logo tab: SVG, ICO 16/32/48, PNG 48/96/180/192/512 và bản maskable (Google Search favicon, 2026-08-28)
- 404, `llms.txt`, `security.txt`, JSON-LD Organization / WebSite / WebApplication (không rating giả, không FAQPage)
- Đăng nhập 2 bước (email → mật khẩu), hiện/ẩn mật khẩu, căn giữa theo cổng Neko Đoàn

## [0.1.0] - 2026-09-27

Nguyên mẫu NCKH đầu tiên — một chủ đề Toán 12 (đơn điệu và cực trị).

### Added

- Phiếu học sinh 5 bước, cổng kiểm định 3 tầng, gia sư không đưa đáp án
- Kho lớp + kết nối ChatGPT bằng khóa API chính thức; Ollama / LM Studio loopback
- Deploy Render free, CI GitHub Actions, ping giữ thức
- Tài khoản thử tổng hợp (không học sinh thật)

[unreleased]: https://github.com/meiiie/du_an_em_linh/compare/v0.1.0...HEAD
[0.1.0]: https://github.com/meiiie/du_an_em_linh/releases/tag/v0.1.0
