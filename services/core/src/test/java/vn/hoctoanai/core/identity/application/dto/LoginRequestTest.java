package vn.hoctoanai.core.identity.application.dto;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.Set;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** BCrypt nhận tối đa 72 byte: giới hạn theo byte UTF-8, không theo ký tự. */
class LoginRequestTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        factory.close();
    }

    private static Set<ConstraintViolation<LoginRequest>> kiem(String matKhau) {
        return validator.validate(new LoginRequest("hs.an@demo.local", matKhau));
    }

    @Test
    void matKhauTinhTheoByteUtf8() {
        assertThat(kiem("a".repeat(72))).isEmpty();
        assertThat(kiem("ệ".repeat(24))).as("24 ký tự × 3 byte = 72 byte").isEmpty();
        assertThat(kiem("a".repeat(73))).extracting(v -> v.getPropertyPath().toString()).containsExactly("password");
        assertThat(kiem("ệ".repeat(25))).as("25 ký tự nhưng 75 byte").extracting(v -> v.getPropertyPath().toString())
            .containsExactly("password");
    }
}
