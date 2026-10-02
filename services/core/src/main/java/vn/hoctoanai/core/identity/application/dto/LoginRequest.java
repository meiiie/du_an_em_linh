package vn.hoctoanai.core.identity.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** BCrypt chỉ nhận 72 byte: giới hạn mật khẩu theo byte UTF-8, không theo ký tự (chữ có dấu chiếm 2–3 byte). */
public record LoginRequest(@NotBlank @Size(max = 254) String email, @NotBlank @ToiDaByteUtf8(72) String password) {

    @Override
    public String toString() {
        return "LoginRequest[***]";
    }
}
