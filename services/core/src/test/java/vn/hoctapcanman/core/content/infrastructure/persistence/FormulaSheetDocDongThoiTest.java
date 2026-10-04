package vn.hoctapcanman.core.content.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
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
import vn.hoctapcanman.core.content.domain.model.Formula;
import vn.hoctapcanman.core.content.domain.model.FormulaSheet;

/**
 * Đọc bảng nháp trong lúc có người đang ghi (Codex #121): lần đọc phải chờ lần ghi commit rồi mới đọc, để không trộn thông
 * tin bảng cũ với dòng mới. Hai giao dịch thật trên hai luồng, nên test không chạy trong giao dịch của test và tự dọn lớp.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({TestcontainersConfiguration.class, FormulaSheetRepositoryAdapter.class})
@Testcontainers(disabledWithoutDocker = true)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class FormulaSheetDocDongThoiTest {

    private static final Instant LUC = Instant.parse("2026-10-04T08:00:00Z");

    @Autowired
    private FormulaSheetRepositoryAdapter sheets;

    @Autowired
    private JdbcClient jdbc;

    @Autowired
    private PlatformTransactionManager tx;

    private final UUID lop = UUID.randomUUID();

    @AfterEach
    void don() {
        jdbc.sql("delete from classes where id = ?").params(lop).update();
    }

    @Test
    void docBangNhapChoLanGhiDangDoCommitRoiMoiDoc() throws Exception {
        jdbc.sql("insert into classes (id, name, grade, school_year, created_at) values (?, '12A9 đọc đồng thời', 12, '2026-2027', now())")
            .params(lop).update();
        FormulaSheet cu = FormulaSheet.draft(lop, 1, "bản cũ", List.of(dong(1, "d-1", "cũ")), LUC);
        sheets.save(cu);
        FormulaSheet moi = new FormulaSheet(cu.id(), lop, 1, cu.status(), "bản mới", null, null, null, LUC,
            List.of(dong(1, "d-1", "mới"), dong(2, "d-2", "mới")));

        CountDownLatch daGhi = new CountDownLatch(1);
        CountDownLatch choCommit = new CountDownLatch(1);
        CompletableFuture<Void> ghi = CompletableFuture.runAsync(() -> new TransactionTemplate(tx).executeWithoutResult(t -> {
            sheets.save(moi);
            daGhi.countDown();
            try {
                choCommit.await(10, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }));
        assertThat(daGhi.await(10, TimeUnit.SECONDS)).isTrue();

        CompletableFuture<Optional<FormulaSheet>> doc = CompletableFuture.supplyAsync(() -> sheets.findDraft(lop));
        Thread.sleep(500);
        // Lần ghi còn giữ khóa dòng bảng: lần đọc phải đang chờ, không trả bảng cũ hay bảng trộn.
        assertThat(doc).isNotDone();

        choCommit.countDown();
        ghi.get(10, TimeUnit.SECONDS);
        assertThat(doc.get(10, TimeUnit.SECONDS)).contains(moi);
    }

    private static Formula dong(int thuTu, String ma, String ban) {
        // Không gắn kỹ năng: test này commit thật, không nạp danh mục chủ đề.
        return Formula.unchecked(thuTu, ma, null, "Dòng " + ma + " " + ban, "x", "Phát biểu " + ban + " của " + ma + ".");
    }
}
