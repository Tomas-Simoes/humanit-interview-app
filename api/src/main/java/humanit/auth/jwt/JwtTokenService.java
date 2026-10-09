package humanit.auth.jwt;

import humanit.auth.dto.LoginResponse;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;

@Service
public class JwtTokenService {
  private final JwtEncoder jwtEncoder;
  private final SecurityProperties securityProperties;

  public JwtTokenService(JwtEncoder jwtEncoder, SecurityProperties securityProperties) {
    this.jwtEncoder = jwtEncoder;
    this.securityProperties = securityProperties;
  }

  public LoginResponse createAccessToken(Authentication authentication) {
    SecurityProperties.Jwt jwt = securityProperties.jwt();

    Instant now = Instant.now();
    Instant expiresAt = now.plusSeconds(jwt.expirationSeconds());

    List<String> roles =
        authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();

    JwtClaimsSet claims =
        JwtClaimsSet.builder()
            .issuer(jwt.issuer())
            .subject(authentication.getName())
            .audience(List.of(jwt.audience()))
            .issuedAt(now)
            .expiresAt(expiresAt)
            .id(UUID.randomUUID().toString())
            .claim("roles", roles)
            .build();

    JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();

    String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();

    return LoginResponse.bearer(token, jwt.expirationSeconds());
  }
}
