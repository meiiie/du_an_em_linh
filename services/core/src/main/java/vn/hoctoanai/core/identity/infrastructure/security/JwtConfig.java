package vn.hoctoanai.core.identity.infrastructure.security;

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
    JwtDecoder jwtDecoder(SecretKey jwtSigningKey, IdentityProperties properties) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(jwtSigningKey).macAlgorithm(MacAlgorithm.HS256).build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(properties.issuer()));
        return decoder;
    }
}
