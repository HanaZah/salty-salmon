package com.finadvise.crm.users;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private JwtEncoder jwtEncoder;

    @Mock
    private AuthenticationManager authManager;

    @InjectMocks
    private AuthService authService;

    @Captor
    private ArgumentCaptor<JwtEncoderParameters> encoderParametersCaptor;

    @BeforeEach
    void setUp() {
        // Injecting the @Value field without a Spring Context
        ReflectionTestUtils.setField(authService, "SELF", "crm-backend");
    }

    @Test
    void authenticateAndGenerateToken_ReturnsLoginResponse_OnValidCredentials() {
        LoginRequestDTO request = new LoginRequestDTO("A1234567", "CorrectPassword");

        Advisor mockAdvisor = Advisor.builder()
                .employeeId("A1234567")
                .firstName("John")
                .lastName("Doe")
                .build();
        CustomUserDetails mockUserDetails = new CustomUserDetails(mockAdvisor);

        Authentication successfulAuth = new UsernamePasswordAuthenticationToken(
                mockUserDetails,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_ADVISOR"))
        );

        Jwt mockJwt = mock(Jwt.class);
        when(mockJwt.getTokenValue()).thenReturn("mocked.jwt.token");

        when(authManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(successfulAuth);
        when(jwtEncoder.encode(any(JwtEncoderParameters.class))).thenReturn(mockJwt);

        LoginResponseDTO response = authService.authenticateAndGenerateToken(request);

        assertThat(response).isNotNull();
        assertThat(response.token()).isEqualTo("mocked.jwt.token");
        assertThat(response.name()).isEqualTo("John Doe");
        assertThat(response.expiresInSeconds()).isEqualTo(36000); // 10 hours

        verify(jwtEncoder).encode(encoderParametersCaptor.capture());
        JwtEncoderParameters capturedParams = encoderParametersCaptor.getValue();

        assertThat(capturedParams.getClaims().getSubject()).isEqualTo("A1234567");
        assertThat(capturedParams.getClaims().getClaimAsString("name")).isEqualTo("John Doe");
        assertThat(capturedParams.getClaims().getClaimAsString("scope")).isEqualTo("ROLE_ADVISOR");
    }

    @Test
    void authenticateAndGenerateToken_ThrowsException_OnInvalidCredentials() {
        LoginRequestDTO request = new LoginRequestDTO("A1234567", "WrongPassword");

        when(authManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> authService.authenticateAndGenerateToken(request))
                .isInstanceOf(BadCredentialsException.class);

        verify(jwtEncoder, never()).encode(any());
    }
}
