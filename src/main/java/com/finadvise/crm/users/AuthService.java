package com.finadvise.crm.users;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthService {

    @Value( "${SELF}")
    private String SELF;
    private final JwtEncoder jwtEncoder;
    private final AuthenticationManager authManager;

    public LoginResponseDTO authenticateAndGenerateToken(LoginRequestDTO request) {

        Authentication auth = authManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.employeeId(), request.password())
        );

        Instant now = Instant.now();
        Instant expiresAt = now.plus(10, ChronoUnit.HOURS);
        long durationSeconds = Duration.between(now, expiresAt).toSeconds();
        User user = ((CustomUserDetails) Objects.requireNonNull(auth.getPrincipal())).user();
        String name = user.getFirstName() + " " + user.getLastName();
        String authorities = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority).collect(Collectors.joining(" "));

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(SELF)
                .issuedAt(now)
                .expiresAt(expiresAt)
                .subject(auth.getName())
                .audience(List.of(SELF))
                .claim("name", name)
                .claim("scope", authorities)
                .build();

        JwsHeader jwsHeader = JwsHeader.with(MacAlgorithm.HS256).build();
        String jwt = jwtEncoder.encode(JwtEncoderParameters.from(jwsHeader, claims)).getTokenValue();
        return new LoginResponseDTO(jwt, name, auth.getName(), durationSeconds);
    }
}
