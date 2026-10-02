package vn.hoctapcanman.core.identity.infrastructure.security;

import java.util.UUID;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import vn.hoctapcanman.core.identity.application.port.PasswordHasher;

/** BCrypt qua DelegatingPasswordEncoder (tiền tố {@code {bcrypt}}): đổi thuật toán sau này không phải băm lại hết. */
@Component
public class SpringPasswordHasher implements PasswordHasher {

    private final PasswordEncoder encoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();
    private final String dummyHash = encoder.encode(UUID.randomUUID().toString());

    @Override
    public String hash(String rawPassword) {
        return encoder.encode(rawPassword);
    }

    @Override
    public boolean matches(String rawPassword, String passwordHash) {
        return encoder.matches(rawPassword, passwordHash);
    }

    @Override
    public String dummyHash() {
        return dummyHash;
    }
}
