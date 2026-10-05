package vn.hoctapcanman.core.content.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import vn.hoctapcanman.core.content.domain.model.Solution;

/**
 * Lời giải cho học sinh viết như v0: so với tệp vàng mà chính {@code loiGiaiHocSinh} của v0 sinh
 * ({@code specs/001-lat-cat-doc/doi-chieu/loi-giai-v0.ts} → {@code content/loi-giai-v0.json}).
 */
class VietLoiGiaiTest {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    @Test
    void trungTungChuVoiV0TrenMoiCaCuaTepVang() throws IOException {
        JsonNode tep;
        try (InputStream doc = Objects.requireNonNull(getClass().getResourceAsStream("/content/loi-giai-v0.json"))) {
            tep = JSON.readTree(doc);
        }
        assertThat(tep.path("ca")).hasSize(11);
        for (JsonNode c : tep.path("ca")) {
            String dapAnCuoi = c.path("finalAnswer").isNull() ? null : c.path("finalAnswer").stringValue();
            Optional<String> v0 = c.path("loiGiai").isNull() ? Optional.empty() : Optional.of(c.path("loiGiai").stringValue());
            assertThat(VietLoiGiai.viet(JSON.writeValueAsString(c.get("baiLam")), dapAnCuoi)).as(c.path("ten").stringValue()).isEqualTo(v0);
        }
    }

    @Test
    void khongCoLoiGiaiMauThiRong() {
        assertThat(VietLoiGiai.viet(new Solution(UUID.randomUUID(), null, "[\"x = 0\"]", null))).isEmpty();
        assertThat(VietLoiGiai.viet(new Solution(UUID.randomUUID(), null, "[\"x = 0\"]", "x = 0"))).contains("x = 0");
    }

    @Test
    void lechKieuKhaiBaoCuaV0ThiRongKhongDoan() {
        // v0 sẽ in «Tập xác định: 1.» hay ném lỗi khi gọi join trên chuỗi: core không đưa chữ ép kiểu ra cho học sinh.
        assertThat(VietLoiGiai.viet("{\"TXD\": 1}", null)).isEmpty();
        assertThat(VietLoiGiai.viet("{\"TXD\": \"R\", \"y_phay_bang_0\": \"0, 2\"}", null)).isEmpty();
        assertThat(VietLoiGiai.viet("{\"TXD\": \"R\", \"ket_luan\": {\"dong_bien\": [1]}}", null)).isEmpty();
        assertThat(VietLoiGiai.viet("{\"TXD\": \"R\", \"ket_luan\": []}", null)).isEmpty();
        assertThat(VietLoiGiai.viet("{\"TXD\": \"R\", \"ket_luan\": {\"dong_bien\": [\"(0; 1)\"]}}", null))
            .contains("Tập xác định: R. Đồng biến trên (0; 1).");
    }
}
