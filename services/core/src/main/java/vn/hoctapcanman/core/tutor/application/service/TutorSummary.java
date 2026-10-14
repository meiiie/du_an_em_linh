package vn.hoctapcanman.core.tutor.application.service;

import java.util.*;
import tools.jackson.databind.json.JsonMapper;

/** Kiểm schema, nguồn lượt và PII của tóm tắt; không qua cổng đáp án. */
public final class TutorSummary {
    private static final JsonMapper JSON=JsonMapper.builder().build();
    public static final List<String> FIELDS=List.of("mucTieuBuoc","cachDaThu","loiDaDuocCham","cauHoiConMo","goiYDaDung","dieuDaSua");
    private TutorSummary() {}
    public static String validate(String raw,List<Long> sources) {
        // Model hay bọc JSON trong ```json; chỉ lấy khối {...}. Nguồn lượt do máy chủ tự gắn, không tin model.
        int open=raw.indexOf('{'),close=raw.lastIndexOf('}');
        if(open<0 || close<=open)throw new IllegalStateException("SUMMARY_SCHEMA");
        var node=JSON.readTree(raw.substring(open,close+1));
        Set<String> keys=new HashSet<>();node.propertyNames().forEach(keys::add);
        keys.remove("luotNguon");
        if(!node.isObject() || !keys.equals(new HashSet<>(FIELDS)))throw new IllegalStateException("SUMMARY_SCHEMA");
        Map<String,Object> clean=new LinkedHashMap<>();
        for(String key:FIELDS) {
            if(!node.path(key).isString())throw new IllegalStateException("SUMMARY_SCHEMA");
            String text=TutorPrompt.withoutIdentifiers(node.path(key).asString());
            if(!TutorPrompt.safeToSend(text,List.of()))throw new IllegalStateException("SUMMARY_IDENTITY");
            clean.put(key,text);
        }
        if(clean.get("mucTieuBuoc").toString().isBlank())throw new IllegalStateException("SUMMARY_EMPTY");
        clean.put("luotNguon",sources);
        return JSON.writeValueAsString(clean);
    }
}
