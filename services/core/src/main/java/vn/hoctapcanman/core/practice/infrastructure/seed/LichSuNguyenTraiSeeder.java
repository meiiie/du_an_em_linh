package vn.hoctapcanman.core.practice.infrastructure.seed;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.json.JsonMapper;
import vn.hoctapcanman.core.shared.infrastructure.math.MathJob;
import vn.hoctapcanman.core.shared.infrastructure.math.MathResult;
import vn.hoctapcanman.core.shared.infrastructure.math.MathServiceClient;
import vn.hoctapcanman.core.shared.infrastructure.seed.LopSeedAnToan;
import vn.hoctapcanman.core.shared.infrastructure.seed.TruongNguyenTrai;

/** Lịch sử hư cấu 3 tuần: từng bài có payload học sinh, phán quyết CAS hoặc giáo viên hư cấu, không sửa bài làm đã có. */
@Component
@Profile("dev")
@Order(5)
public class LichSuNguyenTraiSeeder implements ApplicationRunner {
    private static final Logger LOG = LoggerFactory.getLogger(LichSuNguyenTraiSeeder.class);
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private final JdbcClient db;
    private final MathServiceClient math;
    private final TransactionTemplate tx;
    private final Clock clock;
    private final Path source;
    public LichSuNguyenTraiSeeder(JdbcClient db, MathServiceClient math, PlatformTransactionManager manager,
            Clock clock, @Value("${app.content.source}") String source) {
        this.db = db; this.math = math; this.tx = new TransactionTemplate(manager); this.clock = clock;
        this.source = Path.of(source);
    }
    @Override
    public void run(ApplicationArguments args) {
        List<String> classes = TruongNguyenTrai.LOP.stream().filter(ten -> LopSeedAnToan.kiem(db, ten)).toList();
        if (classes.isEmpty()) return;
        List<Map<String, Object>> fixtures;
        try {
            fixtures = fixtures();
        } catch (UncheckedIOException e) {
            LOG.warn("Bỏ qua lịch sử trường hư cấu: thiếu hoặc không đọc được data/dev.");
            return;
        }
        Map<String, Map<String, @Nullable Object>> grades = new HashMap<>();
        int count = 0;
        for (String ten : classes) {
            UUID cl = TruongNguyenTrai.id("lop:" + ten);
            List<UUID> students = db.sql("""
                SELECT e.user_id FROM enrollments e JOIN users u ON u.id=e.user_id
                WHERE e.class_id=:cl AND e.role_in_class='STUDENT' AND u.synthetic AND u.enabled AND u.role='STUDENT'
                ORDER BY u.email
                """).param("cl", cl).query(UUID.class).list();
            Instant anchor = db.sql("SELECT created_at FROM classes WHERE id=:cl").param("cl", cl)
                .query((r,n) -> r.getTimestamp(1).toInstant()).single();
            for (int i=0; i<students.size(); i++) {
                UUID student = students.get(i);
                schedule(cl, student, anchor);
                List<String> codes = db.sql("""
                    SELECT p.code FROM assignments a JOIN problems p ON p.id=a.problem_id
                    WHERE a.class_id=:cl AND a.student_id=:hs AND a.mode='ON_TAP' AND p.code NOT LIKE 'NT-LUYEN%'
                    ORDER BY p.skill_code, CASE p.level4 WHEN 'NHAN_BIET' THEN 0 WHEN 'THONG_HIEU' THEN 1
                        WHEN 'VAN_DUNG' THEN 2 ELSE 3 END,p.code
                    """).param("cl",cl).param("hs",student).query(String.class).list();
                UUID teacher = db.sql("""
                    SELECT e.user_id FROM enrollments e JOIN users u ON u.id=e.user_id
                    WHERE e.class_id=:cl AND e.role_in_class='TEACHER' AND u.synthetic AND u.enabled
                    AND u.role='TEACHER' ORDER BY e.enrolled_at,e.user_id LIMIT 1
                    """).param("cl",cl).query(UUID.class).single();
                for (Map<String, Object> fixture : fixtures) {
                    String code = (String) fixture.get("ma");
                    int task = codes.indexOf(code);
                    if (task < 0) continue;
                    String skill = code.split("-")[1];
                    List<String> chapter = codes.stream().filter(c -> c.split("-")[1].equals(skill)).toList();
                    int chapterIndex = codes.stream().map(c -> c.split("-")[1]).distinct().toList().indexOf(skill);
                    String kind = outcome(i, chapterIndex, chapter.indexOf(code), chapter.size());
                    if (!kind.equals(fixture.get("loai"))) continue;
                    UUID id = TruongNguyenTrai.id("bai-lam:" + student + ":" + code);
                    if (db.sql("SELECT EXISTS(SELECT 1 FROM submissions WHERE id=:id)").param("id", id).query(Boolean.class).single()) continue;
                    List<ProblemRow> available = db.sql("""
                        SELECT p.id,p.content_version,p.skill_code,p.level4 FROM problems p
                        JOIN assignments a ON a.problem_id=p.id AND a.class_id=:cl AND a.student_id=:hs AND a.status='DA_GIAO' AND a.mode='ON_TAP'
                        JOIN problem_releases r ON r.problem_id=p.id AND r.class_id=:cl AND r.status='DA_PHAT_HANH'
                        WHERE p.code=:code AND (a.due_at IS NULL OR a.due_at>:now)
                        """).param("cl", cl).param("hs", student).param("code", code).param("now", Timestamp.from(clock.instant()))
                        .query((r,n) -> new ProblemRow(r.getObject(1,UUID.class),r.getInt(2),r.getString(3),r.getString(4))).list();
                    if (available.isEmpty()) continue;
                    String key = code + ":" + kind + (Boolean.TRUE.equals(fixture.get("giao_vien_cham")) ? ":"+teacher : "");
                    Map<String, @Nullable Object> grade = grades.get(key);
                    if (grade == null) {
                        if (Boolean.TRUE.equals(fixture.get("giao_vien_cham"))) {
                            grade = new HashMap<>();
                            grade.put("ket_qua", fixture.get("ket_qua"));
                            grade.put("loai_ket_qua", "GV_DUYET_SEED");
                            grade.put("per_buoc", Map.of());
                            grade.put("thong_bao", "Bài làm tổng hợp được giáo viên chấm theo lời giải kiểm bởi pytest.");
                            grade.put("chuan_hoa", Map.of("nguon", "GIAO_VIEN_SEED", "reviewer_id", teacher.toString(),
                                "dap_an_hoc_sinh", fixture.get("dap_an_hoc_sinh")));
                        } else {
                            MathResult result = math.call(MathJob.GRADE, map(fixture.get("payload")));
                            if (!(result instanceof MathResult.Ok ok) || !Objects.equals(ok.body().get("ket_qua"), fixture.get("ket_qua"))) {
                                LOG.warn("Bỏ qua lịch sử hư cấu: CAS không xác nhận payload học sinh.");
                                continue;
                            }
                            grade = ok.body();
                        }
                        grades.put(key, grade);
                    }
                    Map<String, @Nullable Object> verdict = grade;
                    Instant at = anchor.minus(Duration.ofDays(20)).plus(Duration.ofHours(task*12L)).plus(Duration.ofMinutes(i*7));
                    tx.executeWithoutResult(t -> save(id, cl, student, available.getFirst(), fixture, verdict, at));
                    count++;
                }
            }
            reviewQueue(cl, anchor);
        }
        LOG.info("Lịch sử trường hư cấu: {} bài nộp mới có căn cứ chấm", count);
    }

