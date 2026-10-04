package vn.hoctapcanman.core.content.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Testcontainers;
import vn.hoctapcanman.core.TestcontainersConfiguration;
import vn.hoctapcanman.core.content.domain.model.CheckStatus;
import vn.hoctapcanman.core.content.domain.model.Level4;
import vn.hoctapcanman.core.content.domain.model.Problem;
import vn.hoctapcanman.core.content.domain.model.Skill;
import vn.hoctapcanman.core.content.domain.model.TierResult;
import vn.hoctapcanman.core.content.domain.model.Topic;
import vn.hoctapcanman.core.content.domain.model.VerificationRun;

/**
 * Thứ tự khóa (Codex #121): giáo viên duyệt đúng lúc có người sửa bài. Lần sửa giữ khóa dòng bài rồi đánh dấu cũ lượt kiểm;
 * lần duyệt phải chờ ở dòng bài trước khi khóa dòng lượt, nếu không hai bên khóa chéo nhau và CSDL hủy một bên (deadlock).
 * Hai giao dịch thật trên hai luồng, nên test không chạy trong giao dịch của test và tự dọn dữ liệu.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({TestcontainersConfiguration.class, TopicCatalogRepositoryAdapter.class, ProblemRepositoryAdapter.class,
    VerificationRunRepositoryAdapter.class})
@Testcontainers(disabledWithoutDocker = true)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class KhoaThuTuTest {

    private static final Instant LUC = Instant.parse("2026-10-04T08:00:00Z");
    private static final String BAM = "a".repeat(64);

    @Autowired
    private TopicCatalogRepositoryAdapter catalog;

    @Autowired
    private ProblemRepositoryAdapter problems;

    @Autowired
    private VerificationRunRepositoryAdapter runs;

    @Autowired
    private JdbcClient jdbc;

    @Autowired
    private PlatformTransactionManager tx;

    private final UUID lop = UUID.randomUUID();
    private final UUID giaoVien = UUID.randomUUID();
    private Problem bai;

    @BeforeEach
    void nen() {
        catalog.saveTopic(new Topic("DH12", "Đơn điệu và cực trị", 12));
        catalog.saveSkill(new Skill("T12.DH.03", "DH12", "Xét dấu đạo hàm", null, 12, true));
        jdbc.sql("insert into classes (id, name, grade, school_year, created_at) values (?, '12A8 khóa thứ tự', 12, '2026-2027', now())")
            .params(lop).update();
        jdbc.sql("""
                insert into users (id, email, password_hash, display_name, role, created_at, updated_at)
                values (?, ?, 'x', 'Giáo viên khóa thứ tự', 'TEACHER', now(), now())""")
            .params(giaoVien, "gv." + giaoVien + "@test.local").update();
        bai = new Problem(UUID.randomUUID(), "KT-" + lop.toString().substring(0, 8), "T12.DH.03", List.of(), Level4.VAN_DUNG, null, null,
            null, "Đề", "y", null, Problem.TU_LUAN_5_BUOC, null, "SUPHAM", BAM, null, LUC, LUC);
        problems.save(bai);
    }

    @AfterEach
    void don() {
        jdbc.sql("delete from classes where id = ?").params(lop).update();
        jdbc.sql("delete from problems where id = ?").params(bai.id()).update();
        jdbc.sql("delete from users where id = ?").params(giaoVien).update();
    }

    @Test
    void duyetChoDongBaiTruocKhiKhoaDongLuot() throws Exception {
        // Lớp chưa khóa bảng nào: lượt ba tầng KHONG_KIEM_DUOC, không bảng, chờ giáo viên duyệt.
        VerificationRun cho = VerificationRun.forProblem(lop, bai.id(), BAM, 1, null,
            List.of(TierResult.of(1, CheckStatus.KHONG_KIEM_DUOC), TierResult.of(2, CheckStatus.KHONG_KIEM_DUOC),
                TierResult.of(3, CheckStatus.KHONG_KIEM_DUOC)), List.of(), LUC);
        runs.save(cho);
        VerificationRun.Approval duyet = cho.approve(giaoVien, "Đã đối chiếu, đúng.", true, BAM, null, LUC);

        CountDownLatch giuBai = new CountDownLatch(1);
        CountDownLatch danhDau = new CountDownLatch(1);
        // Lần «sửa bài»: giữ khóa dòng bài, rồi đánh dấu cũ lượt như vo_hieu_ket_qua_bai.
        CompletableFuture<Void> sua = CompletableFuture.runAsync(() -> new TransactionTemplate(tx).executeWithoutResult(t -> {
            jdbc.sql("select 1 from problems where id = ? for no key update").params(bai.id()).query(Integer.class).single();
            giuBai.countDown();
            try {
                danhDau.await(10, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            jdbc.sql("update verification_runs set stale = true where id = ?").params(cho.id()).update();
        }));
        assertThat(giuBai.await(10, TimeUnit.SECONDS)).isTrue();

        CompletableFuture<Void> duyetXong = CompletableFuture.runAsync(() -> runs.saveApproval(duyet));
        Thread.sleep(500);
        assertThat(duyetXong).isNotDone();

        danhDau.countDown();
        // Lần sửa không bị chặn ở dòng lượt (lần duyệt chưa khóa nó), nên commit được; không bên nào thua deadlock.
        sua.get(10, TimeUnit.SECONDS);
        // Lần duyệt chạy tiếp sau commit: lượt đã cũ nên không còn gì để duyệt.
        assertThat(duyetXong).failsWithin(10, TimeUnit.SECONDS).withThrowableThat().havingCause()
            .isInstanceOf(IllegalStateException.class).withMessageContaining("không còn chờ duyệt");
        assertThat(runs.findById(cho.id()).orElseThrow().stale()).isTrue();
        assertThat(runs.findReview(cho.id())).isEmpty();
    }
}
