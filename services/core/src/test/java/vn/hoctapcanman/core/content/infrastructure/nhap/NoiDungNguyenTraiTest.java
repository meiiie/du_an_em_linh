package vn.hoctapcanman.core.content.infrastructure.nhap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.nio.file.Path;
import java.time.Clock;
import java.util.HashMap;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;
import vn.hoctapcanman.core.content.domain.model.Problem;
import vn.hoctapcanman.core.content.domain.model.Solution;
import vn.hoctapcanman.core.content.domain.repository.*;

class NoiDungNguyenTraiTest {
    @org.junit.jupiter.api.io.TempDir Path temp;

    @Test void thieuDataDevThiCacRunnerBoQuaKhongLamSapCore() {
        var jdbc = mock(org.springframework.jdbc.core.simple.JdbcClient.class);
        var chung = mock(NhapNoiDungChung.class);
        var theoLop = mock(NhapTheoLop.class);
        var math = mock(vn.hoctapcanman.core.shared.infrastructure.math.MathServiceClient.class);
        var manager = mock(PlatformTransactionManager.class);
        when(chung.nhapTruong()).thenThrow(new java.io.UncheckedIOException(
            new java.nio.file.NoSuchFileException("dev/nguyen-trai/bai-tap.json")));
        try (var guard = mockStatic(vn.hoctapcanman.core.shared.infrastructure.seed.LopSeedAnToan.class)) {
            guard.when(() -> vn.hoctapcanman.core.shared.infrastructure.seed.LopSeedAnToan.kiem(any(), anyString()))
                .thenReturn(true);
            new NoiDungNguyenTraiSeeder(chung, theoLop, jdbc).run(new org.springframework.boot.DefaultApplicationArguments());
            new vn.hoctapcanman.core.practice.infrastructure.seed.GiaoBaiNguyenTraiSeeder(jdbc, temp.toString())
                .run(new org.springframework.boot.DefaultApplicationArguments());
            new vn.hoctapcanman.core.practice.infrastructure.seed.LichSuNguyenTraiSeeder(
                jdbc, math, manager, Clock.systemUTC(), temp.toString())
                .run(new org.springframework.boot.DefaultApplicationArguments());
        }
        verifyNoInteractions(jdbc, theoLop, math, manager);
    }

    @Test void migrationVaImporterDungCungLoiGiaiDayDu() throws java.io.IOException {
        var json = tools.jackson.databind.json.JsonMapper.builder().build();
        try (var source = getClass().getResourceAsStream("/seed-nguyen-trai/loi-giai-day-du.json");
             var migration = getClass().getResourceAsStream("/db/migration/V21__loi_giai_day_du_seed.sql")) {
            var expected = json.readTree(java.util.Objects.requireNonNull(source));
            String sql = new String(java.util.Objects.requireNonNull(migration).readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            var matcher = java.util.regex.Pattern.compile("\\('([^']+)', \\$worked\\$(.*?)\\$worked\\$::jsonb\\)", java.util.regex.Pattern.DOTALL).matcher(sql);
            int count = 0;
            while (matcher.find()) {
                assertThat(json.readTree(matcher.group(2))).isEqualTo(expected.path(matcher.group(1)));
                count++;
            }
            assertThat(count).isEqualTo(74);
            assertThat(sql).contains("p.origin='GIAO_VIEN_NGUYEN_TRAI'", "IS DISTINCT FROM");
            assertThat(sql).doesNotContain("DISABLE TRIGGER", "UPDATE submissions", "UPDATE problem_releases");
        }
    }

    @Test void nguonTruongKhongDungMaySinhVaGhiLoiGiaiDayDuMotLan() {
        var problems=mock(ProblemRepository.class);var solutions=mock(SolutionRepository.class);
        var catalog=mock(TopicCatalogRepository.class);var hints=mock(HintLevelRepository.class);
        var math=mock(GiaiToan.class);var manager=mock(PlatformTransactionManager.class);
        var saved=new HashMap<String,Problem>();
        when(manager.getTransaction(any())).thenAnswer(i -> new SimpleTransactionStatus());
        when(problems.findByCode(any())).thenAnswer(i -> Optional.ofNullable(saved.get(i.getArgument(0))));
        doAnswer(i -> {Problem p=i.getArgument(0);saved.put(p.code(),p);return null;}).when(problems).save(any());
        when(problems.findContentVersion(any())).thenReturn(Optional.of(1));
        var frames = mock(vn.hoctapcanman.core.content.application.port.KhungChamTheoBai.class);
        var source=new NguonNoiDung(new NoiDungProperties(Path.of("../../data").toString(),true));
        var importer=new NhapNoiDungChung(source,math,catalog,problems,solutions,hints,manager,mock(ApplicationEventPublisher.class),Clock.systemUTC(), frames);
        var first=importer.nhapTruong();
        assertThat(first.bai()).hasSize(74);
        var khung = ArgumentCaptor.forClass(vn.hoctapcanman.core.content.application.dto.KhungCham.class);
        verify(frames, times(43)).ghi(any(), eq(1), khung.capture());
        assertThat(khung.getAllValues()).filteredOn(f -> f.cacBuoc().getFirst().maBuoc().startsWith("B.GT.")).hasSize(17);
        assertThat(khung.getAllValues()).filteredOn(f -> f.cacBuoc().getFirst().maBuoc().startsWith("B.XS.")).hasSize(16);
        assertThat(saved.values()).filteredOn(Problem::isFiveStep).hasSize(10);
        assertThat(saved.values()).filteredOn(p -> !p.isFiveStep()).hasSize(64);
        var skills=ArgumentCaptor.forClass(vn.hoctapcanman.core.content.domain.model.Skill.class);
        verify(catalog,atLeastOnce()).saveSkill(skills.capture());
        assertThat(skills.getAllValues()).extracting(vn.hoctapcanman.core.content.domain.model.Skill::name)
            .contains("Cực trị hàm bậc ba", "Thể tích khối chóp", "Cấp số cộng");
        var records=ArgumentCaptor.forClass(Solution.class);
        verify(solutions,times(74)).save(records.capture());
        assertThat(records.getAllValues()).allSatisfy(s -> {
            assertThat(s.finalAnswer()).isNotBlank();assertThat(s.workedSolutionJson()).isNotBlank();
            String full = vn.hoctapcanman.core.content.application.service.VietLoiGiai.toanBo(s).orElseThrow();
            assertThat(full.lines().count()).as("Lời giải nhiều bước cho %s", s.problemId()).isGreaterThanOrEqualTo(2);
            assertThat(full).contains("**Bước 1.**", "**Bước 2.**").doesNotContain("**2", "**3", "**4", "sqrt(", "Derivative(", "Integral(", "binomial(", "oo");
        });
        var integral = records.getAllValues().stream().filter(s -> s.problemId().equals(saved.get("NT-TP12-TH-02").id())).findFirst().orElseThrow();
        assertThat(vn.hoctapcanman.core.content.application.service.VietLoiGiai.toanBo(integral).orElseThrow())
            .contains("F(x)=x^{2} + 5 x", "36-(6)=30", "Newton–Leibniz");
        importer.nhapTruong();
        verify(problems,times(74)).save(any()); // Lần đọc thứ hai không sửa nội dung.
        verifyNoInteractions(math);
        assertThat(first.ketQua().bai()).doesNotHaveDuplicates();
    }
}
