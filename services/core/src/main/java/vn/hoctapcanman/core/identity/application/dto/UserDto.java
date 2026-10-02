package vn.hoctapcanman.core.identity.application.dto;

import java.util.UUID;
import vn.hoctapcanman.core.identity.domain.model.User;

public record UserDto(UUID id, String email, String displayName, String role) {

    public static UserDto from(User user) {
        return new UserDto(user.id().value(), user.email().value(), user.displayName(), user.role().name());
    }
}
