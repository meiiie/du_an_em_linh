package vn.hoctapcanman.core.content.application.dto;

import java.util.List;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.TreeMap;
import java.util.Map;
import vn.hoctapcanman.core.content.application.dto.hocsinh.MoTaBuoc;

/** Nội dung máy chủ theo bài và phiên bản; tuyệt đối không trả DTO này qua API học sinh/gia sư. */
public record KhungCham(int phienBanKhung, List<MoTaBuoc> cacBuoc, Map<String, Object> baiToan) {
    public KhungCham {
        if (phienBanKhung < 1) throw new IllegalArgumentException("Phiên bản khung phải dương");
        cacBuoc = List.copyOf(cacBuoc);
        baiToan = dongBang(baiToan);
    }
    /** Thứ tự khóa ổn định qua lần nạp/restart để băm chấm không đổi; không cho nơi gọi sửa dữ kiện lồng nhau. */
    private static Map<String, Object> dongBang(Map<String, Object> map) {
        Map<String, Object> copy = new LinkedHashMap<>();
        new TreeMap<>(map).forEach((key, value) -> copy.put(key, dongBang(value)));
        return Collections.unmodifiableMap(copy);
    }

    @SuppressWarnings("unchecked")
    private static Object dongBang(Object value) {
        if (value instanceof Map<?, ?> map) return dongBang((Map<String, Object>) map);
        if (value instanceof List<?> list) return list.stream().map(KhungCham::dongBang).toList();
        return value;
    }

    @Override public String toString() { return "KhungCham[phienBanKhung=" + phienBanKhung + "]"; }
}
