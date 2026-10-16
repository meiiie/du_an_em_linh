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
    @Test void structuredAndNonNumericJsonTypesAreLeftForTeacherReview() {
        for(String raw : new String[]{"[3,4]", "{\"H\":[\"2/9\",\"-1/9\",\"20/9\"],\"khoang_cach\":\"8/3\"}", "[]", "{}", "true", "false"}) {
            assertThat(KhungChamRepositoryAdapter.answerText(raw)).isEmpty();
        }
    }
}
