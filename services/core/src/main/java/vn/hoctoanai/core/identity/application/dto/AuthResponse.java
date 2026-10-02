package vn.hoctoanai.core.identity.application.dto;

import java.time.Instant;

public record AuthResponse(
        String accessToken,
        Instant accessTokenExpiresAt,
        String refreshToken,
        Instant refreshTokenExpiresAt,
        UserDto user) {

    @Override
    public String toString() {
        return "AuthResponse[user=" + user.id() + "]";
    }
}