    /** Tỷ lệ khác nhau theo học sinh; chờ duyệt không tính sai, không đưa vào BKT. */
    static String outcome(int student, int chapter, int task, int size) {
        double[] rates = {.75, .95, .90, .85, .80, .75, .65, .55, .40, .20};
        boolean review = (student + chapter) % 3 != 2;
        if (review && task == 0) return "CAN_DUYET";
        int graded = size - (review ? 1 : 0);
        int correct = (int) Math.round(graded * rates[student]);
        int index = task - (review ? 1 : 0);
        // Hai học sinh cần hỗ trợ còn sai ở bài khó cuối; các em khác tiến bộ dần.
        boolean passed = student >= 8 ? index < correct : index >= graded - correct;
        return passed ? "DUNG" : "SAI";
    }

    private void save(UUID id, UUID cl, UUID student, ProblemRow p, Map<String,Object> fixture,
            Map<String,@Nullable Object> grade, Instant at) {
        UUID grading = TruongNguyenTrai.id("cham:" + id);
        String result = (String) Objects.requireNonNull(grade.get("ket_qua"));
        db.sql("""
            INSERT INTO submissions(id,class_id,student_id,problem_id,content_version,status,started_at)
            VALUES(:id,:cl,:hs,:p,:v,'DANG_LAM',:at)
            """).param("id", id).param("cl", cl).param("hs", student).param("p", p.id()).param("v", p.version())
            .param("at", Timestamp.from(at)).update();
        Map<String,Object> payload = map(fixture.get("payload"));
        for (Map<String,Object> step : list(payload.get("cac_buoc"))) {
            String code = (String) step.get("ma_buoc");
            for (Map<String,Object> line : list(step.get("cac_dong"))) {
                db.sql("INSERT INTO submission_steps(submission_id,step_code,line_no,latex,line_kind) VALUES(:id,:step,:n,:text,:kind)")
                    .param("id",id).param("step",code).param("n",line.get("dong")).param("text",line.get("latex")).param("kind",line.get("loai")).update();
            }
            if (step.get("bang") != null) {
                Map<String,Object> table = map(step.get("bang"));
                db.sql("INSERT INTO submission_tables(submission_id,step_code,table_kind) VALUES(:id,:step,'XET_DAU')")
                    .param("id",id).param("step",code).update();
                int ordinal=0;
                for (Map<String,Object> cell : list(table.get("cac_o"))) {
                    db.sql("""
                        INSERT INTO submission_table_cells(submission_id,step_code,ordinal,row_code,k,value)
                        VALUES(:id,:step,:ord,:row,:k,:value)
                        """).param("id",id).param("step",code).param("ord",ordinal++).param("row",cell.get("hang"))
                        .param("k",cell.get("k")).param("value",cell.get("gia_tri")).update();
                }
            }
        }
        db.sql("""
            INSERT INTO grading_results(id,submission_id,step_code,request_hash,result,result_type,wrong_steps,error_code,
                confidence,per_step,message,issues,math_ok,unfinished,normalizer_version,normalization,graded_at)
            VALUES(:gid,:id,'B.DH.KETLUAN',:hash,:result,:type,CAST(:wrong AS jsonb),:error,:confidence,
                CAST(:per AS jsonb),:message,CAST(:issues AS jsonb),:math,:unfinished,:norm,CAST(:normalization AS jsonb),:at)
            """).param("gid",grading).param("id",id).param("hash",sha(JSON.writeValueAsString(payload)))
            .param("result",result).param("type",grade.get("loai_ket_qua")).param("wrong",json(grade.get("buoc_sai")))
            .param("error",grade.get("ma_loi")).param("confidence",grade.get("do_tin_cay"))
            .param("per",JSON.writeValueAsString(grade.getOrDefault("per_buoc",Map.of())))
            .param("message",grade.get("thong_bao")).param("issues",json(grade.get("cac_van_de"))).param("math",grade.get("toan_dung"))
            .param("unfinished",Boolean.TRUE.equals(grade.get("chua_xong"))).param("norm",grade.get("phien_ban_chuan_hoa"))
            .param("normalization",json(grade.get("chuan_hoa"))).param("at",Timestamp.from(at.plusSeconds(1200))).update();
        db.sql("""
            UPDATE submissions SET status='DA_NOP',result=:r,result_grading_id=:g,skill_code=:skill,level4=:level,
                submitted_at=:at WHERE id=:id
            """).param("r",result).param("g",grading).param("skill",p.skill()).param("level",p.level())
            .param("at",Timestamp.from(at.plusSeconds(1200))).param("id",id).update();
        if (result.equals("KHONG_KIEM_DUOC")) {
            db.sql("""
                INSERT INTO escalations(id,class_id,student_id,skill_code,problem_code,step_code,kind,reason,created_at)
                VALUES(:id,:cl,:hs,:skill,:code,:step,'NHO_GV',
                    'Bài đã nộp: cần thầy cô xem cách trình bày và lập luận.',:at)
                ON CONFLICT DO NOTHING
                """).param("id",TruongNguyenTrai.id("duyet-bai:"+id)).param("cl",cl).param("hs",student).param("skill",p.skill())
                .param("code",fixture.get("ma")).param("step",p.skill().equals("T12.NT.HS") ? "B.DH.DAOHAM" : null)
                .param("at",Timestamp.from(at.plusSeconds(1200))).update();
        }
    }

