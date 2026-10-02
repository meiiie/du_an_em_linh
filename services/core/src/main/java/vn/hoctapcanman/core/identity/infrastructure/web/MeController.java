package vn.hoctapcanman.core.identity.infrastructure.web;

import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.hoctapcanman.core.identity.application.dto.UserDto;
import vn.hoctapcanman.core.identity.application.usecase.GetCurrentUserUseCase;

@RestController
public class MeController {

    private final GetCurrentUserUseCase currentUser;

    public MeController(GetCurrentUserUseCase currentUser) {
        this.currentUser = currentUser;
    }

    @GetMapping("/api/me")
    public UserDto me(@AuthenticationPrincipal Jwt jwt) {
        return currentUser.execute(UUID.fromString(jwt.getSubject()));
    }
}
