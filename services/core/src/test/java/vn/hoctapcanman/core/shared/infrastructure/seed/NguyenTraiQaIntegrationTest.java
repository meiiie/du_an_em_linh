package vn.hoctapcanman.core.shared.infrastructure.seed;

import static org.assertj.core.api.Assertions.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.jdbc.core.simple.JdbcClient;
import vn.hoctapcanman.core.content.application.port.BaiDeLam;
import vn.hoctapcanman.core.identity.application.port.PasswordHasher;
import vn.hoctapcanman.core.practice.application.dto.*;
import vn.hoctapcanman.core.practice.application.service.YeuCauCham;
import vn.hoctapcanman.core.practice.application.usecase.*;
import vn.hoctapcanman.core.practice.infrastructure.seed.DemoNamSeeder;
import vn.hoctapcanman.core.workspace.application.dto.WorkspaceDto.*;
import vn.hoctapcanman.core.workspace.application.port.WorkspaceStore;
import vn.hoctapcanman.core.workspace.application.service.WorkspaceService;

/** PostgreSQL + Flyway + CAS + toàn bộ data/dev và seed-nguyen-trai thật; không cần Docker. */
@SpringBootTest @ActiveProfiles("dev") @DirtiesContext
@EnabledIfEnvironmentVariable(named="MATHL_SEED_INTEGRATION",matches="1")
class NguyenTraiQaIntegrationTest {
    static final String SCHEMA="qa_seed_"+UUID.randomUUID().toString().replace("-","");
    @DynamicPropertySource static void configure(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url",()->"jdbc:postgresql://localhost:5432/hoc_toan_core?currentSchema="+SCHEMA);
        r.add("spring.flyway.schemas",()->SCHEMA);
        r.add("spring.flyway.default-schema",()->SCHEMA);
        r.add("app.content.source",()->"../../data");
        r.add("MATHL_DEV_SEED_PASSWORD",()->UUID.randomUUID().toString());
    }
    @Autowired JdbcClient db;
    @Autowired WorkspaceStore store;
    @Autowired WorkspaceService service;
    @Autowired BaiDeLam content;
    @Autowired DemoNamSeeder demo;
    @Autowired NopBuocUseCase check;
    @Autowired NopBaiUseCase submit;
    @Autowired GetBaiCuaHocSinhUseCase studentAssignments;
    @Autowired PasswordHasher passwords;

