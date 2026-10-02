package vn.hoctoanai.core.identity.application.port;

public interface PasswordHasher {

    String hash(String rawPassword);

    boolean matches(String rawPassword, String passwordHash);

    /**
     * Băm hợp lệ của một chuỗi ngẫu nhiên: so mật khẩu với nó khi email không tồn tại, để thời gian phản hồi không lộ
     * tài khoản nào có thật.
     */
    String dummyHash();
}
