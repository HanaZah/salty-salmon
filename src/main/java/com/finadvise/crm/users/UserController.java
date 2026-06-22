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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@Tag(name = "User Self-Management", description = "Personal profile details, updates and support contacts")
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
    @GetMapping("/{employeeId}")
    public AdvisorDTO getAdvisorById(@PathVariable String employeeId, Principal principal) {
        return userService.getAdvisorByEmployeeId(employeeId, principal.getName());
    }

    @Operation(summary = "Change user password", description = "Allows an authenticated user to change their password after verifying the old one.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Password changed successfully"),
            @ApiResponse(responseCode = "400", description = "Validation failed: Old password incorrect or new password matches old",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PatchMapping("/password")
    public ResponseEntity<Void> changePassword(@RequestBody @Valid ChangePasswordRequestDTO request, Principal principal) {
        userService.changePassword(principal.getName(), request);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Update user profile", description = "Updates the first name, last name, and phone number of the authenticated user.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Profile updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input data provided",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "User not found in the system",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Optimistic locking failure: The profile was updated by someone else",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PutMapping("/profile")
    public ResponseEntity<Void> updateProfile(@RequestBody @Valid UpdateProfileRequestDTO request, Principal principal) {
        userService.updateProfile(principal.getName(), request);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Get current user profile", description = "Fetches the profile of the currently authenticated user based on their JWT.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profile retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "User not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping("/me")
    public Object getCurrentUser(Principal principal) {
        return userService.getMe(principal.getName());
    }


    @Operation(summary = "List all active Admins contacts", description = "Fetches a list of all active Admins contact information")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Contact list retrieved successfully")
    })
    @GetMapping("/admins/contacts")
    public List<AdminContactDTO> getAllAdminsContacts() {
        return userService.getActiveAdminContacts();
    }

}