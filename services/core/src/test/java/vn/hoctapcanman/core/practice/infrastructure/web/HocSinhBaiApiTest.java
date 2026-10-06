package vn.hoctapcanman.core.practice.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import org.testcontainers.junit.jupiter.Testcontainers;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import vn.hoctapcanman.core.TestcontainersConfiguration;
import vn.hoctapcanman.core.identity.application.port.AccessTokenIssuer;
import vn.hoctapcanman.core.identity.domain.model.Email;
import vn.hoctapcanman.core.identity.domain.model.Role;
import vn.hoctapcanman.core.identity.domain.model.User;
import vn.hoctapcanman.core.identity.domain.model.UserId;
import vn.hoctapcanman.core.practice.application.port.MayCham;
import vn.hoctapcanman.core.practice.infrastructure.persistence.DuLieuPractice;

/**
 * API học sinh làm bài qua HTTP thật (T021): access token thật, PostgreSQL 18, use case thật; chỉ dịch vụ toán thay bằng máy
 * chấm thử ({@link MayChamThu}: bước có {@code SAI_O_DAY} thì sai, có {@code LOI_MAY} thì không trả lời, còn lại đạt). So cả
 * thân JSON: trường hợp đồng ghi {@code ?} phải vắng mặt, trường «hay null» phải có mặt.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, HocSinhBaiApiTest.CauHinhMayCham.class})
@Testcontainers(disabledWithoutDocker = true)
class HocSinhBaiApiTest {

    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final String BI_MAT = "BIMAT";
    private static final String DE = "Xét tính đơn điệu của y = x^3 - 3x^2 + 2.";
    private static final String KHUNG = """
            [{"maBuoc":"B.DH.TXD","ten":"Tập xác định","viec":"Viết tập xác định của hàm số"},
             {"maBuoc":"B.DH.DAOHAM","ten":"Đạo hàm","viec":"Tính đạo hàm của hàm số"},
             {"maBuoc":"B.DH.NGHIEM","ten":"Nghiệm y′","viec":"Tìm nghiệm y′ = 0 và điểm y′ không xác định"},
             {"maBuoc":"B.DH.XETDAU","ten":"Xét dấu","viec":"Xét dấu y′ và chiều biến thiên"},
             {"maBuoc":"B.DH.KETLUAN","ten":"Kết luận","viec":"Kết luận khoảng đơn điệu và cực trị"}]""";
    private static final String KHONG_CO_BAI = "Bài chưa mở hoặc không chấm được.";
    /** Bài đầu tiên của em, đạt, ở bài mức Thông hiểu: BKT của v0 đưa mastery 0,3 lên 0,6995 và mức lên đúng một nấc. */
    private static final String NOP_DAT_LEN_THONG_HIEU = """
            {"ketQua":"DAT","mucHieu":[{"kyNang":"T12.DH.02","muc4Truoc":"NHAN_BIET","muc4Sau":"THONG_HIEU"}]}""";
    private static final String TRANG_HOC_TRONG = """
            {"ten":"Người thử","soKyNang":[],"hoanThanh":{"kyNang":[],"chuDe":[]}}""";

    @Autowired
    private MockMvcTester mvc;

    @Autowired
    private AccessTokenIssuer tokens;

    @Autowired
    private JdbcClient jdbc;

    @Autowired
    private MayChamThu mayCham;

    private UUID lop;
    private UUID lopKhac;
    private UUID an;
    private UUID binh;
    private UUID p1;
    private String ma1;
    private String ma2;
    private String ma3;
    private String maChua;
    private String maKhac;
    private String maRut;
    private final List<UUID> nguoi = new ArrayList<>();
    private final List<UUID> bai = new ArrayList<>();
    private final Map<UUID, Role> vaiTro = new LinkedHashMap<>();

    @BeforeEach
    void duLieu() {
        mayCham.yeuCau.clear();
        DuLieuPractice.danhMuc(jdbc);
        lop = DuLieuPractice.lop(jdbc);
        lopKhac = DuLieuPractice.lop(jdbc);
        an = nguoiMoi(Role.STUDENT);
        binh = nguoiMoi(Role.STUDENT);
        UUID dung = nguoiMoi(Role.STUDENT);
        UUID giaoVien = nguoiMoi(Role.TEACHER);
        DuLieuPractice.ghiDanh(jdbc, lop, an, "STUDENT");
        DuLieuPractice.ghiDanh(jdbc, lop, binh, "STUDENT");
        DuLieuPractice.ghiDanh(jdbc, lop, giaoVien, "TEACHER");
        DuLieuPractice.ghiDanh(jdbc, lopKhac, dung, "STUDENT");

        p1 = baiMoi("NB");
        jdbc.sql("""
                insert into solutions (problem_id, worked_solution, protected_facts, final_answer)
                values (?, cast(? as jsonb), '["BIMAT_DU_KIEN"]', 'BIMAT_DAP_AN')""")
            .params(p1, """
                {"TXD": "D = R BIMAT", "ket_luan": {"dong_bien": ["(-oo; 0)", "(2; +oo)"], "nghich_bien": ["(0; 2)"]}}""")
            .update();
        DuLieuPractice.phatHanh(jdbc, lop, p1);
        ma1 = ma(p1);
        UUID p2 = baiMoi("TH");
        DuLieuPractice.phatHanh(jdbc, lop, p2);
        ma2 = ma(p2);
        DuLieuPractice.BaiDaGhi p3 = DuLieuPractice.baiV0(jdbc, "VD", "T12.DH.02", "VAN_DUNG", "Tìm cực trị của hàm số y = x^3 - 3x.",
            "y = x^3 - 3x", "x**3 - 3*x", "TU_LUAN_5_BUOC", "B.DH.NGHIEM", "SUPHAM");
        bai.add(p3.id());
        DuLieuPractice.phatHanh(jdbc, lop, p3.id());
        ma3 = p3.ma();
        maChua = ma(baiMoi("CHUA"));
        UUID pKhac = baiMoi("KHAC");
        DuLieuPractice.phatHanh(jdbc, lopKhac, pKhac);
        maKhac = ma(pKhac);
        UUID pRut = baiMoi("RUT");
        DuLieuPractice.phatHanh(jdbc, lop, pRut);
        maRut = ma(pRut);

        giao(lop, p1, an, "2026-10-06T08:00:00Z", "2026-10-13T08:00:00Z");
        giao(lop, p2, an, "2026-10-05T08:00:00Z", null);
        giao(lop, pRut, an, "2026-10-04T08:00:00Z", null);
        giao(lop, p1, binh, "2026-10-06T08:00:00Z", null);
        giao(lopKhac, pKhac, dung, "2026-10-06T08:00:00Z", null);
        // Sửa đề sau khi giao: phiên bản nội dung tăng, phát hành ở mọi lớp về NHAP (V5), bài thôi hiện với học sinh.
        jdbc.sql("update problems set content_hash = ? where id = ?").params("b".repeat(64), pRut).update();
    }

    @AfterEach
    void don() {
        jdbc.sql("delete from classes where id in (?, ?)").params(lop, lopKhac).update();
        bai.forEach(id -> jdbc.sql("delete from problems where id = ?").params(id).update());
        nguoi.forEach(id -> jdbc.sql("delete from users where id = ?").params(id).update());
    }

    @Test
    void danhSachChiCoBaiDuocGiaoChoEmMaDangPhatHanhTheoLucGiao() {
        assertJson(get("/api/hs/bai", an), """
            [%s, %s]""".formatted(dongDanhSach(ma2, null, "CHUA_LAM", 0, null), dongDanhSach(ma1, "\"2026-10-13T08:00:00Z\"", "CHUA_LAM", 0, null)));
        assertJson(get("/api/hs/bai", binh), "[%s]".formatted(dongDanhSach(ma1, null, "CHUA_LAM", 0, null)));
        UUID chuaGhiDanh = nguoiMoi(Role.STUDENT);
        assertJson(get("/api/hs/bai", chuaGhiDanh), "[]");
        assertThat(get("/api/hs/bai/" + ma1, chuaGhiDanh).status()).isEqualTo(404);
    }

    @Test
    void trangThaiSoBuocDatVaKetQuaTheoBaiLamThat() {
        assertThat(nopBuoc(ma1, dong("B.DH.TXD", "D = R", null)).status()).isEqualTo(200);
        assertJson(nopBuoc(ma1, dong("B.DH.DAOHAM", "y' = SAI_O_DAY", null)), """
            {"ketQua":"SAI","thongBao":"Chưa đúng ở dòng này.","oSai":[{"maBuoc":"B.DH.DAOHAM","dong":0}],"maLoi":"ERR.DH.01"}""");
        assertJson(get("/api/hs/bai", an), "[%s, %s]".formatted(dongDanhSach(ma2, null, "CHUA_LAM", 0, null),
            dongDanhSach(ma1, "\"2026-10-13T08:00:00Z\"", "DANG_LAM", 1, null)));

        assertJson(nopBuoc(ma1, dong("B.DH.DAOHAM", "y' = 3x^2 - 6x", null)), """
            {"ketQua":"DAT","thongBao":"Đúng rồi.","oSai":[],"buocKe":"B.DH.NGHIEM"}""");
        lamTiepToiKetLuan(ma1);
        assertThat(soBuocDat(ma1)).isEqualTo(5);
        assertJson(nopBai(ma1), NOP_DAT_LEN_THONG_HIEU);
        assertJson(get("/api/hs/bai", an), "[%s, %s]".formatted(dongDanhSach(ma2, null, "CHUA_LAM", 0, null),
            dongDanhSach(ma1, "\"2026-10-13T08:00:00Z\"", "DA_NOP", 5, "\"DAT\"")));

        // Làm lại sau khi nộp: nộp một bước mở bài làm mới, đang làm, chưa có kết quả.
        nopBuoc(ma1, dong("B.DH.TXD", "D = R", null));
        assertJson(get("/api/hs/bai", an), "[%s, %s]".formatted(dongDanhSach(ma2, null, "CHUA_LAM", 0, null),
            dongDanhSach(ma1, "\"2026-10-13T08:00:00Z\"", "DANG_LAM", 1, null)));
    }

    @Test
    void nopBaiDoiMucHieuMotLanVaTrangHocThayMucMoi() {
        assertJson(get("/api/hs/trang-hoc", an), TRANG_HOC_TRONG);
        nopBuoc(ma1, dong("B.DH.TXD", "D = R", null));
        nopBuoc(ma1, dong("B.DH.DAOHAM", "y' = 3x^2 - 6x", null));
        lamTiepToiKetLuan(ma1);
        assertJson(nopBai(ma1), NOP_DAT_LEN_THONG_HIEU);
        assertJson(nopBai(ma1), NOP_DAT_LEN_THONG_HIEU);
        assertThat(jdbc.sql("select attempts || ' ' || level4 || ' ' || mastery from mastery_states where student_id = ?").params(an)
            .query(String.class).list()).as("gửi lại không tính lần hai").containsExactly("1 THONG_HIEU 0.6995122");
        assertJson(get("/api/hs/trang-hoc", an), """
            {"ten":"Người thử","soKyNang":[{"kyNang":"T12.DH.02","tenKyNang":"Tính đạo hàm","muc4":"THONG_HIEU","ket":false}],
             "hoanThanh":{"kyNang":[],"chuDe":[]}}""");
        assertJson(get("/api/hs/trang-hoc", binh), TRANG_HOC_TRONG);
        UUID gv = nguoiMoi(Role.TEACHER);
        assertThat(get("/api/hs/trang-hoc", gv).status()).isEqualTo(403);
    }

    @Test
    void chiTietCoDeKhungVaBaiLamTheoNoiDungHienTai() {
        assertJson(get("/api/hs/bai/" + ma1, an), chiTiet1("CHUA_LAM", "[]", false));
        // Bài đang phát hành mà chưa giao vẫn xem được; đề hỏi cực trị thì bước kết luận khai thêm cực đại, cực tiểu.
        assertJson(get("/api/hs/bai/" + ma3, an), """
            {"maBai":"%s","de":{"text":"Tìm cực trị của hàm số y = x^3 - 3x.","latex":"y = x^3 - 3x"},"kyNang":"T12.DH.02",
             "tenKyNang":"Tính đạo hàm","muc4":"VAN_DUNG","dangTraLoi":"TU_LUAN_5_BUOC","buocBatDau":"B.DH.NGHIEM",
             "khaiBaoKetLuan":["dong_bien","nghich_bien","cuc_dai","cuc_tieu"],"cacBuoc":%s,
             "baiLam":{"trangThai":"CHUA_LAM","cacBuoc":[]},"coTheMoLoiGiai":false}""".formatted(ma3, KHUNG));

        nopBuoc(ma1, dong("B.DH.TXD", "D = R", null));
        nopBuoc(ma1, dong("B.DH.DAOHAM", "y' = SAI_O_DAY", null));
        assertJson(nopBuoc(ma1, dong("B.DH.NGHIEM", "LOI_MAY", "NGHIEM")), """
            {"ketQua":"KHONG_CHAM_DUOC","thongBao":"Máy chấm đang bận. Em thử lại sau ít giây nhé.","oSai":[]}""");
        nopBuoc(ma1, """
            {"maBuoc":"B.DH.XETDAU","bang":[{"hang":"X","k":0,"giaTri":"0"},{"hang":"DAU_YPHAY","k":2,"giaTri":"+"}]}""");
        String xetDau = """
            {"maBuoc":"B.DH.XETDAU","dong":[],"bang":[{"hang":"X","k":0,"giaTri":"0"},{"hang":"DAU_YPHAY","k":2,"giaTri":"+"}],
             %s}""";
        assertJson(get("/api/hs/bai/" + ma1, an), chiTiet1("DANG_LAM", """
            [{"maBuoc":"B.DH.TXD","dong":[{"dong":0,"latex":"D = R"}],"bang":[],"ketQua":"DAT","thongBao":"Đúng rồi.","oSai":[]},
             {"maBuoc":"B.DH.DAOHAM","dong":[{"dong":0,"latex":"y' = SAI_O_DAY"}],"bang":[],"ketQua":"SAI",
              "thongBao":"Chưa đúng ở dòng này.","oSai":[{"maBuoc":"B.DH.DAOHAM","dong":0}]},
             {"maBuoc":"B.DH.NGHIEM","dong":[{"dong":0,"latex":"LOI_MAY","loai":"NGHIEM"}],"bang":[],"ketQua":"KHONG_CHAM_DUOC",
              "thongBao":"Máy chấm đang bận. Em thử lại sau ít giây nhé.","oSai":[]},
             %s]""".formatted(xetDau.formatted("\"ketQua\":\"DAT\",\"thongBao\":\"Đúng rồi.\",\"oSai\":[]")), false));

        // Sửa bước đầu: lần chấm của các bước sau (yêu cầu chấm gồm cả bước đầu) không còn là của nội dung hiện tại.
        nopBuoc(ma1, dong("B.DH.TXD", "D = ℝ", null));
        assertJson(get("/api/hs/bai/" + ma1, an), chiTiet1("DANG_LAM", """
            [{"maBuoc":"B.DH.TXD","dong":[{"dong":0,"latex":"D = ℝ"}],"bang":[],"ketQua":"DAT","thongBao":"Đúng rồi.","oSai":[]},
             {"maBuoc":"B.DH.DAOHAM","dong":[{"dong":0,"latex":"y' = SAI_O_DAY"}],"bang":[],"ketQua":null,"thongBao":null,"oSai":[]},
             {"maBuoc":"B.DH.NGHIEM","dong":[{"dong":0,"latex":"LOI_MAY","loai":"NGHIEM"}],"bang":[],"ketQua":null,"thongBao":null,
              "oSai":[]},
             %s]""".formatted(xetDau.formatted("\"ketQua\":null,\"thongBao\":null,\"oSai\":[]")), false));
    }

    @Test
    void baiNgoaiLopChuaPhatHanhHayBiRutThi404NhuKhongCoBai() {
        for (String ma : List.of(maChua, maKhac, maRut, "KHONG-CO-BAI")) {
            for (Phan p : List.of(get("/api/hs/bai/" + ma, an), nopBuoc(ma, dong("B.DH.TXD", "D = R", null)), nopBai(ma))) {
                assertThat(p.status()).as(ma).isEqualTo(404);
                assertThat(p.json().path("detail").asString()).as(ma).isEqualTo(KHONG_CO_BAI);
            }
        }
        assertThat(mayCham.yeuCau).as("không gọi máy chấm").isEmpty();
        assertThat(jdbc.sql("select count(*) from submissions where student_id = ?").params(an).query(Integer.class).single()).isZero();
    }

    @Test
    void giaoVienThi403() {
        UUID gv = nguoiMoi(Role.TEACHER);
        DuLieuPractice.ghiDanh(jdbc, lop, gv, "TEACHER");
        assertThat(get("/api/hs/bai", gv).status()).isEqualTo(403);
        assertThat(get("/api/hs/bai/" + ma1, gv).status()).isEqualTo(403);
        assertThat(nopBuoc(ma1, dong("B.DH.TXD", "D = R", null), gv).status()).isEqualTo(403);
        assertThat(nopBai(ma1, gv).status()).isEqualTo(403);
        assertThat(get("/api/hs/bai", an).status()).as("cùng đường, học sinh thì vào được").isEqualTo(200);
    }

    @Test
    void idTrongThanBiBoQuaChiDungNguoiTrongToken() {
        Phan p = nopBuoc(ma1, """
            {"maBuoc":"B.DH.TXD","dong":[{"dong":0,"latex":"D = R"}],"hocSinhId":"%s","lopId":"%s","baiLamId":"%s"}"""
            .formatted(binh, lopKhac, UUID.randomUUID()));
        assertThat(p.status()).isEqualTo(200);
        assertThat(jdbc.sql("select student_id || ' ' || class_id from submissions where problem_id = ?").params(p1).query(String.class)
            .list()).containsExactly(an + " " + lop);
    }

    @Test
    void thanSaiThi400KhongGhiGi() {
        Map<String, String> thanSai = new LinkedHashMap<>();
        thanSai.put("{}", "Bài làm gửi lên thiếu hay sai trường: maBuoc.");
        thanSai.put("{\"maBuoc\":\"B.DH.TXD\",\"dong\":[{\"dong\":0}]}", "Bài làm gửi lên thiếu hay sai trường: dong[0].latex.");
        thanSai.put("{\"maBuoc\":\"B.DH.TXD\",\"dong\":[{\"dong\":-1,\"latex\":\"x\"}]}", "Bài làm gửi lên thiếu hay sai trường: dong[0].dong.");
        thanSai.put("{\"maBuoc\":", "Không đọc được bài làm gửi lên.");
        thanSai.put("{\"maBuoc\":\"B.DH.KHAC\",\"dong\":[{\"dong\":0,\"latex\":\"x\"}]}", "Bước không thuộc khung của bài: B.DH.KHAC");
        thanSai.put("{\"maBuoc\":\"B.DH.TXD\"}", "Bước B.DH.TXD không có dòng hay bảng nào");
        thanSai.put("{\"maBuoc\":\"B.DH.XETDAU\",\"bang\":[]}", "Bước B.DH.XETDAU không có dòng hay bảng nào");
        String dongTxd = "\"maBuoc\":\"B.DH.TXD\",\"dong\":[{\"dong\":0,\"latex\":\"D = R\"}]";
        thanSai.put("{" + dongTxd + ",\"suKien\":[{\"maBuoc\":\"B.XX\",\"giaTriMoi\":\"1\",\"luc\":\"2026-10-06T00:00:00Z\"}]}",
            "Sự kiện nhập của bước không thuộc khung của bài: B.XX");
        thanSai.put("{" + dongTxd + ",\"suKien\":[{\"maBuoc\":\"B.DH.XETDAU\",\"hang\":\"X\",\"k\":40000,\"giaTriMoi\":\"1\",\"luc\":\"2026-10-06T00:00:00Z\"}]}",
            "Bài làm gửi lên thiếu hay sai trường: suKien[0].k.");
        thanSai.forEach((than, chiTiet) -> {
            Phan p = nopBuoc(ma1, than);
            assertThat(p.status()).as(than).isEqualTo(400);
            assertThat(p.json().path("detail").asString()).as(than).isEqualTo(chiTiet);
        });
        assertThat(mayCham.yeuCau).isEmpty();
        assertThat(jdbc.sql("select count(*) from submissions where student_id = ?").params(an).query(Integer.class).single()).isZero();
    }

    @Test
    void nopKhiChuaLamDuBuocThi409NoiEmPhaiLamGi() {
        nopBuoc(ma1, dong("B.DH.TXD", "D = R", null));
        Phan p = nopBai(ma1);
        assertThat(p.status()).isEqualTo(409);
        assertThat(p.json().path("detail").asString()).isEqualTo("Em làm đủ các bước, tới bước kết luận, rồi hãy nộp bài.");
        assertThat(p.json().path("lyDo").asString()).isEqualTo("CHUA_LAM_DU_BUOC");
        assertThat(jdbc.sql("select status from submissions where student_id = ?").params(an).query(String.class).list())
            .containsExactly("DANG_LAM");
    }

    @Test
    void khongPhanHoiNaoMangLoiGiaiTruNopBaiKhiLopMoCo() {
        List<Phan> daThay = new ArrayList<>();
        daThay.add(get("/api/hs/bai", an));
        daThay.add(get("/api/hs/bai/" + ma1, an));
        daThay.add(nopBuoc(ma1, dong("B.DH.TXD", "D = R", null)));
        daThay.add(nopBuoc(ma1, dong("B.DH.DAOHAM", "y' = SAI_O_DAY", null)));
        daThay.add(nopBuoc(ma1, dong("B.DH.DAOHAM", "y' = 3x^2 - 6x", null)));
        daThay.addAll(lamTiepToiKetLuan(ma1));
        daThay.add(get("/api/hs/bai/" + ma1, an));
        Phan nop = nopBai(ma1);
        daThay.add(nop);
        assertJson(nop, NOP_DAT_LEN_THONG_HIEU);
        daThay.add(get("/api/hs/bai/" + ma1, an));
        daThay.add(get("/api/hs/bai", an));
        assertThat(daThay).allSatisfy(p -> assertThat(p.status()).isEqualTo(200));
        assertThat(daThay).allSatisfy(p -> assertThat(p.than()).doesNotContain(BI_MAT));

        // Lớp bật «mở lời giải sau khi nộp»: chỉ phản hồi nộp bài mang lời giải; đề và bài làm vẫn không.
        jdbc.sql("insert into class_settings (class_id, reveal_solution_after_submit, updated_at) values (?, true, now())").params(lop).update();
        assertThat(nopBai(ma1).json().path("loiGiai").asString()).as("lời giải viết như v0: có đáp án cuối thì dùng đáp án cuối")
            .isEqualTo("BIMAT_DAP_AN");
        Phan chiTiet = get("/api/hs/bai/" + ma1, an);
        assertThat(chiTiet.json().path("coTheMoLoiGiai").asBoolean()).isTrue();
        assertThat(chiTiet.than()).doesNotContain(BI_MAT);
        assertThat(get("/api/hs/bai", an).than()).doesNotContain(BI_MAT);
    }

    private String chiTiet1(String trangThai, String cacBuocDaLam, boolean coTheMoLoiGiai) {
        return """
            {"maBai":"%s","de":{"text":"%s","latex":"y = x^3 - 3x^2 + 2"},"kyNang":"T12.DH.02","tenKyNang":"Tính đạo hàm",
             "muc4":"THONG_HIEU","dangTraLoi":"TU_LUAN_5_BUOC","buocBatDau":null,"khaiBaoKetLuan":["dong_bien","nghich_bien"],
             "cacBuoc":%s,"baiLam":{"trangThai":"%s","cacBuoc":%s},"coTheMoLoiGiai":%s}"""
            .formatted(ma1, DE, KHUNG, trangThai, cacBuocDaLam, coTheMoLoiGiai);
    }

    private static String dongDanhSach(String ma, @Nullable String han, String trangThai, int soBuocDat, @Nullable String ketQua) {
        return """
            {"maBai":"%s","deBai":"%s","deBaiLatex":"y = x^3 - 3x^2 + 2","kyNang":"T12.DH.02","tenKyNang":"Tính đạo hàm",
             "muc4":"THONG_HIEU","han":%s,"trangThai":"%s","soBuocDat":%d,"soBuoc":5,"ketQua":%s}"""
            .formatted(ma, DE, han, trangThai, soBuocDat, ketQua);
    }

    /** Nộp bước nghiệm, xét dấu, kết luận đúng (bước trước đã đạt). */
    private List<Phan> lamTiepToiKetLuan(String ma) {
        return List.of(
            nopBuoc(ma, dong("B.DH.NGHIEM", "x = 0, x = 2", "NGHIEM")),
            nopBuoc(ma, """
                {"maBuoc":"B.DH.XETDAU","bang":[{"hang":"X","k":0,"giaTri":"0"},{"hang":"DAU_YPHAY","k":2,"giaTri":"+"}]}"""),
            nopBuoc(ma, """
                {"maBuoc":"B.DH.KETLUAN","dong":[{"dong":0,"latex":"(-\\\\infty; 0), (2; +\\\\infty)","loai":"DONG_BIEN"},
                 {"dong":1,"latex":"(0; 2)","loai":"NGHICH_BIEN"}]}"""));
    }

    private int soBuocDat(String ma) {
        for (JsonNode d : get("/api/hs/bai", an).json()) {
            if (d.path("maBai").asString().equals(ma)) {
                return d.path("soBuocDat").asInt();
            }
        }
        throw new AssertionError("Không có bài " + ma);
    }

    private static String dong(String maBuoc, String latex, @Nullable String loai) {
        Map<String, Object> d = new LinkedHashMap<>();
        d.put("dong", 0);
        d.put("latex", latex);
        if (loai != null) {
            d.put("loai", loai);
        }
        return JSON.writeValueAsString(Map.of("maBuoc", maBuoc, "dong", List.of(d)));
    }

    private Phan get(String uri, UUID nguoiGoi) {
        return phan(mvc.get().uri(uri).header("Authorization", bearer(nguoiGoi)).exchange());
    }

    private Phan nopBuoc(String ma, String than) {
        return nopBuoc(ma, than, an);
    }

    private Phan nopBuoc(String ma, String than, UUID nguoiGoi) {
        return phan(mvc.post().uri("/api/hs/bai/" + ma + "/buoc").header("Authorization", bearer(nguoiGoi))
            .contentType(MediaType.APPLICATION_JSON).content(than).exchange());
    }

    private Phan nopBai(String ma) {
        return nopBai(ma, an);
    }

    private Phan nopBai(String ma, UUID nguoiGoi) {
        return phan(mvc.post().uri("/api/hs/bai/" + ma + "/nop").header("Authorization", bearer(nguoiGoi)).exchange());
    }

    private static Phan phan(MvcTestResult r) {
        return new Phan(r.getResponse().getStatus(), new String(r.getResponse().getContentAsByteArray(), StandardCharsets.UTF_8));
    }

    private static void assertJson(Phan p, String kyVong) {
        assertThat(p.status()).as(p.than()).isEqualTo(200);
        assertThat(p.json()).isEqualTo(JSON.readTree(kyVong));
    }

    private String bearer(UUID id) {
        Instant now = Instant.now();
        User u = new User(new UserId(id), new Email(id + "@demo.local"), "x", "Người thử", vaiTro.get(id), true, true, now, now);
        return "Bearer " + tokens.issue(u, now).value();
    }

    private UUID nguoiMoi(Role role) {
        UUID id = DuLieuPractice.nguoi(jdbc, role.name());
        nguoi.add(id);
        vaiTro.put(id, role);
        return id;
    }

    private UUID baiMoi(String ma) {
        UUID id = DuLieuPractice.bai(jdbc, ma);
        bai.add(id);
        return id;
    }

    private String ma(UUID id) {
        return jdbc.sql("select code from problems where id = ?").params(id).query(String.class).single();
    }

    private void giao(UUID l, UUID baiId, UUID hs, String luc, @Nullable String han) {
        jdbc.sql("""
                insert into assignments (id, class_id, problem_id, student_id, status, set_name, due_at, assigned_at)
                values (?, ?, ?, ?, 'DA_GIAO', 'Đơn điệu và cực trị', cast(? as timestamptz), cast(? as timestamptz))""")
            .params(UUID.randomUUID(), l, baiId, hs, han, luc).update();
    }

    /** Một phản hồi: mã trạng thái và thân nguyên văn. */
    record Phan(int status, String than) {

        JsonNode json() {
            return JSON.readTree(than);
        }
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class CauHinhMayCham {

        @Bean
        @Primary
        MayChamThu mayChamThu() {
            return new MayChamThu();
        }
    }

    /**
     * Máy chấm thử thay {@code /v1/grade}: phong bì đủ 12 khóa như {@code _pack} của {@code grader.py}. Bước nộp có
     * {@code SAI_O_DAY} thì sai ở dòng 0, có {@code LOI_MAY} thì không trả lời ({@code KHONG_CHAM_DUOC}), còn lại đạt.
     */
    static class MayChamThu implements MayCham {

        private static final List<String> KHUNG = List.of("B.DH.TXD", "B.DH.DAOHAM", "B.DH.NGHIEM", "B.DH.XETDAU", "B.DH.KETLUAN");

        final List<Map<String, ?>> yeuCau = new CopyOnWriteArrayList<>();

        @Override
        public Optional<Map<String, @Nullable Object>> cham(Map<String, ?> y) {
            yeuCau.add(y);
            String nopToi = (String) y.get("nop_toi");
            String buoc = ((List<?>) y.get("cac_buoc")).stream()
                .filter(b -> b instanceof Map<?, ?> m && nopToi.equals(m.get("ma_buoc"))).map(String::valueOf).findFirst().orElse("");
            if (buoc.contains("LOI_MAY")) {
                return Optional.empty();
            }
            boolean dat = !buoc.contains("SAI_O_DAY");
            Map<String, @Nullable Object> p = new LinkedHashMap<>();
            p.put("ket_qua", dat ? "DAT" : "SAI");
            p.put("loai_ket_qua", dat ? "DAT" : "SAI_TOAN");
            p.put("buoc_sai", dat ? null : Map.of("ma_buoc", nopToi, "dong", 0));
            p.put("ma_loi", dat ? null : "ERR.DH.01");
            p.put("do_tin_cay", dat ? null : 0.9);
            Map<String, String> per = new LinkedHashMap<>();
            if (dat) {
                KHUNG.subList(0, KHUNG.indexOf(nopToi) + 1).forEach(b -> per.put(b, "DAT"));
            } else {
                per.put(nopToi, "SAI");
            }
            p.put("per_buoc", per);
            p.put("thong_bao", dat ? "Đúng rồi." : "Chưa đúng ở dòng này.");
            p.put("cac_van_de", List.of());
            p.put("chua_xong", !dat || !nopToi.equals(KHUNG.getLast()));
            p.put("chuan_hoa", List.of());
            p.put("phien_ban_chuan_hoa", "norm-0.2");
            p.put("nop_toi", nopToi);
            return Optional.of(p);
        }
    }
}
