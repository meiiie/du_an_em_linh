package vn.hoctapcanman.core.content.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.RecordComponent;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import vn.hoctapcanman.core.content.application.dto.hocsinh.DeBaiChoHocSinh;
import vn.hoctapcanman.core.content.domain.model.Level4;
import vn.hoctapcanman.core.content.domain.model.Problem;
import vn.hoctapcanman.core.content.domain.model.ProblemRelease;
import vn.hoctapcanman.core.content.domain.model.ReleaseStatus;
import vn.hoctapcanman.core.content.domain.repository.ProblemRepository;

/** T015: học sinh chỉ thấy bài đã phát hành ở lớp mình, và đề không mang lời giải. */
class DeBaiHocSinhServiceTest {

    private static final Instant LUC = Instant.parse("2026-10-04T08:00:00Z");
    private static final UUID LOP = UUID.randomUUID();
    private static final UUID LOP_KHAC = UUID.randomUUID();

    private final List<Problem> bai = new ArrayList<>();
    private final List<ProblemRelease> phatHanh = new ArrayList<>();
    private final DeBaiHocSinhService dichVu = new DeBaiHocSinhService(new KhoBai());

    @Test
    void chiBaiDaPhatHanhOLopMinh() {
        Problem daMo = them("DH12-03-VD-01", LOP, ReleaseStatus.DA_PHAT_HANH);
        them("DH12-01-TH-01", LOP, ReleaseStatus.CHO_GIAO_VIEN_DUYET);
        them("DH12-DEMO-CHAN-01", LOP, ReleaseStatus.BI_CHAN);
        them("DH12-NB-01", LOP, ReleaseStatus.NHAP);
        them("DH12-TH-02", LOP_KHAC, ReleaseStatus.DA_PHAT_HANH);

        assertThat(dichVu.baiDaPhatHanh(LOP)).extracting(DeBaiChoHocSinh::ma).containsExactly("DH12-03-VD-01");
        assertThat(dichVu.bai(LOP, "DH12-03-VD-01")).contains(new DeBaiChoHocSinh("DH12-03-VD-01", "T12.DH.03", "VAN_DUNG",
            daMo.statementText(), daMo.statementLatex(), Problem.TU_LUAN_5_BUOC, null));
        assertThat(dichVu.bai(LOP, "DH12-01-TH-01")).isEmpty();
        assertThat(dichVu.bai(LOP, "DH12-DEMO-CHAN-01")).isEmpty();
        assertThat(dichVu.bai(LOP, "DH12-TH-02")).isEmpty();
        assertThat(dichVu.bai(LOP, "KHONG-CO")).isEmpty();
        assertThat(dichVu.baiDaPhatHanh(UUID.randomUUID())).isEmpty();
    }

    @Test
    void deChoHocSinhKhongCoTruongLoiGiai() {
        assertThat(Arrays.stream(DeBaiChoHocSinh.class.getRecordComponents()).map(RecordComponent::getName))
            .containsExactly("ma", "kyNang", "mucDo", "deBai", "deBaiLatex", "dangTraLoi", "buocBatDau")
            .noneMatch(ten -> ten.toLowerCase().contains("giai") || ten.toLowerCase().contains("dapan"));
    }

    private Problem them(String ma, UUID lop, ReleaseStatus trangThai) {
        Problem p = new Problem(UUID.randomUUID(), ma, "T12.DH.03", List.of(), Level4.VAN_DUNG, null, null, null, "Đề " + ma,
            "y = x^3", null, Problem.TU_LUAN_5_BUOC, null, "SUPHAM", "a".repeat(64), null, LUC, LUC);
        bai.add(p);
        phatHanh.add(new ProblemRelease(lop, p.id(), trangThai, trangThai == ReleaseStatus.NHAP ? null : UUID.randomUUID(), LUC));
        return p;
    }

    private final class KhoBai implements ProblemRepository {
        @Override
        public void save(Problem problem) {
            bai.add(problem);
        }

        @Override
        public Optional<Problem> findById(UUID id) {
            return bai.stream().filter(p -> p.id().equals(id)).findFirst();
        }

        @Override
        public Optional<Problem> findByCode(String code) {
            return bai.stream().filter(p -> p.code().equals(code)).findFirst();
        }

        @Override
        public List<Problem> findAllById(Collection<UUID> ids) {
            return bai.stream().filter(p -> ids.contains(p.id())).sorted(Comparator.comparing(Problem::code)).toList();
        }

        @Override
        public Optional<Integer> findContentVersion(UUID problemId) {
            return findById(problemId).map(p -> 1);
        }

        @Override
        public List<Problem> findReleasedInClass(UUID classId) {
            return bai.stream().filter(p -> dangPhatHanh(classId, p)).sorted(Comparator.comparing(Problem::code)).toList();
        }

        @Override
        public Optional<Problem> findReleasedInClass(UUID classId, String code) {
            return bai.stream().filter(p -> p.code().equals(code) && dangPhatHanh(classId, p)).findFirst();
        }

        private boolean dangPhatHanh(UUID classId, Problem p) {
            return phatHanh.stream().anyMatch(r -> r.classId().equals(classId) && r.problemId().equals(p.id()) && r.visibleToStudents());
        }
    }
}
