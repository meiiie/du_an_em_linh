package vn.hoctapcanman.core.workspace.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import vn.hoctapcanman.core.workspace.application.dto.WorkspaceDto.Item;

class BankGradeTest {
    @Test void lop12KhongThayBaiKhoi10DuDaPhatHanhChoLop() {
        var grade10 = row("NT-BH10-NB-01", 10);
        var grade11 = row("NT-DH11-TH-02", 11);
        var grade12 = row("NT-DH12-NB-01", 12);
        assertThat(WorkspaceStoreAdapter.bankForGrade(List.of(grade10, grade11, grade12), 12))
            .containsExactly(grade12);
        assertThat(WorkspaceStoreAdapter.bankForGrade(List.of(grade10, grade11, grade12), 10))
            .containsExactly(grade10);
        assertThat(WorkspaceStoreAdapter.bankForGrade(List.of(), 12)).isEmpty();
    }
    private Item row(String code, int grade) {
        return new Item(code, code, "Đề", "DA_PHAT_HANH", Map.of("grade", grade));
    }
}
