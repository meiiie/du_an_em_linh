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
import vn.hoctapcanman.core.workspace.application.dto.WorkspaceDto.Item;
import vn.hoctapcanman.core.workspace.infrastructure.persistence.WorkspaceStoreAdapter;

/** Cách đọc dữ liệu seed dev: kỹ năng đúng khối, tên bộ giao; lớp khác dùng cách đọc hiện hành. */
@Repository
@Primary
@Profile("dev")
public class WorkspaceNguyenTraiSeedAdapter extends WorkspaceStoreAdapter {
    public WorkspaceNguyenTraiSeedAdapter(JdbcClient db,Clock clock) {
        super(db,clock);
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
