package vn.hoctapcanman.core.workspace.infrastructure.seed;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import vn.hoctapcanman.core.shared.infrastructure.seed.LopSeedAnToan;
import vn.hoctapcanman.core.shared.infrastructure.seed.TruongNguyenTrai;
import vn.hoctapcanman.core.workspace.application.dto.WorkspaceDto.Item;
import vn.hoctapcanman.core.workspace.infrastructure.persistence.WorkspaceStoreAdapter;

/** Cách đọc dữ liệu seed dev: kỹ năng đúng khối, tên bộ giao; lớp khác dùng cách đọc hiện hành. */
@Repository
@Primary
@Profile("dev")
public class WorkspaceNguyenTraiSeedAdapter extends WorkspaceStoreAdapter {
    private final JdbcClient db;
    public WorkspaceNguyenTraiSeedAdapter(JdbcClient db) {
        super(db);
        this.db = db;
    }

    private boolean seeded(UUID cl) {
        return TruongNguyenTrai.LOP.stream()
            .anyMatch(name -> TruongNguyenTrai.id("lop:"+name).equals(cl) && LopSeedAnToan.kiem(db,name));
    }

    @Override public List<Item> students(UUID cl) {
        var rows = super.students(cl);
        if (!seeded(cl)) return rows;
        var skills = db.sql("SELECT code FROM skills WHERE grade=(SELECT grade FROM classes WHERE id=:cl)")
            .param("cl",cl).query(String.class).list();
        return rows.stream().map(row -> {
            Map<String,Object> details = new HashMap<>(row.details());
            if (details.get("skills") instanceof List<?> all) {
                details.put("skills", all.stream().filter(value -> value instanceof Map<?,?> skill
                    && skills.contains(String.valueOf(skill.get("code")))).toList());
            }
            return new Item(row.id(),row.title(),row.subtitle(),row.status(),details);
        }).toList();
    }

    @Override public List<Item> assignments(UUID cl) {
        var rows = super.assignments(cl);
        if (!seeded(cl)) return rows;
        Map<String,String> titles = new HashMap<>();
        db.sql("SELECT id,set_name FROM assignments WHERE class_id=:cl AND set_name IS NOT NULL")
            .param("cl",cl).query((r,n) -> Map.entry(r.getString("id"),r.getString("set_name"))).list()
            .forEach(e -> titles.put(e.getKey(),e.getValue()));
        return rows.stream().map(row -> new Item(row.id(),titles.getOrDefault(row.id(),row.title()),
            row.subtitle(),row.status(),row.details())).toList();
    }
}
