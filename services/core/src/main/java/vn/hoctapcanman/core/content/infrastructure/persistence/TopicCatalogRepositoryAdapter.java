package vn.hoctapcanman.core.content.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import vn.hoctapcanman.core.content.domain.model.ErrorType;
import vn.hoctapcanman.core.content.domain.model.InputKind;
import vn.hoctapcanman.core.content.domain.model.Skill;
import vn.hoctapcanman.core.content.domain.model.SkillPrerequisite;
import vn.hoctapcanman.core.content.domain.model.StepTemplate;
import vn.hoctapcanman.core.content.domain.model.Topic;
import vn.hoctapcanman.core.content.domain.repository.TopicCatalogRepository;

/** Danh mục của chủ đề trên bảng {@code topics}, {@code skills}, {@code skill_prerequisites}, {@code step_templates}, {@code error_types}. */
@Repository
public class TopicCatalogRepositoryAdapter implements TopicCatalogRepository {

    private final JdbcClient jdbc;

    public TopicCatalogRepositoryAdapter(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void saveTopic(Topic topic) {
        jdbc.sql("""
                insert into topics (code, name, grade) values (:code, :name, :grade)
                on conflict (code) do update set name = excluded.name, grade = excluded.grade""")
            .param("code", topic.code()).param("name", topic.name()).param("grade", topic.grade())
            .update();
    }

    @Override
    public void saveSkill(Skill skill) {
        jdbc.sql("""
                insert into skills (code, topic_code, name, description, grade, is_core)
                values (:code, :topic, :name, :description, :grade, :core)
                on conflict (code) do update set topic_code = excluded.topic_code, name = excluded.name,
                    description = excluded.description, grade = excluded.grade, is_core = excluded.is_core""")
            .param("code", skill.code()).param("topic", skill.topicCode()).param("name", skill.name())
            .param("description", skill.description()).param("grade", skill.grade()).param("core", skill.core())
            .update();
    }

    @Override
    @Transactional
    public void replacePrerequisites(String skillCode, List<SkillPrerequisite> prerequisites) {
        if (prerequisites.stream().anyMatch(p -> !p.skillCode().equals(skillCode))) {
            throw new IllegalArgumentException("Tiên quyết phải của đúng kỹ năng " + skillCode);
        }
        jdbc.sql("delete from skill_prerequisites where skill_code = :skill").param("skill", skillCode).update();
        for (SkillPrerequisite p : prerequisites) {
            jdbc.sql("insert into skill_prerequisites (skill_code, prerequisite_code, min_level) values (:skill, :pre, :min)")
                .param("skill", p.skillCode()).param("pre", p.prerequisiteCode()).param("min", p.minLevel())
                .update();
        }
    }

    @Override
    public void saveStepTemplate(StepTemplate step) {
        jdbc.sql("""
                insert into step_templates (step_code, topic_code, ordinal, input_kind, skill_code, description)
                values (:code, :topic, :ordinal, :input, :skill, :description)
                on conflict (step_code) do update set topic_code = excluded.topic_code, ordinal = excluded.ordinal,
                    input_kind = excluded.input_kind, skill_code = excluded.skill_code, description = excluded.description""")
            .param("code", step.stepCode()).param("topic", step.topicCode()).param("ordinal", step.ordinal())
            .param("input", step.inputKind().name()).param("skill", step.skillCode()).param("description", step.description())
            .update();
    }

    @Override
    public void saveErrorType(ErrorType e) {
        jdbc.sql("""
                insert into error_types (code, skill_code, step_code, name, fix_hint, result_types)
                values (:code, :skill, :step, :name, :fix, string_to_array(:results, ','))
                on conflict (code) do update set skill_code = excluded.skill_code, step_code = excluded.step_code,
                    name = excluded.name, fix_hint = excluded.fix_hint, result_types = excluded.result_types""")
            .param("code", e.code()).param("skill", e.skillCode()).param("step", e.stepCode()).param("name", e.name())
            .param("fix", e.fixHint()).param("results", Cot.noiMa(e.resultTypes()))
            .update();
    }

    @Override
    public Optional<Topic> findTopic(String code) {
        return jdbc.sql("select code, name, grade from topics where code = :code").param("code", code)
            .query((rs, n) -> new Topic(Cot.chu(rs, "code"), Cot.chu(rs, "name"), rs.getInt("grade")))
            .optional();
    }

    @Override
    public List<Skill> findSkills(String topicCode) {
        return jdbc.sql("select code, topic_code, name, description, grade, is_core from skills where topic_code = :topic order by code")
            .param("topic", topicCode)
            .query((rs, n) -> new Skill(Cot.chu(rs, "code"), Cot.chu(rs, "topic_code"), Cot.chu(rs, "name"),
                rs.getString("description"), Cot.soNeuCo(rs, "grade"), rs.getBoolean("is_core")))
            .list();
    }

    @Override
    public List<SkillPrerequisite> findPrerequisites(String skillCode) {
        return jdbc.sql("""
                select skill_code, prerequisite_code, min_level from skill_prerequisites
                where skill_code = :skill order by prerequisite_code""")
            .param("skill", skillCode)
            .query((rs, n) -> new SkillPrerequisite(Cot.chu(rs, "skill_code"), Cot.chu(rs, "prerequisite_code"), rs.getString("min_level")))
            .list();
    }

    @Override
    public List<StepTemplate> findStepTemplates(String topicCode) {
        return jdbc.sql("""
                select step_code, topic_code, ordinal, input_kind, skill_code, description from step_templates
                where topic_code = :topic order by ordinal""")
            .param("topic", topicCode)
            .query((rs, n) -> new StepTemplate(Cot.chu(rs, "step_code"), Cot.chu(rs, "topic_code"), rs.getInt("ordinal"),
                InputKind.valueOf(Cot.chu(rs, "input_kind")), rs.getString("skill_code"), Cot.chu(rs, "description")))
            .list();
    }

    @Override
    public List<ErrorType> findErrorTypes(String skillCode) {
        return jdbc.sql("""
                select code, skill_code, step_code, name, fix_hint, result_types from error_types
                where skill_code = :skill order by code""")
            .param("skill", skillCode)
            .query((rs, n) -> new ErrorType(Cot.chu(rs, "code"), rs.getString("skill_code"), rs.getString("step_code"),
                Cot.chu(rs, "name"), rs.getString("fix_hint"), Cot.mang(rs, "result_types")))
            .list();
    }
}
