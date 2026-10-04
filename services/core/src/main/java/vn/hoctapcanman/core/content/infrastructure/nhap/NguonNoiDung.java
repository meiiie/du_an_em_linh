package vn.hoctapcanman.core.content.infrastructure.nhap;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * Đọc tệp nội dung dưới {@code app.content.source}: JSON thành {@code Map} / {@code List} giữ thứ tự khóa (để dấu vân tay
 * tính như v0), CSV theo đúng cách {@code parseCsv} của {@code seed.ts}. Đường dẫn tương đối không được ra ngoài thư mục
 * nguồn.
 */
@Component
@EnableConfigurationProperties(NoiDungProperties.class)
public class NguonNoiDung {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    private final Path goc;

    public NguonNoiDung(NoiDungProperties properties) {
        this.goc = properties.source().toAbsolutePath().normalize();
    }

    /** Một đối tượng JSON. */
    @SuppressWarnings("unchecked")
    public Map<String, @Nullable Object> doiTuong(String duong) {
        return (Map<String, @Nullable Object>) JSON.readValue(chu(duong), Map.class);
    }

    /** Một mảng đối tượng JSON. */
    @SuppressWarnings("unchecked")
    public List<Map<String, @Nullable Object>> danhSach(String duong) {
        return (List<Map<String, @Nullable Object>>) JSON.readValue(chu(duong), List.class);
    }

    /** Các dòng của CSV (dòng toàn ô trống bị bỏ), như {@code parseCsv} của v0: ngoặc kép, {@code ""} trong ngoặc, bỏ CR. */
    public List<List<String>> csv(String duong) {
        String text = chu(duong);
        List<List<String>> dong = new ArrayList<>();
        List<String> hang = new ArrayList<>();
        StringBuilder o = new StringBuilder();
        boolean trongNgoac = false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (trongNgoac) {
                if (c == '"') {
                    if (i + 1 < text.length() && text.charAt(i + 1) == '"') {
                        o.append('"');
                        i++;
                    } else {
                        trongNgoac = false;
                    }
                } else {
                    o.append(c);
                }
            } else if (c == '"') {
                trongNgoac = true;
            } else if (c == ',') {
                hang.add(o.toString());
                o.setLength(0);
            } else if (c == '\n') {
                hang.add(o.toString());
                dong.add(hang);
                hang = new ArrayList<>();
                o.setLength(0);
            } else if (c != '\r') {
                o.append(c);
            }
        }
        if (!o.isEmpty() || !hang.isEmpty()) {
            hang.add(o.toString());
            dong.add(hang);
        }
        return dong.stream().filter(h -> h.stream().anyMatch(x -> !x.isBlank())).toList();
    }

    private String chu(String duong) {
        Path tep = goc.resolve(duong).normalize();
        if (!tep.startsWith(goc)) {
            throw new IllegalArgumentException("Đường dẫn ra ngoài thư mục nội dung: " + duong);
        }
        try {
            return Files.readString(tep, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Không đọc được " + tep, e);
        }
    }
}
