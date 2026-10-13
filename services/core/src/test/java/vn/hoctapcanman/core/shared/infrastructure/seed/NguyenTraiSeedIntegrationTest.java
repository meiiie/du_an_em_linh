package vn.hoctapcanman.core.shared.infrastructure.seed;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import vn.hoctapcanman.core.classroom.infrastructure.seed.LopNguyenTraiSeeder;
import vn.hoctapcanman.core.content.infrastructure.nhap.NoiDungNguyenTraiSeeder;
import vn.hoctapcanman.core.identity.infrastructure.seed.TaiKhoanNguyenTraiSeeder;
import vn.hoctapcanman.core.mastery.infrastructure.seed.MucHieuNguyenTraiSeeder;
import vn.hoctapcanman.core.practice.infrastructure.seed.GiaoBaiNguyenTraiSeeder;
import vn.hoctapcanman.core.practice.infrastructure.seed.LichSuNguyenTraiSeeder;

/** Opt-in PostgreSQL thật + CAS thật, schema riêng được Flyway tạo; không đụng dữ liệu DB hiện có. */
@SpringBootTest
@ActiveProfiles("dev")
@DirtiesContext
@EnabledIfEnvironmentVariable(named="MATHL_SEED_INTEGRATION", matches="1")
class NguyenTraiSeedIntegrationTest {
    private static final String SCHEMA="seed_test_"+UUID.randomUUID().toString().replace("-","");
    @DynamicPropertySource static void configure(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url",()->"jdbc:postgresql://localhost:5432/hoc_toan_core?currentSchema="+SCHEMA);
        r.add("spring.flyway.schemas",()->SCHEMA);
        r.add("spring.flyway.default-schema",()->SCHEMA);
        r.add("MATHL_DEV_SEED_PASSWORD",()->UUID.randomUUID().toString());
    }
    @Autowired JdbcClient db;
    @Autowired DataSource dataSource;
    @Autowired TaiKhoanNguyenTraiSeeder accounts;
    @Autowired LopNguyenTraiSeeder classes;
    @Autowired NoiDungNguyenTraiSeeder content;
    @Autowired GiaoBaiNguyenTraiSeeder assignments;
    @Autowired LichSuNguyenTraiSeeder history;
    @Autowired MucHieuNguyenTraiSeeder mastery;
    @Autowired vn.hoctapcanman.core.workspace.application.port.WorkspaceStore workspace;

