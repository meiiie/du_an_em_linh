package vn.hoctapcanman.core.content.infrastructure.nhap;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Testcontainers;
import vn.hoctapcanman.core.TestcontainersConfiguration;
import vn.hoctapcanman.core.content.domain.model.BloomLevel;
import vn.hoctapcanman.core.content.domain.model.HintLevel;
import vn.hoctapcanman.core.content.domain.model.Level3;
import vn.hoctapcanman.core.content.domain.model.Level4;
import vn.hoctapcanman.core.content.domain.model.Problem;
import vn.hoctapcanman.core.content.infrastructure.persistence.HintLevelRepositoryAdapter;
import vn.hoctapcanman.core.content.infrastructure.persistence.ProblemRepositoryAdapter;
import vn.hoctapcanman.core.content.infrastructure.persistence.SolutionRepositoryAdapter;
import vn.hoctapcanman.core.content.infrastructure.persistence.TopicCatalogRepositoryAdapter;

/**
 * Nhập nội dung chung (T012a) từ {@code data/} thật của repo trên PostgreSQL 18, với dịch vụ toán giả trả lời cố định:
 * đủ bài, quy đổi mức và Bloom như v0, bài khung ngắn, ví dụ cổng chặn, và nhập lại không nhân bản.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({
    TestcontainersConfiguration.class,
    TopicCatalogRepositoryAdapter.class,
    ProblemRepositoryAdapter.class,
    SolutionRepositoryAdapter.class,
    HintLevelRepositoryAdapter.class,
    NguonNoiDung.class,
    NhapNoiDungChung.class,
    NhapNoiDungChungTest.ToanGia.class
})
@Testcontainers(disabledWithoutDocker = true)
class NhapNoiDungChungTest {

    @DynamicPropertySource
    static void nguon(DynamicPropertyRegistry r) {
        r.add("app.content.source", () -> thuMucData().toString());
    }

    /** {@code data/} của repo, tìm ngược từ thư mục chạy (Maven chạy ở {@code services/core}). */
    static Path thuMucData() {
        for (Path p = Path.of("").toAbsolutePath(); p != null; p = p.getParent()) {
            if (Files.exists(p.resolve("data/supham/danh-muc-ky-nang-DH.json"))) {
                return p.resolve("data");
            }
        }
        String daDat = System.getProperty("app.content.source");
        if (daDat != null) {
            return Path.of(daDat);
        }
        throw new IllegalStateException("Không tìm thấy data/ của repo");
    }

    @Autowired
    private NhapNoiDungChung nhap;

    @Autowired
    private ProblemRepositoryAdapter problems;

    @Autowired
    private SolutionRepositoryAdapter solutions;

    @Autowired
    private HintLevelRepositoryAdapter hints;

    @Autowired
    private TopicCatalogRepositoryAdapter catalog;

    @Autowired
    private JdbcClient jdbc;

    @Test
    void nhapDuNoiDungChungNhuV0VaNhapLaiKhongNhanBan() {
        NhapNoiDungChung.KetQua lan1 = nhap.nhap();
        assertThat(lan1.kyNang()).isEqualTo(11);
        assertThat(lan1.maLoi()).isEqualTo(31);
        assertThat(lan1.buoc()).isEqualTo(5);
        // 3 bài ví dụ + 2 bài máy giải + 2 biến thể (huu_ti lỗi ở dịch vụ toán giả nên bỏ như v0) + 8 khung ngắn + 1 ví dụ chặn.
        assertThat(lan1.bai()).hasSize(16).contains("DH12-03-VD-01", "DH12-NB-01", "GEN-bac_ba-11", "DH12-05-TH-02", "DH12-DEMO-CHAN-01")
            .doesNotContain("GEN-huu_ti-5");

        assertThat(catalog.findTopic(NhapNoiDungChung.CHU_DE).orElseThrow().name()).startsWith("Ứng dụng đạo hàm");
        assertThat(catalog.findPrerequisites("T12.DH.03")).hasSize(3);
        assertThat(catalog.findStepTemplates(NhapNoiDungChung.CHU_DE)).hasSize(5);

        Problem tuLuan = problems.findByCode("DH12-03-VD-01").orElseThrow();
        assertThat(tuLuan.answerForm()).isEqualTo(Problem.TU_LUAN_5_BUOC);
        assertThat(tuLuan.functionSympy()).isEqualTo("x**3 - 6*x**2 + 9*x + 2");
        assertThat(tuLuan.level4()).isEqualTo(Level4.VAN_DUNG);
        assertThat(solutions.findByProblemId(tuLuan.id()).orElseThrow().workedSolutionJson()).contains("d(x**3 - 6*x**2 + 9*x + 2)");

        Problem thamSo = problems.findByCode("DH12-06-VDC-01").orElseThrow();
        assertThat(thamSo.functionSympy()).isNull();
        assertThat(thamSo.answerForm()).isEqualTo("TRA_LOI_NGAN");
        assertThat(thamSo.level3()).isEqualTo(Level3.VAN_DUNG);
        assertThat(thamSo.bloomLevel()).isEqualTo(BloomLevel.PHAN_TICH);
        assertThat(solutions.findByProblemId(thamSo.id()).orElseThrow().workedSolutionJson()).isNull();

        Problem sinh = problems.findByCode("GEN-bac_ba-11").orElseThrow();
        assertThat(sinh.bloomLevel()).isEqualTo(BloomLevel.VAN_DUNG);
        assertThat(sinh.statementText()).doesNotContain("$");

        Problem khungNgan = problems.findByCode("DH12-03-NB-02").orElseThrow();
        assertThat(khungNgan.startStep()).isEqualTo("B.DH.XETDAU");
        assertThat(khungNgan.answerForm()).isEqualTo("TN_NHIEU_LUA_CHON");
        assertThat(hints.findByProblemId(khungNgan.id())).isNotEmpty();

        Problem chan = problems.findByCode("DH12-DEMO-CHAN-01").orElseThrow();
        assertThat(chan.bloomLevel()).isEqualTo(BloomLevel.NHO);
        assertThat(solutions.findByProblemId(chan.id()).orElseThrow().workedSolutionJson()).contains("\"dao_ham\": \"3*x\"");
        // SP-08: cấp gợi ý rỗng (chỉ ly_do_trong) không thành dòng gợi ý.
        assertThat(hints.findByProblemId(chan.id())).extracting(HintLevel::level).containsExactly(1);

        Map<String, String> bamLan1 = bamTheoMa(lan1.bai());
        long dongGoiY = jdbc.sql("select count(*) from hint_levels").query(Long.class).single();
        int phienBan = phienBan(tuLuan);
        NhapNoiDungChung.KetQua lan2 = nhap.nhap();
        assertThat(lan2.bai()).isEqualTo(lan1.bai());
        assertThat(bamTheoMa(lan2.bai())).isEqualTo(bamLan1);
        assertThat(jdbc.sql("select count(*) from problems").query(Long.class).single()).isEqualTo(16);
        assertThat(jdbc.sql("select count(*) from hint_levels").query(Long.class).single()).isEqualTo(dongGoiY);
        assertThat(problems.findByCode("DH12-03-VD-01").orElseThrow().id()).isEqualTo(tuLuan.id());
        // Nhập lại y như cũ không đổi gì của nội dung bài, nên phiên bản nội dung (V5) giữ nguyên, phát hành không bị rút.
        assertThat(phienBan(tuLuan)).isEqualTo(phienBan);
    }

    private int phienBan(Problem p) {
        return jdbc.sql("select content_version from problems where id = ?").params(p.id()).query(Integer.class).single();
    }

    private Map<String, String> bamTheoMa(List<String> ma) {
        Map<String, String> bam = new HashMap<>();
        for (String m : ma) {
            Problem p = problems.findByCode(m).orElseThrow();
            bam.put(m, p.id() + "/" + p.contentHash());
        }
        return bam;
    }

    /** Dịch vụ toán giả: giải mọi hàm bằng lời giải cố định; {@code huu_ti} lỗi như khi máy không dùng được. */
    @TestConfiguration
    static class ToanGia {

        @Bean
        GiaiToan giaiToan() {
            return new GiaiToan() {
                @Override
                public Optional<Map<String, @Nullable Object>> giai(Map<String, ?> yeuCau) {
                    String ham = (String) yeuCau.get("ham");
                    Map<String, @Nullable Object> kq = new LinkedHashMap<>();
                    kq.put("dat", true);
                    kq.put("latex", "L(" + ham + ")");
                    kq.put("bai_lam", new LinkedHashMap<>(Map.of("ham", ham, "dao_ham", "d(" + ham + ")")));
                    kq.put("su_kien", List.of(Map.of("loai", "DAO_HAM", "gia_tri", "d(" + ham + ")")));
                    Map<String, @Nullable Object> capRong = new LinkedHashMap<>();
                    capRong.put("cap", 2);
                    capRong.put("noi_dung", null);
                    capRong.put("ly_do_trong", "Bộ lọc chặn vì có thể lộ kết quả.");
                    kq.put("thang_goi_y", List.of(Map.of("ma_buoc", "B.DH.DAOHAM",
                        "cac_cap", List.of(Map.of("cap", 1, "noi_dung", "Đạo hàm từng hạng tử."), capRong))));
                    return Optional.of(kq);
                }

                @Override
                public Optional<Map<String, @Nullable Object>> sinh(Map<String, ?> yeuCau) {
                    if ("huu_ti".equals(yeuCau.get("dang"))) {
                        return Optional.of(Map.of("loi", "khong dung duoc loi giai may"));
                    }
                    Map<String, @Nullable Object> kq = new LinkedHashMap<>();
                    kq.put("ham", "x**3 - 3*x");
                    kq.put("latex", "x^{3} - 3 x");
                    kq.put("de_bai", "Tìm các khoảng đồng biến, nghịch biến và cực trị của hàm số $y = x^{3} - 3 x$.");
                    kq.put("muc_do_4", "VAN_DUNG");
                    kq.put("muc_do_bo_3", "VAN_DUNG");
                    kq.put("muc_bloom", "APPLY");
                    kq.put("bai_lam", Map.of("ham", "x**3 - 3*x"));
                    kq.put("su_kien", List.of());
                    kq.put("thang_goi_y", List.of());
                    kq.put("ky_nang_chinh", "T12.DH.03");
                    return Optional.of(kq);
                }
            };
        }

        @Bean
        Clock clock() {
            return Clock.fixed(Instant.parse("2026-10-04T08:00:00Z"), ZoneOffset.UTC);
        }
    }
}
