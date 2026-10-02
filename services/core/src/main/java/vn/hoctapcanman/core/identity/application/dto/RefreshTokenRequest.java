package vn.hoctapcanman.core.identity.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Thân yêu cầu của làm mới phiên và đăng xuất. */
public record RefreshTokenRequest(@NotBlank @Size(max = 128) String refreshToken) {

    @Override
    public String toString() {
        return "RefreshTokenRequest[***]";
    }
}
