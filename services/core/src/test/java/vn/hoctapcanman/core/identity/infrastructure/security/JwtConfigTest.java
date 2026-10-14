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
