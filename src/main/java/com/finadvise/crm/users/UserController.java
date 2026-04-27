package com.finadvise.crm.users;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
    @GetMapping("/{employeeId}")
    public AdvisorDTO getAdvisorById(@PathVariable String employeeId, Principal principal) {
        return userService.getAdvisorByEmployeeId(employeeId, principal.getName());
    }

    @Operation(summary = "Create Admin", description = "Registers a new System Administrator. Requires ROLE_ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Admin created successfully"),
            @ApiResponse(responseCode = "400", description = "Validation failed (e.g., missing fields or invalid email)",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Email already in use",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping("/new/admin")
    public AdminDTO createNewAdmin(@RequestBody @Valid CreateAdminRequestDTO request) {
        return userService.createAdmin(request);
    }

    @Operation(summary = "Create Advisor", description = "Registers a new Financial Advisor. Requires ROLE_ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Advisor created successfully"),
            @ApiResponse(responseCode = "400", description = "Validation failed (e.g., missing fields, invalid ICO format)",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Email or ICO already in use",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping("/new/advisor")
    public AdvisorDTO createNewAdvisor(@RequestBody @Valid CreateAdvisorRequestDTO request) {
        return userService.createAdvisor(request);
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

    @Operation(summary = "List all Advisors", description = "Returns a paginated list of all advisors in the system. Requires ROLE_ADMIN.")
    @ApiResponse(responseCode = "200", description = "List retrieved successfully")
    @GetMapping("/advisors")
    @PreAuthorize("hasRole('ADMIN')")
    public Page<AdvisorDTO> getAllAdvisors(Pageable pageable) {
        return userService.getAllAdvisors(pageable);
    }

    @Operation(summary = "Assign Manager", description = "Assigns or removes a manager for an advisor. Requires ROLE_ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Manager assigned successfully"),
            @ApiResponse(responseCode = "400", description = "Circular reference detected",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Advisor or Manager not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PatchMapping("/{employeeId}/manager")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> assignManager(
            @PathVariable String employeeId,
            @RequestBody AssignManagerRequestDTO request) {
        userService.assignManager(employeeId, request.managerEmployeeId());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Deactivate User", description = "Soft deletes a user by setting isActive to false. Requires ROLE_ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "User deactivated successfully"),
            @ApiResponse(responseCode = "404", description = "User not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @DeleteMapping("/{employeeId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deactivateUser(@PathVariable String employeeId) {
        userService.deactivateUser(employeeId);
        return ResponseEntity.noContent().build();
    }
}