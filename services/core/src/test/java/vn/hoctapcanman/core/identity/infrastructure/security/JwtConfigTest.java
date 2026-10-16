package vn.hoctapcanman.core.identity.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Base64;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import vn.hoctapcanman.core.identity.domain.repository.UserRepository;

/** Test ops đặt ngoài src/test để không đụng lane core; chạy bằng ops/test-jwt.sh. */
class JwtConfigTest {
    @Test
    void accessTokenCarriesEpochAndRejectsMissingOrDifferentEpoch() {
        var user = vn.hoctapcanman.core.identity.domain.model.User.create(
            new vn.hoctapcanman.core.identity.domain.model.Email("epoch@demo.local"), "hash", "Tài khoản thử",
            vn.hoctapcanman.core.identity.domain.model.Role.STUDENT, true, java.time.Instant.now());
        var users = org.mockito.Mockito.mock(UserRepository.class);
        org.mockito.Mockito.when(users.findById(user.id())).thenReturn(java.util.Optional.of(user));
        var config = new JwtConfig();
        var properties = new IdentityProperties("", java.time.Duration.ofMinutes(15), "test", false);
        var key = new javax.crypto.spec.SecretKeySpec(new byte[32], "HmacSHA256");
        var encoder = config.jwtEncoder(key);
        var decoder = config.jwtDecoder(key, properties, users);
        var now = java.time.Instant.now();
        var issued = new JwtAccessTokenIssuer(encoder, properties).issue(user, now);
        assertThat(decoder.decode(issued.value()).getClaims()).containsEntry("session_epoch", 0L);
        var updated = new vn.hoctapcanman.core.identity.domain.model.User(user.id(), user.email(), user.passwordHash(),
            user.displayName(), user.role(), true, true, user.createdAt(), now, 1);
        org.mockito.Mockito.when(users.findById(user.id())).thenReturn(java.util.Optional.of(updated));
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> decoder.decode(issued.value()))
            .isInstanceOf(org.springframework.security.oauth2.jwt.JwtValidationException.class);
        assertThat(decoder.decode(new JwtAccessTokenIssuer(encoder, properties).issue(updated, now).value()).getClaims())
            .containsEntry("session_epoch", 1L);
        org.mockito.Mockito.when(users.findById(user.id())).thenReturn(java.util.Optional.of(user));
        for (Object epoch : java.util.Arrays.asList(null, 1L, -1L, "0", 0.5)) {
            var claims = org.springframework.security.oauth2.jwt.JwtClaimsSet.builder().issuer("test")
                .subject(user.id().value().toString()).issuedAt(now).expiresAt(now.plusSeconds(900)).claim("role", "STUDENT");
            if (epoch != null) claims.claim("session_epoch", epoch);
            String token = encoder.encode(org.springframework.security.oauth2.jwt.JwtEncoderParameters.from(
                org.springframework.security.oauth2.jwt.JwsHeader.with(org.springframework.security.oauth2.jose.jws.MacAlgorithm.HS256).build(), claims.build())).getTokenValue();
            org.assertj.core.api.Assertions.assertThatThrownBy(() -> decoder.decode(token))
                .as("epoch %s", epoch).isInstanceOf(org.springframework.security.oauth2.jwt.JwtValidationException.class);
        }
    }

    private ApplicationContextRunner context(String profiles, String secret) {
        return new ApplicationContextRunner()
            .withUserConfiguration(JwtConfig.class)
            .withBean(UserRepository.class, () -> new UserRepository() {
                public java.util.Optional<vn.hoctapcanman.core.identity.domain.model.User> findById(
                        vn.hoctapcanman.core.identity.domain.model.UserId id) {
                    return java.util.Optional.empty();
                }
                public java.util.Optional<vn.hoctapcanman.core.identity.domain.model.User> findByEmail(
                        vn.hoctapcanman.core.identity.domain.model.Email email) {
                    return java.util.Optional.empty();
                }
                public vn.hoctapcanman.core.identity.domain.model.User save(
                        vn.hoctapcanman.core.identity.domain.model.User user) {
                    throw new UnsupportedOperationException();
                }
            })
            .withInitializer(c -> c.getEnvironment().setActiveProfiles(
                profiles.isEmpty() ? new String[0] : profiles.split(",")))
            .withPropertyValues("spring.profiles.active=" + profiles, "app.identity.jwt-secret=" + secret);
    }

    @ParameterizedTest
    @ValueSource(strings = {"prod", "prod,dev", "prod,test"})
    void missingSecretPreventsStartupInProd(String profiles) {
        context(profiles, " ").run(c -> {
            assertThat(c).hasFailed();
            assertThat(c.getStartupFailure()).hasRootCauseMessage(
                "Thiếu APP_IDENTITY_JWT_SECRET: profile prod phải cấp khóa base64 của ít nhất 32 byte.");
        });
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "dev", "test", "staging"})
    void nonProdCanGenerateTemporaryKey(String profiles) {
        context(profiles, "").run(c -> {
            assertThat(c).hasNotFailed();
            assertThat(c.getBean(SecretKey.class).getEncoded()).hasSize(32);
        });
    }

    @ParameterizedTest
    @ValueSource(strings = {"prod", "dev", "test"})
    void shortSecretFails(String profiles) {
        context(profiles, Base64.getEncoder().encodeToString(new byte[31])).run(c -> {
            assertThat(c).hasFailed();
            assertThat(c.getStartupFailure()).hasRootCauseMessage(
                "APP_IDENTITY_JWT_SECRET phải là base64 của ít nhất 32 byte");
        });
    }

    @Test
    void malformedSecretHasVietnameseMessageWithoutLeakingItsValue() {
        context("prod", "invalid-value!").run(c -> {
            assertThat(c).hasFailed();
            assertThat(c.getStartupFailure()).hasRootCauseMessage(
                "APP_IDENTITY_JWT_SECRET phải là base64 hợp lệ của ít nhất 32 byte.");
            assertThat(c.getStartupFailure().getMessage()).doesNotContain("invalid-value!");
        });
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "prod", "staging", "dev", "test"})
    void validSecretIsUsedAsSupplied(String profiles) {
        byte[] bytes = new byte[32];
        new java.security.SecureRandom().nextBytes(bytes);
        context(profiles, Base64.getEncoder().encodeToString(bytes)).run(c -> {
            assertThat(c).hasNotFailed();
            assertThat(c.getBean(SecretKey.class).getEncoded()).isEqualTo(bytes);
        });
    }
}
