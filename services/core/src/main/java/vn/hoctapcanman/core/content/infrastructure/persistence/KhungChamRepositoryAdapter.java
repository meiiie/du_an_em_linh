package vn.hoctapcanman.core.content.infrastructure.persistence;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;
import vn.hoctapcanman.core.content.application.dto.KhungCham;
import vn.hoctapcanman.core.content.application.dto.hocsinh.MoTaBuoc;
import vn.hoctapcanman.core.content.application.port.KhungChamTheoBai;

@Repository
public class KhungChamRepositoryAdapter implements KhungChamTheoBai {
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private final JdbcClient jdbc;
    public KhungChamRepositoryAdapter(JdbcClient jdbc) { this.jdbc = jdbc; }

    @Override public Optional<KhungCham> doc(UUID id, int version) {
        var explicit = jdbc.sql("SELECT frame_version, steps::text, problem_data::text FROM exercise_step_frames WHERE problem_id=:id AND content_version=:v")
            .param("id", id).param("v", version).query((rs, n) -> new KhungCham(rs.getInt(1),
                JSON.readValue(rs.getString(2), new TypeReference<List<MoTaBuoc>>() {}),
                JSON.readValue(rs.getString(3), new TypeReference<Map<String, Object>>() {}))).optional();
        if (explicit.isPresent()) return explicit;
        return jdbc.sql("""
            SELECT s.final_answer FROM problems p LEFT JOIN solutions s ON s.problem_id=p.id
            WHERE p.id=:id AND p.content_version=:v
                AND (p.answer_form<>'TU_LUAN_5_BUOC' OR p.function_sympy IS NULL)
            """).param("id",id).param("v",version).query((rs,n) -> {
                Map<String,Object> data = new java.util.LinkedHashMap<>();
                data.put("loai","tra_loi_tu_do");
                data.put("dap_so", answerText(rs.getString(1)));
                return new KhungCham(1,List.of(new MoTaBuoc("B.TD.TRALOI","Bài làm",
                    "Nhập đáp số cuối và trình bày cách giải. Thầy cô sẽ xem phần trình bày.","TU_DO")),data);
            }).optional();
    }

    static String answerText(String raw) {
        if(raw==null||raw.equals("null"))return "";
        if(raw.startsWith("\"")) {
            try { return JSON.readValue(raw,String.class); }
            catch(RuntimeException e) { return ""; }
        }
        return raw;
    }

    @Override public void ghi(UUID id, int version, KhungCham khung) {
        jdbc.sql("""
            INSERT INTO exercise_step_frames (problem_id, content_version, frame_version, steps, problem_data)
            VALUES (:id, :v, :fv, CAST(:steps AS jsonb), CAST(:data AS jsonb))
            ON CONFLICT (problem_id, content_version) DO NOTHING
            """).param("id", id).param("v", version).param("fv", khung.phienBanKhung())
            .param("steps", JSON.writeValueAsString(khung.cacBuoc())).param("data", JSON.writeValueAsString(khung.baiToan())).update();
        if (!doc(id, version).orElseThrow().equals(khung)) {
            throw new IllegalStateException("Khung chấm đã đổi; cần phiên bản nội dung mới");
        }
    }
}
