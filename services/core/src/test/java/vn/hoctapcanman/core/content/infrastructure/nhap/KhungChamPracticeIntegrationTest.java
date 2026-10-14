package vn.hoctapcanman.core.content.infrastructure.nhap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.time.Clock;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Testcontainers;
import tools.jackson.databind.json.JsonMapper;
import vn.hoctapcanman.core.TestcontainersConfiguration;
import vn.hoctapcanman.core.classroom.application.service.ClassMembershipService;
import vn.hoctapcanman.core.classroom.infrastructure.persistence.ClassSettingsRepositoryAdapter;
import vn.hoctapcanman.core.classroom.infrastructure.persistence.EnrollmentRepositoryAdapter;
import vn.hoctapcanman.core.content.application.service.BaiDeLamService;
import vn.hoctapcanman.core.content.infrastructure.persistence.*;
import vn.hoctapcanman.core.practice.application.dto.DongNop;
import vn.hoctapcanman.core.practice.application.dto.NopBuocRequest;
import vn.hoctapcanman.core.practice.application.port.MayCham;
import vn.hoctapcanman.core.practice.application.service.BaiLamHienTai;
import vn.hoctapcanman.core.practice.application.service.MoLoiGiai;
import vn.hoctapcanman.core.practice.application.usecase.NopBaiUseCase;
import vn.hoctapcanman.core.practice.application.usecase.NopBuocUseCase;
import vn.hoctapcanman.core.practice.infrastructure.persistence.*;

