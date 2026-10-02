package vn.hoctoanai.core.identity.domain.model;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;

/**
 * Khóa đếm lần đăng nhập sai: SHA-256 (hex) của email đã chuẩn hóa và IP, như v0 ({@code apps/web/lib/gioi-han.ts}).
 * Chỉ lưu băm, không lưu email hay IP. Email không hợp lệ vẫn có khóa riêng, để không lộ email nào có thật.
 */
public record LoginAttemptKey(String hash) {

    public static LoginAttemptKey of(String email, String ip) {
        String chuanHoa = email.strip().toLowerCase(Locale.ROOT) + "|" + (ip.isBlank() ? "-" : ip.strip());
        try {
            byte[] bam = MessageDigest.getInstance("SHA-256").digest(chuanHoa.getBytes(StandardCharsets.UTF_8));
            return new LoginAttemptKey(HexFormat.of().formatHex(bam));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("JVM thiếu SHA-256", e);
        }
    }

    @Override
    public String toString() {
        return "LoginAttemptKey[***]";
    }
}
