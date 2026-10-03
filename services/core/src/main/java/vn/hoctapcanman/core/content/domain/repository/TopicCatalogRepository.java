package vn.hoctapcanman.core.content.domain.repository;

import java.util.List;
import java.util.Optional;
import vn.hoctapcanman.core.content.domain.model.ErrorType;
import vn.hoctapcanman.core.content.domain.model.Skill;
import vn.hoctapcanman.core.content.domain.model.SkillPrerequisite;
import vn.hoctapcanman.core.content.domain.model.StepTemplate;
import vn.hoctapcanman.core.content.domain.model.Topic;

/**
 * Danh mục của chủ đề: chủ đề, kỹ năng và tiên quyết, khung bước, mã lỗi. Ghi là ghi đè theo mã (importer chạy lại
 * không nhân bản). Thứ tự ghi theo khóa ngoại: chủ đề, rồi mọi kỹ năng, rồi tiên quyết, khung bước, mã lỗi.
 */
public interface TopicCatalogRepository {

    void saveTopic(Topic topic);

    void saveSkill(Skill skill);

    /** Thay toàn bộ tiên quyết của kỹ năng {@code skillCode}; mọi kỹ năng tiên quyết phải đã có. */
    void replacePrerequisites(String skillCode, List<SkillPrerequisite> prerequisites);

    void saveStepTemplate(StepTemplate step);

    void saveErrorType(ErrorType errorType);

    Optional<Topic> findTopic(String code);

    List<Skill> findSkills(String topicCode);

    List<SkillPrerequisite> findPrerequisites(String skillCode);

    /** Khung bước của chủ đề, theo thứ tự. */
    List<StepTemplate> findStepTemplates(String topicCode);

    List<ErrorType> findErrorTypes(String skillCode);
}