/** Flyway + importer + content port + use cases + persistence thật; chỉ máy CAS ở biên là giả. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({TestcontainersConfiguration.class, ProblemRepositoryAdapter.class, TopicCatalogRepositoryAdapter.class,
    SolutionRepositoryAdapter.class, HintLevelRepositoryAdapter.class, KhungChamRepositoryAdapter.class,
    BaiDeLamService.class, EnrollmentRepositoryAdapter.class, ClassSettingsRepositoryAdapter.class,
    ClassMembershipService.class, SubmissionRepositoryAdapter.class, GradingResultRepositoryAdapter.class})
@Testcontainers(disabledWithoutDocker = true)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class KhungChamPracticeIntegrationTest {
    @Autowired JdbcClient jdbc;
    @Autowired PlatformTransactionManager manager;
    @Autowired ApplicationEventPublisher events;
    @Autowired ProblemRepositoryAdapter problems;
    @Autowired TopicCatalogRepositoryAdapter catalog;
    @Autowired SolutionRepositoryAdapter solutions;
    @Autowired HintLevelRepositoryAdapter hints;
    @Autowired KhungChamRepositoryAdapter frames;
    @Autowired BaiDeLamService content;
    @Autowired ClassMembershipService membership;
    @Autowired SubmissionRepositoryAdapter submissions;
    @Autowired GradingResultRepositoryAdapter grades;
    private UUID lop, student;
    private final List<Map<String, ?>> requests = new ArrayList<>();
    private final NguonNoiDung source = new NguonNoiDung(new NoiDungProperties("../../data", false));
    private final JsonMapper json = JsonMapper.builder().build();

    @BeforeEach void seed() {
        new NhapNoiDungChung(source, mock(GiaiToan.class), catalog, problems, solutions, hints, manager, events,
            Clock.systemUTC(), frames).nhapTruong();
        lop = DuLieuPractice.lop(jdbc);
        student = DuLieuPractice.nguoi(jdbc, "STUDENT");
        DuLieuPractice.ghiDanh(jdbc, lop, student, "STUDENT");
    }

    @AfterEach void cleanup() {
        jdbc.sql("DELETE FROM classes WHERE id=?").params(lop).update();
        jdbc.sql("DELETE FROM users WHERE id=?").params(student).update();
        jdbc.sql("DELETE FROM problems WHERE origin='GIAO_VIEN_NGUYEN_TRAI'").update();
    }

    @ParameterizedTest @ValueSource(strings = {"NT-TP12-NB-01", "NT-XS12-NB-01"})
    void namBuocQuaUseCaseDuKienDungBaiPhienBanKhongLoDapAn(String code) {
        var problem = problems.findByCode(code).orElseThrow();
        // Cổng phát hành của fixture dùng BAM; không sửa nội dung đã nhập, nên dùng hash thật để tạo lượt kiểm.
        UUID run = UUID.randomUUID();
        int version = problems.findContentVersion(problem.id()).orElseThrow();
        jdbc.sql("""
            INSERT INTO verification_runs(id,class_id,subject_kind,subject_id,content_hash,overall_status,publish_status,content_version,created_at)
            VALUES (?,?,'PROBLEM',?,?,'DAT','DA_PHAT_HANH',?,clock_timestamp())
            """).params(run, lop, problem.id(), problem.contentHash(), version).update();
        jdbc.sql("INSERT INTO problem_releases(class_id,problem_id,status,run_id,updated_at) VALUES (?,?,'DA_PHAT_HANH',?,now())")
            .params(lop, problem.id(), run).update();
        var bai = content.bai(lop, code).orElseThrow();
        var frame = DocKhungCham.doc(source).get(code);
        assertThat(bai.chamTungBuoc()).isTrue();
        assertThat(bai.moTaBuoc()).isEqualTo(frame.cacBuoc());
        assertThat(frames.doc(problem.id(), version + 1)).isEmpty();
        String publicJson = json.writeValueAsString(bai);
        assertThat(publicJson).doesNotContain("dap_an", "bai_toan", "du_kien", "baiToan");
        MayCham engine = payload -> {
            requests.add(payload);
            assertThat(payload.get("bai_toan")).isEqualTo(frame.baiToan());
            assertThat(payload).doesNotContainKey("ham");
            assertThat(json.writeValueAsString(payload)).doesNotContain("dap_an", "payload_dung");
            return Optional.of(response(payload, "DAT"));
        };
        var tx = new TransactionTemplate(manager);
        var nop = new NopBuocUseCase(membership, content, submissions, grades, engine, tx, Clock.systemUTC(), 4);
        for (var step : bai.moTaBuoc()) {
            // Nội dung giả từ học sinh: test chỉ xác nhận nối payload, không tự phán đúng toán.
            var result = nop.execute(student, lop, code, new NopBuocRequest(step.maBuoc(), List.of(new DongNop(0, "1", null)), null, null));
            assertThat(result.ketQua()).isEqualTo("DAT");
        }
        assertThat(requests).hasSize(5);
        assertThat((List<?>) requests.getLast().get("cac_buoc")).hasSize(5);
        var history = new BaiLamHienTai(submissions, grades, content);
        assertThat(history.cua(student, lop, bai).soBuocDat()).isEqualTo(5);
        nop.execute(student, lop, code, new NopBuocRequest(bai.cacBuoc().getLast(), List.of(new DongNop(0, "1", null)), null, null));
        assertThat(requests).hasSize(5); // Gửi lại cùng nội dung dùng băm và kết quả đã lưu.
        var submit = new NopBaiUseCase(membership, content, submissions, grades, List.of(), mock(MoLoiGiai.class), tx, Clock.systemUTC());
        assertThat(submit.execute(student, lop, code).ketQua().name()).isEqualTo("DAT");
        assertThat(history.cua(student, lop, bai).trangThai().name()).isEqualTo("DA_NOP");
    }

    @SuppressWarnings("unchecked")
    static Map<String, @Nullable Object> response(Map<String, ?> payload, String result) {
        var steps = (List<Map<String, Object>>) payload.get("cac_buoc");
        var per = new LinkedHashMap<String, String>();
        steps.forEach(s -> per.put((String) s.get("ma_buoc"), result));
        Map<String, @Nullable Object> response = new LinkedHashMap<>();
        response.put("ket_qua", result); response.put("loai_ket_qua", result);
        response.put("buoc_sai", null); response.put("ma_loi", null); response.put("do_tin_cay", null);
        response.put("per_buoc", per); response.put("cac_van_de", List.of()); response.put("chuan_hoa", List.of());
        response.put("phien_ban_chuan_hoa", "test"); response.put("thong_bao", "Đã kiểm tra.");
        response.put("chua_xong", steps.size() < 5); response.put("nop_toi", payload.get("nop_toi"));
        return response;
    }
}
