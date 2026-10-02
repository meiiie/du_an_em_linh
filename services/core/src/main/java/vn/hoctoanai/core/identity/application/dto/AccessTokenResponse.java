package vn.hoctoanai.core.identity.application.dto;

import java.time.Instant;

/** Thân phản hồi đăng nhập / làm mới: access token và người dùng. Refresh token đi riêng trong cookie HttpOnly. */
public record AccessTokenResponse(String accessToken, Instant accessTokenExpiresAt, UserDto user) {

    public static AccessTokenResponse from(AuthResponse phien) {
        return new AccessTokenResponse(phien.accessToken(), phien.accessTokenExpiresAt(), phien.user());
    }

    @Override
    public String toString() {
        return "AccessTokenResponse[user=" + user.id() + "]";
    }
}