    @Test void completeSeedAndSecondRunPreservesData() throws Exception {
        try {
            assertThat(count("users")).isEqualTo(35);
            assertThat(count("classes")).isEqualTo(3);
            assertThat(count("enrollments")).isEqualTo(36);
            assertThat(count("problems")).isEqualTo(74);
            assertThat(count("documents")).isEqualTo(21);
            assertThat(count("formula_sheets")).isEqualTo(6);
            assertThat(count("assignments")).isEqualTo(690);
            assertThat(count("submissions")).isEqualTo(390);
            assertThat(count("study_sessions")).isEqualTo(120);
            assertThat(count("mastery_states")).isEqualTo(160);
            assertThat(count("mastery_events")).isEqualTo(322);
            assertThat(db.sql("SELECT count(*) FROM submissions WHERE result='DAT'").query(Integer.class).single()).isEqualTo(227);
            assertThat(db.sql("SELECT count(*) FROM submissions WHERE result='SAI'").query(Integer.class).single()).isEqualTo(95);
            assertThat(count("tutor_review_queue")).isEqualTo(1);
            assertThat(db.sql("SELECT count(*) FROM users WHERE NOT synthetic").query(Integer.class).single()).isZero();
            assertThat(db.sql("SELECT DISTINCT level4 FROM mastery_states").query(String.class).list())
                .containsExactlyInAnyOrder("NHAN_BIET","THONG_HIEU","VAN_DUNG","VAN_DUNG_CAO");
            assertThat(db.sql("SELECT count(*) FROM assignments WHERE mode='KIEM_TRA' AND due_at>now() AND due_at<now()+interval '24 hours'").query(Integer.class).single()).isEqualTo(240);
            assertThat(db.sql("SELECT count(*) FROM submissions WHERE result='KHONG_KIEM_DUOC'").query(Integer.class).single()).isEqualTo(68);
            assertThat(db.sql("SELECT count(*) FROM problem_releases WHERE status='CHO_GIAO_VIEN_DUYET'").query(Integer.class).single()).isEqualTo(5);
            assertThat(count("content_reviews")).isEqualTo(61);
            assertThat(db.sql("""
                SELECT count(*) FROM assignments a JOIN classes c ON c.id=a.class_id
                JOIN problems p ON p.id=a.problem_id JOIN skills sk ON sk.code=p.skill_code
                JOIN topics topic ON topic.code=sk.topic_code
                WHERE topic.grade<>c.grade
                """).query(Integer.class).single()).isZero();
            for (String name:TruongNguyenTrai.LOP) {
                var dashboard=workspace.dashboard(TruongNguyenTrai.id("lop:"+name));
                int grade = Integer.parseInt(name.substring(0,2));
                int skillCount = db.sql("SELECT count(*) FROM skills WHERE grade=:grade AND is_core")
                    .param("grade",grade).query(Integer.class).single();
                assertThat(dashboard.distribution().values().stream().mapToLong(Long::longValue).sum()).isEqualTo(skillCount*10L);
                assertThat(dashboard.deadlines()).hasSize(name.equals("10A1") ? 4 : name.equals("11A2") ? 7 : 5);
                var test = dashboard.deadlines().stream().filter(row -> row.status().equals("KIEM_TRA")).findFirst().orElseThrow();
                assertThat(test.title()).isEqualTo("Kiểm tra hàm số — 45 phút");
                assertThat(test.details()).containsEntry("problemCount",8).containsEntry("assignmentCount",80)
                    .containsEntry("submittedCount",0);
                assertThat(workspace.bank(TruongNguyenTrai.id("lop:"+name))).allSatisfy(row -> {
                    assertThat(row.details()).containsEntry("grade",grade);
                    assertThat(row.details().get("skillName")).isInstanceOf(String.class).isNotEqualTo("Kỹ năng toán học");
                });
                assertThat(dashboard.deadlines()).allSatisfy(row -> {
                    assertThat(row.title()).doesNotStartWith("NT-");
                    assertThat(row.details()).containsKeys("tenBo","setKey","problemCodes","problemCount","studentCount","assignmentCount","submittedCount");
                    assertThat(row.details().get("studentCount")).isEqualTo(10);
                });
                assertThat(workspace.assignments(TruongNguyenTrai.id("lop:"+name)))
                    .allSatisfy(row -> assertThat(row.title()).doesNotStartWith("NT-"));
            }
            assertThat(db.sql("""
                SELECT count(*) FROM (
                    SELECT a.student_id FROM assignments a JOIN problems p ON p.id=a.problem_id
                    WHERE p.code LIKE 'NT-LUYEN%' AND a.mode='ON_TAP' AND a.due_at>now()
                    AND EXISTS(SELECT 1 FROM step_templates st WHERE st.skill_code=p.skill_code)
                    AND NOT EXISTS(SELECT 1 FROM submissions s WHERE s.student_id=a.student_id
                        AND s.class_id=a.class_id AND s.problem_id=a.problem_id AND s.status='DA_NOP')
                    GROUP BY a.student_id HAVING count(*) BETWEEN 2 AND 4
                ) ready
                """).query(Integer.class).single()).isEqualTo(30);
            Map<String,List<String>> before=snapshot();
            for(ApplicationRunner seed:List.of(accounts,classes,content,assignments,history,mastery)) seed.run(new DefaultApplicationArguments());
            assertThat(snapshot()).isEqualTo(before);
            UUID cl=TruongNguyenTrai.id("lop:12A3");
            assertThat(LopSeedAnToan.kiem(db,"12A3")).isTrue();
            UUID teacher=db.sql("SELECT user_id FROM enrollments WHERE class_id=:cl AND role_in_class='TEACHER' LIMIT 1")
                .param("cl",cl).query(UUID.class).single();
            db.sql("UPDATE users SET synthetic=false WHERE id=:id").param("id",teacher).update();
            assertThat(LopSeedAnToan.kiem(db,"12A3")).isFalse();
            before=snapshot();
            for(ApplicationRunner seed:List.of(accounts,classes,content,assignments,history,mastery)) seed.run(new DefaultApplicationArguments());
            assertThat(snapshot()).isEqualTo(before);
        } finally {
            db.sql("DROP SCHEMA "+SCHEMA+" CASCADE").update();
        }
    }
    private int count(String table) {return db.sql("SELECT count(*) FROM "+table).query(Integer.class).single();}
    private Map<String,List<String>> snapshot() {
        var result=new java.util.LinkedHashMap<String,List<String>>();
        for(String table:List.of("users","classes","enrollments","class_settings","documents","document_passages","formula_sheets","formulas","problems","solutions","verification_runs","content_reviews","verification_tier_results","problem_releases","assignments","submissions","grading_results","submission_steps","submission_tables","submission_table_cells","mastery_states","mastery_events","study_sessions","escalations","tutor_review_queue")) {
            result.put(table,db.sql("SELECT row_to_json(t)::text FROM "+table+" t ORDER BY row_to_json(t)::text").query(String.class).list());
        }
        return result;
    }
}
