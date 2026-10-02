package vn.hoctapcanman.core.identity.application.dto;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.nio.charset.StandardCharsets;
import org.jspecify.annotations.Nullable;

public class ToiDaByteUtf8Validator implements ConstraintValidator<ToiDaByteUtf8, String> {

    private int toiDa;

    @Override
    public void initialize(ToiDaByteUtf8 annotation) {
        toiDa = annotation.value();
    }

    @Override
    public boolean isValid(@Nullable String value, ConstraintValidatorContext context) {
        return value == null || value.getBytes(StandardCharsets.UTF_8).length <= toiDa;
    }
}
