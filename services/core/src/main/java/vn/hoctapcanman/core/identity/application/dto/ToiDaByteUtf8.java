package vn.hoctapcanman.core.identity.application.dto;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Độ dài tối đa tính theo byte UTF-8 (BCrypt chỉ nhận 72 byte; chữ có dấu chiếm 2–3 byte). */
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ToiDaByteUtf8Validator.class)
public @interface ToiDaByteUtf8 {

    int value();

    String message() default "dài quá {value} byte";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
