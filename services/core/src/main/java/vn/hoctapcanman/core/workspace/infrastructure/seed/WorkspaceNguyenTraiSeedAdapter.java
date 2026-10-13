package vn.hoctapcanman.core.workspace.infrastructure.seed;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.time.Clock;
import java.nio.charset.StandardCharsets;
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
import vn.hoctapcanman.core.workspace.application.dto.WorkspaceDto.Dashboard;
import vn.hoctapcanman.core.workspace.infrastructure.persistence.WorkspaceStoreAdapter;

/** Cách đọc dữ liệu seed dev: kỹ năng đúng khối, tên bộ giao; lớp khác dùng cách đọc hiện hành. */
@Repository
@Primary
@Profile("dev")
public class WorkspaceNguyenTraiSeedAdapter extends WorkspaceStoreAdapter {
    private final JdbcClient db;
    public WorkspaceNguyenTraiSeedAdapter(JdbcClient db,Clock clock) {
        super(db,clock);
        this.db = db;
    }

    private boolean seeded(UUID cl) {
        return TruongNguyenTrai.LOP.stream()
            .anyMatch(name -> TruongNguyenTrai.id("lop:"+name).equals(cl) && LopSeedAnToan.kiem(db,name));
    }

    @Override public List<Item> assignments(UUID cl) {
        var rows = super.assignments(cl);
        if (!seeded(cl)) return rows;
        Map<String,Map<String,Object>> metadata = new HashMap<>();
        db.sql("""
            SELECT a.id,a.set_name,EXISTS(SELECT 1 FROM submissions s
                JOIN problems p ON p.id=s.problem_id
                WHERE s.class_id=a.class_id AND s.student_id=a.student_id AND s.problem_id=a.problem_id
                AND s.content_version=p.content_version AND s.status='DA_NOP') submitted
            FROM assignments a WHERE a.class_id=:cl AND a.set_name IS NOT NULL
            """).param("cl",cl).query((r,n) -> Map.entry(r.getString("id"),
                Map.<String,Object>of("tenBo",r.getString("set_name"),"classId",cl.toString(),"submitted",r.getBoolean("submitted"))))
            .list().forEach(e -> metadata.put(e.getKey(),e.getValue()));
        return rows.stream().map(row -> {
            Map<String,Object> details = new HashMap<>(row.details());
            details.putAll(metadata.getOrDefault(row.id(),Map.of()));
            String title = String.valueOf(details.getOrDefault("tenBo",row.title()));
            details.put("setKey",setKey(new Item(row.id(),title,row.subtitle(),row.status(),details)));
            return new Item(row.id(),title,row.subtitle(),row.status(),details);
        }).toList();
    }

    @Override public Dashboard dashboard(UUID cl) {
        var dashboard = super.dashboard(cl);
        if (!seeded(cl)) return dashboard;
        var deadlines = boBai(upcomingDeadlines(assignments(cl)));
        return new Dashboard(dashboard.students(),dashboard.stuck(),dashboard.pending(),dashboard.assignments(),
            dashboard.distribution(),deadlines);
    }

    /** Một bộ = lớp + tên + hạn + mode; không trộn hai lần giao có hạn khác nhau. */
    private static String setKey(Item row) {
        var d = row.details();
        String name = String.valueOf(d.getOrDefault("tenBo",d.get("problemCode")));
        String key = String.join("\u001f",String.valueOf(d.get("classId")),name,
            String.valueOf(d.get("dueAt")),row.status());
        return UUID.nameUUIDFromBytes(key.getBytes(StandardCharsets.UTF_8)).toString();
    }

    static List<Item> boBai(List<Item> rows) {
        Map<String,List<Item>> groups = rows.stream().collect(java.util.stream.Collectors.groupingBy(
            WorkspaceNguyenTraiSeedAdapter::setKey,LinkedHashMap::new,java.util.stream.Collectors.toList()));
        return groups.entrySet().stream().map(group -> {
            var assignments = group.getValue();
            var first = assignments.getFirst();
            var codes = assignments.stream().map(row -> String.valueOf(row.details().get("problemCode"))).distinct().sorted().toList();
            Map<String,Object> details = new HashMap<>();
            details.put("tenBo",first.title());
            details.put("setKey",group.getKey());
            details.put("dueAt",first.details().get("dueAt"));
            details.put("mode",first.status());
            details.put("problemCodes",codes);
            details.put("problemCount",codes.size());
            details.put("studentCount",(int)assignments.stream().map(row -> row.details().get("studentId")).distinct().count());
            details.put("assignmentCount",assignments.size());
            details.put("submittedCount",(int)assignments.stream().filter(row -> Boolean.TRUE.equals(row.details().get("submitted"))).count());
            return new Item(group.getKey(),first.title(),codes.size()+" bài",first.status(),details);
        }).toList();
    }
}
