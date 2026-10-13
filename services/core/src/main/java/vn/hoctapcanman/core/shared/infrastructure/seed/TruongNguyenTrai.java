package vn.hoctapcanman.core.shared.infrastructure.seed;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.springframework.core.io.ClassPathResource;
import tools.jackson.databind.json.JsonMapper;

/** Danh sách hoàn toàn hư cấu (ADR 006); chỉ các seeder profile dev dùng, không có thông tin đăng nhập bí mật. */
public final class TruongNguyenTrai {
    public static final String TEN = "Trường THPT Nguyễn Trãi";
    public static final String NAM_HOC = "2026-2027";
    public static final List<String> LOP = List.of("10A1", "11A2", "12A3");
    public record TaiKhoan(String email, String ten, String vaiTro, List<String> lop) {}
    private TruongNguyenTrai() {}

    public static UUID id(String key) {
        return UUID.nameUUIDFromBytes(("mathl-dev-nguyen-trai-2026:" + key).getBytes(StandardCharsets.UTF_8));
    }

    public static List<TaiKhoan> taiKhoan() {
        try {
            String text = new ClassPathResource("seed-nguyen-trai/nguoi-dung.json").getContentAsString(StandardCharsets.UTF_8);
            return List.copyOf(Arrays.asList(JsonMapper.builder().build().readValue(text, TaiKhoan[].class)));
        } catch (IOException e) {
            throw new UncheckedIOException("Không đọc được danh sách trường hư cấu", e);
        }
    }
}
