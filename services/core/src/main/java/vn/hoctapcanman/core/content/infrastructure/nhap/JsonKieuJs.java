package vn.hoctapcanman.core.content.infrastructure.nhap;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * JSON viết đúng như {@code JSON.stringify} của JavaScript (không khoảng trắng, giữ thứ tự khóa, số nguyên không có
 * {@code .0}, cách thoát ký tự như ECMAScript), để dấu vân tay nội dung tính được như v0 ({@code seed.ts}: sha256 của
 * {@code JSON.stringify(...)}) và đối chiếu được với tệp vàng của v0 (T013, T014).
 */
final class JsonKieuJs {

    private JsonKieuJs() {}

    static String stringify(@Nullable Object giaTri) {
        StringBuilder sb = new StringBuilder();
        viet(sb, giaTri);
        return sb.toString();
    }

    private static void viet(StringBuilder sb, @Nullable Object v) {
        switch (v) {
            case null -> sb.append("null");
            case String s -> chuoi(sb, s);
            case Boolean b -> sb.append(b);
            case Number n -> so(sb, n);
            case Map<?, ?> m -> {
                sb.append('{');
                boolean dau = true;
                for (Map.Entry<?, ?> e : m.entrySet()) {
                    if (!dau) {
                        sb.append(',');
                    }
                    dau = false;
                    chuoi(sb, String.valueOf(e.getKey()));
                    sb.append(':');
                    viet(sb, e.getValue());
                }
                sb.append('}');
            }
            case List<?> l -> {
                sb.append('[');
                for (int i = 0; i < l.size(); i++) {
                    if (i > 0) {
                        sb.append(',');
                    }
                    viet(sb, l.get(i));
                }
                sb.append(']');
            }
            default -> throw new IllegalArgumentException("Kiểu JSON không hỗ trợ: " + v.getClass().getName());
        }
    }

    /** Số như {@code Number.prototype.toString}: nguyên thì không phần thập phân; mũ chỉ khi < 1e-6 hoặc >= 1e21. */
    private static void so(StringBuilder sb, Number n) {
        if (n instanceof Integer || n instanceof Long || n instanceof Short || n instanceof Byte) {
            sb.append(n.longValue());
            return;
        }
        double d = n.doubleValue();
        if (Double.isNaN(d) || Double.isInfinite(d)) {
            sb.append("null");
            return;
        }
        if (d == 0) {
            sb.append('0');
            return;
        }
        // Double.toString cho chữ số ngắn nhất khôi phục được giá trị (JDK 19+), cùng tập chữ số với ECMAScript.
        BigDecimal bd = new BigDecimal(Double.toString(d)).stripTrailingZeros();
        double abs = Math.abs(d);
        if (abs >= 1e-6 && abs < 1e21) {
            sb.append(bd.toPlainString());
            return;
        }
        String chuSo = bd.unscaledValue().abs().toString();
        int mu = chuSo.length() - 1 - bd.scale();
        if (d < 0) {
            sb.append('-');
        }
        sb.append(chuSo.charAt(0));
        if (chuSo.length() > 1) {
            sb.append('.').append(chuSo, 1, chuSo.length());
        }
        sb.append('e').append(mu >= 0 ? "+" : "-").append(Math.abs(mu));
    }

    /** Thoát như ECMAScript: {@code " \ \b \f \n \r \t}, ký tự điều khiển và nửa cặp thay thế lẻ thành \\uXXXX chữ thường. */
    private static void chuoi(StringBuilder sb, String s) {
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\b' -> sb.append("\\b");
                case '\f' -> sb.append("\\f");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    boolean leCao = Character.isHighSurrogate(c) && (i + 1 >= s.length() || !Character.isLowSurrogate(s.charAt(i + 1)));
                    boolean leThap = Character.isLowSurrogate(c) && (i == 0 || !Character.isHighSurrogate(s.charAt(i - 1)));
                    if (c < 0x20 || leCao || leThap) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        sb.append('"');
    }
}
