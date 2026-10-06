package vn.hoctapcanman.core.mastery.domain.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import vn.hoctapcanman.core.mastery.domain.model.BktConfig;
import vn.hoctapcanman.core.mastery.domain.model.MasteryEvent;
import vn.hoctapcanman.core.mastery.domain.model.MasteryState;

/** Lưu trữ của module mức hiểu: tham số BKT, trạng thái theo (học sinh, kỹ năng), sự kiện theo bài làm. */
public interface MasteryRepository {

    /** Tham số đang dùng (dòng {@code bkt} của {@code mastery_config}); thiếu dòng là lỗi triển khai. */
    BktConfig config();

    Optional<MasteryEvent> eventOf(UUID submissionId);

    /**
     * Khóa dòng (học sinh, kỹ năng) tới hết giao dịch; chưa có thì ghi {@code initial} rồi khóa. Hai bài nộp cùng lúc vào một
     * kỹ năng được tính lần lượt, bài sau tính trên trạng thái bài trước để lại.
     */
    MasteryState lock(MasteryState initial);

    /** Ghi trạng thái sau bài và sự kiện của bài trong giao dịch của bên gọi; bài làm đã có sự kiện thì ném lỗi. */
    void save(MasteryState after, MasteryEvent event);

    /** Mọi trạng thái của các học sinh này. */
    List<MasteryState> statesOf(Collection<UUID> studentIds);
}
