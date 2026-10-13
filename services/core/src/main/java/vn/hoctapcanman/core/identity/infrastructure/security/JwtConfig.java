package vn.hoctapcanman.core.identity.infrastructure.security;

import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

/** Khóa HS256 dùng chung cho phát (JwtEncoder) và kiểm (JwtDecoder) access token. */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(IdentityProperties.class)
public class JwtConfig {

    private static final Logger LOG = LoggerFactory.getLogger(JwtConfig.class);
    private static final int SO_BYTE_TOI_THIEU = 32;

    @Bean
    SecretKey jwtSigningKey(IdentityProperties properties) {
        byte[] bytes;
        if (properties.jwtSecret().isBlank()) {
            LOG.warn("Chưa đặt APP_IDENTITY_JWT_SECRET: dùng khóa JWT tạm, token mất hiệu lực khi khởi động lại.");
            bytes = new byte[SO_BYTE_TOI_THIEU];
            new SecureRandom().nextBytes(bytes);
        } else {
            bytes = Base64.getDecoder().decode(properties.jwtSecret());
            if (bytes.length < SO_BYTE_TOI_THIEU) {
                throw new IllegalStateException("APP_IDENTITY_JWT_SECRET phải là base64 của ít nhất 32 byte");
            }
        }
        return new SecretKeySpec(bytes, "HmacSHA256");
    }

    @Bean
    JwtEncoder jwtEncoder(SecretKey jwtSigningKey) {
        return NimbusJwtEncoder.withSecretKey(jwtSigningKey).algorithm(MacAlgorithm.HS256).build();
    }

    @Bean
    JwtDecoder jwtDecoder(SecretKey jwtSigningKey, IdentityProperties properties, vn.hoctapcanman.core.identity.domain.repository.UserRepository users) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(jwtSigningKey).macAlgorithm(MacAlgorithm.HS256).build();
        decoder.setJwtValidator(new org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator<>(
            JwtValidators.createDefaultWithIssuer(properties.issuer()), jwt -> {
                try {
                    boolean active=users.findById(new vn.hoctapcanman.core.identity.domain.model.UserId(java.util.UUID.fromString(jwt.getSubject())))
                        .filter(u->u.enabled() && u.role().name().equals(jwt.getClaimAsString("role"))).isPresent();
                    if(active)return org.springframework.security.oauth2.core.OAuth2TokenValidatorResult.success();
                } catch(IllegalArgumentException ignored) { /* malformed subject fails closed */ }
                return org.springframework.security.oauth2.core.OAuth2TokenValidatorResult.failure(
                    new org.springframework.security.oauth2.core.OAuth2Error("invalid_token","Phiên không còn hợp lệ.",null));
            }));
        return decoder;
    }
}
