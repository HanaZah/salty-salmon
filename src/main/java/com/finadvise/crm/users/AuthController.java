package com.finadvise.crm.users;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Authentication", description = "Endpoints for identity verification and JWT issuance")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserService userService;

    @Operation(summary = "User Login", description = "Authenticates credentials and returns a JWT access token.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Successfully authenticated"),
            @ApiResponse(responseCode = "401", description = "Invalid employee ID or password",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping("/login")
    public LoginResponseDTO login(@Valid @RequestBody LoginRequestDTO request) {
        return authService.authenticateAndGenerateToken(request);
    }

    @Operation(
            summary = "Password recovery info",
            description = "Provides active admin emails to contact for password recovery"
    )
    @ApiResponses(
            @ApiResponse(responseCode = "200", description = "Successful data fetch")
    )
    @GetMapping("/password-recovery")
    public PasswordRecoveryResponseDTO passwordRecovery() {
        return new PasswordRecoveryResponseDTO("Admin contact necessary", userService.getActiveAdminEmails());
    }
}
