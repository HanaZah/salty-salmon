package com.finadvise.crm.users;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@Tag(name = "User Management", description = "Admin-level user creation and personal profile updates")
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "Get Advisor by ID", description = "Fetches a specific Advisor's details. Restricted to Admin/Owner.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Advisor found"),
            @ApiResponse(responseCode = "404", description = "Advisor not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping("/{id}")
    public AdvisorDTO getAdvisorById(@PathVariable Long id) {
        return userService.getAdvisorById(id);
    }

    @Operation(summary = "Create Admin", description = "Registers a new System Administrator. Requires ROLE_ADMIN.")
    @ApiResponse(responseCode = "200", description = "Admin created successfully")@PostMapping("/new/admin")
    public AdminDTO createNewAdmin(@RequestBody CreateAdminRequestDTO request) {
        return userService.createAdmin(request);
    }

    @Operation(summary = "Create Advisor", description = "Registers a new Financial Advisor. Requires ROLE_ADMIN.")
    @ApiResponse(responseCode = "200", description = "Advisor created successfully")@PostMapping("/new/advisor")
    public AdvisorDTO createNewAdvisor(@RequestBody CreateAdvisorRequestDTO request) {
        return userService.createAdvisor(request);
    }

    @Operation(summary = "Change user password", description = "Allows an authenticated user to change their password after verifying the old one.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Password changed successfully"),
            @ApiResponse(responseCode = "400", description = "Validation failed: Old password incorrect or new password matches old",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PatchMapping("/password")
    public ResponseEntity<Void> changePassword(@RequestBody ChangePasswordRequestDTO request, Principal principal) {
        userService.changePassword(principal.getName(), request);
        return ResponseEntity.noContent().build(); // Standard 204 No Content for successful updates
    }

    @Operation(summary = "Update user profile", description = "Updates the first name, last name, and phone number of the authenticated user.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Profile updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input data provided",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "User not found in the system",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PutMapping("/profile")
    public ResponseEntity<Void> updateProfile(@RequestBody UpdateProfileRequestDTO request, Principal principal) {
        userService.updateProfile(principal.getName(), request);
        return ResponseEntity.noContent().build();
    }
}
