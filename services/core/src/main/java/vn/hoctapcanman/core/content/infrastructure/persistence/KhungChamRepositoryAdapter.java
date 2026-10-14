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
        return jdbc.sql("SELECT frame_version, steps::text, problem_data::text FROM exercise_step_frames WHERE problem_id=:id AND content_version=:v")
            .param("id", id).param("v", version).query((rs, n) -> new KhungCham(rs.getInt(1),
                JSON.readValue(rs.getString(2), new TypeReference<List<MoTaBuoc>>() {}),
                JSON.readValue(rs.getString(3), new TypeReference<Map<String, Object>>() {}))).optional();
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
