package com.finadvise.crm.administration;

import com.finadvise.crm.users.*;
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

@Tag(name = "Global User Management", description = "Admin-level user creation, updates and lifecycle management")
@RestController
@RequestMapping("/api/v1/administration/users")
@RequiredArgsConstructor
public class UserAdministrationController {
    private final UserService userService;

    @Operation(summary = "Create Admin", description = "Registers a new System Administrator. Requires ROLE_ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Admin created successfully"),
            @ApiResponse(responseCode = "400", description = "Validation failed (e.g., missing fields or invalid email)",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Email already in use",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping("/new/admin")
    @PreAuthorize("hasAuthority('ADMIN')")
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
    @PreAuthorize("hasAuthority('ADMIN')")
    public AdvisorDTO createNewAdvisor(@RequestBody @Valid CreateAdvisorRequestDTO request) {
        return userService.createAdvisor(request);
    }

    @Operation(summary = "Search Advisors", description = "Returns a paginated, filtered list of advisors in the system. Requires ROLE_ADMIN.")
    @ApiResponse(responseCode = "200", description = "List retrieved successfully")
    @GetMapping("/advisors")
    @PreAuthorize("hasAuthority('ADMIN')")
    public Page<AdvisorDTO> searchAdvisors(AdvisorSearchCriteriaDTO criteria, Pageable pageable) {
        return userService.searchAdvisors(criteria, pageable);
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
    @PreAuthorize("hasAuthority('ADMIN')")
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
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Void> deactivateUser(@PathVariable String employeeId) {
        userService.deactivateUser(employeeId);
        return ResponseEntity.noContent().build();
    }
}
