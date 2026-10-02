package vn.hoctapcanman.core.identity.infrastructure.security;

import java.time.Instant;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;
import vn.hoctapcanman.core.identity.application.port.AccessTokenIssuer;
import vn.hoctapcanman.core.identity.domain.model.User;

/** Access token JWT HS256: {@code sub} = id người dùng, {@code role} = vai trò. Không đưa email vào token. */
@Component
public class JwtAccessTokenIssuer implements AccessTokenIssuer {

    private final JwtEncoder encoder;
    private final IdentityProperties properties;

    public JwtAccessTokenIssuer(JwtEncoder encoder, IdentityProperties properties) {
        this.encoder = encoder;
        this.properties = properties;
    }

    @Override
    public IssuedAccessToken issue(User user, Instant now) {
        Instant expiresAt = now.plus(properties.accessTokenTtl());
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(properties.issuer())
                .subject(user.id().value().toString())
                .issuedAt(now)
                .expiresAt(expiresAt)
                .claim("role", user.role().name())
                .build();
        String value = encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
        return new IssuedAccessToken(value, expiresAt);
    }
}
