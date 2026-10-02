package vn.hoctoanai.core.identity.infrastructure.web;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.hoctoanai.core.identity.application.dto.AuthResponse;
import vn.hoctoanai.core.identity.application.dto.LoginRequest;
import vn.hoctoanai.core.identity.application.dto.RefreshTokenRequest;
import vn.hoctoanai.core.identity.application.usecase.LoginUseCase;
import vn.hoctoanai.core.identity.application.usecase.LogoutUseCase;
import vn.hoctoanai.core.identity.application.usecase.RefreshSessionUseCase;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final LoginUseCase login;
    private final RefreshSessionUseCase refresh;
    private final LogoutUseCase logout;

    public AuthController(LoginUseCase login, RefreshSessionUseCase refresh, LogoutUseCase logout) {
        this.login = login;
        this.refresh = refresh;
        this.logout = logout;
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return login.execute(request);
    }

    @PostMapping("/refresh")
    public AuthResponse refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return refresh.execute(request);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshTokenRequest request) {
        logout.execute(request);
        return ResponseEntity.noContent().build();
    }
}
