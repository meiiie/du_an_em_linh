package vn.hoctapcanman.core.content.infrastructure.persistence;
import static org.assertj.core.api.Assertions.*;
import org.junit.jupiter.api.Test;
class FreeResponseReferenceTest {
    @Test void readsJsonStringNumberAndMissingAnswerWithoutPublishingSolution() {
        assertThat(KhungChamRepositoryAdapter.answerText("\"72\"")).isEqualTo("72");
        assertThat(KhungChamRepositoryAdapter.answerText("72")).isEqualTo("72");
        assertThat(KhungChamRepositoryAdapter.answerText("null")).isEmpty();
        assertThat(KhungChamRepositoryAdapter.answerText(null)).isEmpty();
        assertThat(KhungChamRepositoryAdapter.answerText("\"broken")).isEmpty();
    }
}
