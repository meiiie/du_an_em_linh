package vn.hoctapcanman.core.content.infrastructure.nhap;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Kết quả phải trùng từng ký tự với {@code JSON.stringify} của Node (giá trị kỳ vọng chạy bằng Node 24). */
class JsonKieuJsTest {

    @Test
    void giuThuTuKhoaSoNguyenKhongCoPhanThapPhan() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("z", 1);
        m.put("a", Arrays.asList(1.5, 2.0, -0.25, null, true));
        m.put("m", Map.of("k", 3L));
        assertThat(JsonKieuJs.stringify(m)).isEqualTo("{\"z\":1,\"a\":[1.5,2,-0.25,null,true],\"m\":{\"k\":3}}");
    }

    @Test
    void soLonNhoVietNhuNumberToString() {
        // node -e "console.log(JSON.stringify([1e-7, 1e21, 123456789012345680000, 0.000001, 1.5e-10, -2e22, 0.1+0.2]))"
        assertThat(JsonKieuJs.stringify(List.of(1e-7, 1e21, 123456789012345680000.0, 0.000001, 1.5e-10, -2e22, 0.1 + 0.2)))
            .isEqualTo("[1e-7,1e+21,123456789012345680000,0.000001,1.5e-10,-2e+22,0.30000000000000004]");
    }

    @Test
    void thoatKyTuNhuEcmaScript() {
        // node -e 'console.log(JSON.stringify("a\"b\\c\n\t\u0001\u001f/Đạo hàm 𝑥 \ud800"))'
        String s = "a\"b\\c\n\t\u0001\u001f/Đạo hàm 𝑥 \ud800";
        assertThat(JsonKieuJs.stringify(s)).isEqualTo("\"a\\\"b\\\\c\\n\\t\\u0001\\u001f/Đạo hàm 𝑥 \\ud800\"");
    }
}
