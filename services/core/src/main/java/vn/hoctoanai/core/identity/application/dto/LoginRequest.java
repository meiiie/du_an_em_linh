package vn.hoctoanai.core.identity.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** BCrypt chỉ dùng 72 byte đầu nên mật khẩu tối đa 72 ký tự (như v0). */
public record LoginRequest(@NotBlank @Size(max = 254) String email, @NotBlank @Size(max = 72) String password) {

    @Override
    public String toString() {
        return "LoginRequest[***]";
    }
}
