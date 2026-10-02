package vn.hoctoanai.core.identity.infrastructure.security;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * {@code app.identity.*}. {@code jwtSecret}: base64 của ít nhất 32 byte (HS256), lấy từ biến môi trường
 * {@code APP_IDENTITY_JWT_SECRET}; để trống thì dùng khóa tạm sinh lúc khởi động (token mất hiệu lực khi khởi động lại).
 */
@ConfigurationProperties("app.identity")
public record IdentityProperties(
        @DefaultValue("") String jwtSecret,
        @DefaultValue("PT15M") Duration accessTokenTtl,
        @DefaultValue("hoc-toan-ai-core") String issuer) {}