    private void schedule(UUID cl, UUID student, Instant anchor) {
        for (int session=0;session<4;session++) {
            UUID id=TruongNguyenTrai.id("lich:"+student+":"+session);
            Instant start=anchor.plus(Duration.ofHours(session==0 ? 2 : 24L*session+2));
            db.sql("""
                INSERT INTO study_sessions(id,student_id,title,starts_at,ends_at)
                VALUES(:id,:hs,:title,:start,:end) ON CONFLICT DO NOTHING
                """).param("id",id).param("hs",student).param("title",session%2==0 ? "Luyện truy hồi — đơn điệu và cực trị" : "Ôn xen kẽ — công thức các chương")
                .param("start",Timestamp.from(start)).param("end",Timestamp.from(start.plusSeconds(2700))).update();
        }
    }

    private void reviewQueue(UUID cl, Instant anchor) {
        db.sql("""
            INSERT INTO tutor_review_queue(id,class_id,problem_id,draft,reason,created_at)
            SELECT :id,:cl,p.id,'Em hãy viết rõ điều kiện xác định trước khi dùng quy tắc đạo hàm.',
                'Một bài nộp chưa đọc được ký hiệu; giáo viên cần kiểm tra cách diễn đạt gợi ý.',:at
            FROM problems p JOIN problem_releases r ON r.problem_id=p.id AND r.class_id=:cl
            WHERE p.code='NT-DH12-VDC-08' AND r.status='DA_PHAT_HANH' ON CONFLICT DO NOTHING
            """).param("id",TruongNguyenTrai.id("goi-y-duyet:"+cl)).param("cl",cl).param("at",Timestamp.from(anchor.minus(Duration.ofDays(1)))).update();
    }

    @SuppressWarnings("unchecked") private static Map<String,Object> map(Object value) { return (Map<String,Object>) value; }
    @SuppressWarnings("unchecked") private static List<Map<String,Object>> list(@Nullable Object value) { return value==null ? List.of() : (List<Map<String,Object>>) value; }
    private static @Nullable String json(@Nullable Object value) { return value==null ? null : JSON.writeValueAsString(value); }
    private static String sha(String value) {
        try { return java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8))); }
        catch (java.security.NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
    @SuppressWarnings("unchecked") private List<Map<String,Object>> fixtures() {
        try { return (List<Map<String,Object>>) (List<?>) JSON.readValue(Files.readString(source.resolve("dev/nguyen-trai/bai-lam-lich-su.json")),List.class); }
        catch (IOException e) { throw new UncheckedIOException("Không đọc được bài làm lịch sử hư cấu",e); }
    }
    private record ProblemRow(UUID id,int version,String skill,String level) {}
}