    @Test void seedExercisesSelfStudyFreeResponseAndAdministration()throws Exception {
        try {
            UUID cl=TruongNguyenTrai.id("lop:12A3");
            UUID nam=db.sql("SELECT id FROM users WHERE email='nam.nguyen.hoai@thpt-nguyentrai.edu.vn'").query(UUID.class).single();
            UUID teacher=db.sql("SELECT user_id FROM enrollments WHERE class_id=:cl AND role_in_class='TEACHER' LIMIT 1").param("cl",cl).query(UUID.class).single();
            UUID admin=db.sql("SELECT id FROM users WHERE role='ADMIN' LIMIT 1").query(UUID.class).single();
            assertThat(store.modes(nam,cl)).anyMatch(i->i.status().equals("KIEM_TRA")&&Boolean.TRUE.equals(i.details().get("conHan")));
            assertThat(store.recommendations(nam,cl)).anyMatch(i->!i.id().isBlank());
            assertThat(store.dashboard(cl).stuck()).isEqualTo((int)store.stuckAlerts(cl).stream().map(i->i.details().get("studentId")).distinct().count());
            var before=db.sql("SELECT row_to_json(a)::text FROM assignments a WHERE student_id=:hs ORDER BY id").param("hs",nam).query(String.class).list();
            demo.run(new DefaultApplicationArguments());
            assertThat(db.sql("SELECT row_to_json(a)::text FROM assignments a WHERE student_id=:hs ORDER BY id").param("hs",nam).query(String.class).list()).isEqualTo(before);
            for(String topic:List.of("KG12","TP12","XS12")) {
                var rows=store.selfStudy(nam,cl,topic);
                assertThat(rows).as("Tự học %s từ seed thật",topic).isNotEmpty();
                for(var row:rows) {
                    assertThat(db.sql("""
                        SELECT count(*) FROM assignments a JOIN problems p ON p.id=a.problem_id
                        WHERE a.student_id=:hs AND a.class_id=:cl AND p.code=:code AND a.status='DA_GIAO'
                        """).param("hs",nam).param("cl",cl).param("code",row.id()).query(Integer.class).single()).isZero();
                }
                String code=rows.getFirst().id();
                if(topic.equals("XS12")) {
                    db.sql("INSERT INTO review_blocks(run_id,teacher_id,reason,created_at) SELECT r.run_id,:teacher,'Kiểm tra lọc bài bị chặn',now() FROM problem_releases r JOIN problems p ON p.id=r.problem_id WHERE r.class_id=:cl AND p.code=:code")
                        .param("teacher",teacher).param("cl",cl).param("code",code).update();
                    assertThat(store.selfStudy(nam,cl,topic)).noneMatch(i->i.id().equals(code));
                    assertThatThrownBy(()->service.chooseStudy(nam,code)).isInstanceOf(vn.hoctapcanman.core.workspace.application.exception.WorkspaceException.class);
                    continue;
                }
                service.chooseStudy(nam,code);
                assertThat(store.selfStudy(nam,cl,topic)).noneMatch(i->i.id().equals(code));
            }
            var page=service.bankPage(teacher,cl,"tich phan","","","",1,2);
            assertThat(page.items()).hasSize(2);assertThat(page.total()).isGreaterThan(2);
            assertThat(page.items()).allSatisfy(i->assertThat(i.details()).containsEntry("grade",12));
            assertThat(service.bankPage(teacher,cl,"tich phan","","","",2,2).items()).noneMatch(i->page.items().stream().anyMatch(first->first.id().equals(i.id())));
            var geometry=content.bai(cl,"NT-KG12-NB-01").orElseThrow();
            assertThat(geometry.moTaBuoc()).singleElement().satisfies(s->assertThat(s.kieu()).isEqualTo("TU_DO"));
            var internal=content.duKienCham(geometry.problemId(),geometry.phienBan());
            assertThat(internal).containsEntry("dap_so","72");
            assertThat(tools.jackson.databind.json.JsonMapper.builder().build().writeValueAsString(geometry)).doesNotContain("dap_so","72");
            var result=check.execute(nam,cl,geometry.ma(),new NopBuocRequest("B.TD.TRALOI",List.of(new DongNop(0,"144/2",null),new DongNop(1,"Em tính diện tích đáy rồi nhân chiều cao và chia ba.",null)),null,null));
            assertThat(result.ketQua()).isEqualTo("DAT");
            var unknown=check.execute(nam,cl,geometry.ma(),new NopBuocRequest("B.TD.TRALOI",List.of(new DongNop(0,"",null),new DongNop(1,"Em xin thầy cô xem phần trình bày.",null)),null,null));
            assertThat(unknown.ketQua()).isEqualTo("KHONG_KIEM_DUOC");
            submit.execute(nam,cl,geometry.ma());
            assertThat(store.reviews(cl)).anyMatch(i->"STEP".equals(i.details().get("kind"))&&i.title().contains(geometry.ma()));
            var xsPending=check.execute(nam,cl,"NT-XS12-VDC-07",new NopBuocRequest("B.XS.KETLUAN",List.of(new DongNop(0,"Em cần thầy cô xem cách lập xác suất.",null)),null,null));
            assertThat(xsPending.ketQua()).isEqualTo("KHONG_KIEM_DUOC");
            assertThat(store.reviews(cl)).filteredOn(i->i.title().contains("NT-XS12-VDC-07"))
                .anySatisfy(i->assertThat(i.subtitle()).contains("Xác suất có điều kiện").doesNotContain("pytest","đủ ba tầng","Bước B.XS"));
            // Cùng các bài còn phát hành: GV và HS phải nhìn cùng số câu của bộ hình học.
            var gv=store.assignments(cl).stream().filter(i->nam.toString().equals(i.details().get("studentId")))
                .map(i->i.details().get("problemCode")).filter(c->c.toString().startsWith("NT-KG12")).distinct().count();
            var hs=studentAssignments.execute(nam,cl).stream().filter(i->i.maBai().startsWith("NT-KG12")).count();
            assertThat(gv).isEqualTo(hs);
            // Một dòng nhắc cho mỗi lượt giao: không lặp theo từng bài trong bộ.
            var reminderKeys=store.reminders(nam,Instant.now()).stream().filter(i->i.status().equals("KIEM_TRA"))
                .map(i->i.title()+i.details().get("dueAt")).toList();
            assertThat(reminderKeys).doesNotHaveDuplicates();
            String doc=store.library(cl,true).documents().getFirst().id();
            service.archiveDocument(teacher,cl,UUID.fromString(doc));
            assertThat(store.library(cl,true).documents()).noneMatch(i->i.id().equals(doc));
            assertThat(store.library(cl,false).documents()).noneMatch(i->i.id().equals(doc));
            var assigned=store.assignments(cl).getFirst();
            var ids=store.assignments(cl).stream().filter(i->i.details().get("setKey").equals(assigned.details().get("setKey")))
                .map(i->UUID.fromString(i.id())).toList();
            service.archiveAssignments(teacher,new ArchiveSet(cl,ids));
            assertThat(store.assignments(cl)).noneMatch(i->ids.contains(UUID.fromString(i.id())));
            assertThat(store.dashboard(cl).assignments()).isEqualTo(store.assignments(cl).size());
            UUID session=UUID.randomUUID();
            db.sql("INSERT INTO auth_sessions(id,user_id,created_at) VALUES(:id,:user,now())").param("id",session).param("user",nam).update();
            db.sql("INSERT INTO refresh_tokens(id,session_id,user_id,token_hash,expires_at,created_at) VALUES(:id,:session,:user,:hash,now()+interval '1 day',now())")
                .param("id",UUID.randomUUID()).param("session",session).param("user",nam).param("hash","f".repeat(64)).update();
            String raw="Qa-reset-new-password-123";
            service.resetPassword(admin,nam,new ResetPassword(raw));
            String hash=db.sql("SELECT password_hash FROM users WHERE id=:id").param("id",nam).query(String.class).single();
            assertThat(hash).startsWith("{bcrypt}");assertThat(passwords.matches(raw,hash)).isTrue();
            assertThat(db.sql("SELECT count(*) FROM refresh_tokens WHERE user_id=:id AND revoked_at IS NULL").param("id",nam).query(Integer.class).single()).isZero();
            service.archiveUser(admin,nam);
            assertThat(store.users()).noneMatch(i->i.id().equals(nam.toString()));
            assertThat(store.accountRole(nam)).isEmpty();
            assertThat(db.sql("SELECT count(*) FROM auth_sessions WHERE user_id=:id AND revoked_at IS NULL").param("id",nam).query(Integer.class).single()).isZero();
            assertThat(store.audit()).anyMatch(i->i.title().equals("DAT_LAI_MAT_KHAU")&&i.details().get("targetName").equals("Nguyễn Hoài Nam"));
            assertThat(store.audit()).noneMatch(i->i.subtitle().contains(raw));
        } finally {db.sql("DROP SCHEMA "+SCHEMA+" CASCADE").update();}
    }
}
