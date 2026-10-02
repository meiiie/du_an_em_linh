package vn.hoctapcanman.core.identity.application.port;

public interface RefreshTokenGenerator {

    /** Chuỗi ngẫu nhiên an toàn (256 bit), mã base64url. */
    String newToken();
}
