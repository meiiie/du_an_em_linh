package vn.hoctapcanman.core.tutor.application.service;

import java.util.*;
import tools.jackson.databind.json.JsonMapper;

/** Kiểm schema, nguồn lượt và PII của tóm tắt; không qua cổng đáp án. */
public final class TutorSummary {
    private static final JsonMapper JSON=JsonMapper.builder().build();
    public static final List<String> FIELDS=List.of("mucTieuBuoc","cachDaThu","loiDaDuocCham","cauHoiConMo","goiYDaDung","dieuDaSua");
    private TutorSummary() {}
    public static String validate(String raw,List<Long> sources) {
        var node=JSON.readTree(raw);
        Set<String> keys=new HashSet<>();node.propertyNames().forEach(keys::add);
        Set<String> required=new HashSet<>(FIELDS);required.add("luotNguon");
        if(!node.isObject() || !keys.equals(required))throw new IllegalStateException("SUMMARY_SCHEMA");
        var ids=node.path("luotNguon");
        if(!ids.isArray() || ids.size()!=sources.size())throw new IllegalStateException("SUMMARY_SOURCES");
        for(int i=0;i<sources.size();i++)if(!ids.path(i).isIntegralNumber() || ids.path(i).asLong()!=sources.get(i))throw new IllegalStateException("SUMMARY_SOURCES");
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
