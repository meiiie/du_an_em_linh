package vn.hoctapcanman.core.practice.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
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
import vn.hoctapcanman.core.practice.domain.model.GradeStatus;
import vn.hoctapcanman.core.practice.domain.model.GradingResult;
import vn.hoctapcanman.core.practice.domain.model.Submission;

/**
 * Hai tab của cùng học sinh (T022): mở bài làm cùng lúc thì nhận cùng một bài làm; ghi cùng một yêu cầu chấm cùng lúc thì
 * chỉ một dòng. Giao dịch đầu giữ chỗ chưa commit, giao dịch sau phải chờ ở chỉ mục duy nhất từng phần (test xác nhận nó
 * đang chờ khóa), rồi không ghi mà đọc lại dòng của giao dịch đầu. Hai giao dịch thật trên hai luồng, nên test không chạy
 * trong giao dịch của test và tự dọn dữ liệu.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({TestcontainersConfiguration.class, SubmissionRepositoryAdapter.class, GradingResultRepositoryAdapter.class})
@Testcontainers(disabledWithoutDocker = true)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class PracticeDongThoiTest {

    private static final Instant LUC = Instant.parse("2026-10-05T08:00:00Z");

    @Autowired
    private SubmissionRepositoryAdapter submissions;

    @Autowired
    private GradingResultRepositoryAdapter grades;

    @Autowired
    private JdbcClient jdbc;

    @Autowired
    private PlatformTransactionManager tx;

    private UUID lop;
    private UUID an;
    private UUID bai;

    @BeforeEach
    void duLieu() {
        DuLieuPractice.danhMuc(jdbc);
        lop = DuLieuPractice.lop(jdbc);
        an = DuLieuPractice.nguoi(jdbc, "STUDENT");
        DuLieuPractice.ghiDanh(jdbc, lop, an, "STUDENT");
        bai = DuLieuPractice.bai(jdbc, "PR-DT");
        DuLieuPractice.phatHanh(jdbc, lop, bai);
    }

    @AfterEach
    void don() {
        jdbc.sql("delete from classes where id = ?").params(lop).update();
        jdbc.sql("delete from problems where id = ?").params(bai).update();
        jdbc.sql("delete from users where id = ?").params(an).update();
    }

    @Test
    void haiTabMoCungLucNhanCungMotBaiLam() throws Exception {
        List<Submission> ketQua = haiGiaoDichChongNhau(
            () -> submissions.openOrGet(Submission.open(lop, an, bai, 1, LUC)),
            () -> submissions.openOrGet(Submission.open(lop, an, bai, 1, LUC.plusSeconds(1))));
        assertThat(ketQua.get(1)).isEqualTo(ketQua.get(0));
        assertThat(jdbc.sql("select count(*) from submissions where student_id = ?").params(an).query(Integer.class).single()).isOne();
    }

    @Test
    void haiTabGhiCungYeuCauChamChiMotDong() throws Exception {
        UUID bl = submissions.openOrGet(Submission.open(lop, an, bai, 1, LUC)).id();
        String bam = "f".repeat(64);
        List<GradingResult> ketQua = haiGiaoDichChongNhau(
            () -> grades.record(ketQua(bl, bam, GradeStatus.SAI)),
            () -> grades.record(ketQua(bl, bam, GradeStatus.SAI)));
        assertThat(ketQua.get(1)).isEqualTo(ketQua.get(0));
        assertThat(grades.bySubmission(bl)).hasSize(1);
    }

    /**
     * Giao dịch đầu chạy {@code dau} rồi giữ, chưa commit; giao dịch sau chạy {@code sau} trên luồng khác. Khi thấy giao dịch
     * sau đang chờ khóa thì cho giao dịch đầu commit. Trả kết quả của hai giao dịch.
     */
    private <T> List<T> haiGiaoDichChongNhau(Supplier<T> dau, Supplier<T> sau) throws Exception {
        CountDownLatch daGhi = new CountDownLatch(1);
        CountDownLatch choCommit = new CountDownLatch(1);
        CompletableFuture<T> mot = CompletableFuture.supplyAsync(() -> new TransactionTemplate(tx).execute(t -> {
            T kq = dau.get();
            daGhi.countDown();
            try {
                assertThat(choCommit.await(30, TimeUnit.SECONDS)).isTrue();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(e);
            }
            return kq;
        }));
        assertThat(daGhi.await(30, TimeUnit.SECONDS)).isTrue();
        CompletableFuture<T> hai = CompletableFuture.supplyAsync(() -> new TransactionTemplate(tx).execute(t -> sau.get()));
        choDenKhiCoGiaoDichChoKhoa(Duration.ofSeconds(30));
        assertThat(hai).isNotDone();
        choCommit.countDown();
        return List.of(mot.get(30, TimeUnit.SECONDS), hai.get(30, TimeUnit.SECONDS));
    }

    private void choDenKhiCoGiaoDichChoKhoa(Duration toiDa) throws InterruptedException {
        long het = System.nanoTime() + toiDa.toNanos();
        while (System.nanoTime() < het) {
            int dangCho = jdbc.sql("""
                    select count(*) from pg_stat_activity where datname = current_database() and wait_event_type = 'Lock'""")
                .query(Integer.class).single();
            if (dangCho > 0) {
                return;
            }
            Thread.sleep(20);
        }
        throw new AssertionError("Giao dịch sau không chờ khóa của giao dịch đầu");
    }

    private static GradingResult ketQua(UUID bl, String bam, GradeStatus kq) {
        return new GradingResult(UUID.randomUUID(), bl, "B.DH.DAOHAM", bam, kq, null, null, null, null, Map.of(), "thông báo", null, null,
            false, null, null, LUC);
    }
}
