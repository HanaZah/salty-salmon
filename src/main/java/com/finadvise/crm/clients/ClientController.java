package com.finadvise.crm.clients;

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
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@Tag(name = "Clients", description = "Core operations for managing client lifecycle, details, and search capabilities.")
@RestController
@RequestMapping("/api/v1/clients")
@RequiredArgsConstructor
public class ClientController {

    private final ClientService clientService;
    private final ClientDetailFacade clientDetailFacade;

    @Operation(summary = "Get Client Detail", description = "Retrieves the fully aggregated client view, including cross-domain summaries (assets, products, documents, budget).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Client details retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Client not found or access denied",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping("/{clientUid}")
    public ResponseEntity<ClientDetailDTO> getClientDetail(
            @PathVariable String clientUid,
            Principal principal) {

        return ResponseEntity.ok(clientDetailFacade.getClientDetail(clientUid, principal.getName()));
    }

    @Operation(summary = "Create Client", description = "Registers a new client, generates a business UID, and resolves addresses.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Client created successfully"),
            @ApiResponse(responseCode = "400", description = "Validation failed for the request payload",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Conflict: Client with this personal ID already exists",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping
    public ResponseEntity<ClientDetailDTO> createClient(
            @Valid @RequestBody ClientCreateRequestDTO request,
            Authentication authentication) {

        ClientDetailDTO createdClient = clientService.createClient(
                request,
                authentication.getName(),
                isAdmin(authentication)
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(createdClient);
    }

    @Operation(summary = "Update General Details", description = "Updates a client's basic information and contact/permanent addresses.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Client details updated successfully"),
            @ApiResponse(responseCode = "400", description = "Validation failed for the request payload",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Client not found or access denied",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PutMapping("/{clientUid}/details")
    public ResponseEntity<Void> updateClientDetails(
            @PathVariable String clientUid,
            @Valid @RequestBody ClientUpdateDetailsRequestDTO request,
            Principal principal) {

        clientService.updateClientDetails(clientUid, request, principal.getName());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Update ID Card", description = "Updates the client's ID card details, enforcing chronological validation on dates.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "ID card updated successfully"),
            @ApiResponse(responseCode = "400", description = "Validation failed for dates or payload",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Client not found or access denied",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PutMapping("/{clientUid}/id-card")
    public ResponseEntity<Void> updateClientIdCard(
            @PathVariable String clientUid,
            @Valid @RequestBody ClientUpdateIdCardRequestDTO request,
            Principal principal) {

        clientService.updateClientIdCard(clientUid, request, principal.getName());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Delete Client", description = "Performs a soft delete of the client. Allowed for the owning advisor or administrators.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Client soft-deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Client not found or access denied",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @DeleteMapping("/{clientUid}")
    public ResponseEntity<Void> deleteClient(
            @PathVariable String clientUid,
            Authentication authentication) {

        clientService.deleteClient(clientUid, authentication.getName(), isAdmin(authentication));
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Search Clients", description = "Executes a dynamic search against the optimized database view. Non-admin users are restricted to searching their own portfolio.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Search executed successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid search parameters",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping("/search")
    public ResponseEntity<Page<ClientSearchMinimalDTO>> searchClients(
            @Valid @RequestBody ClientSearchCriteriaDTO criteria,
            Pageable pageable,
            Authentication authentication) {

        Page<ClientSearchMinimalDTO> results = clientService.searchClients(
                criteria,
                authentication.getName(),
                isAdmin(authentication),
                pageable
        );
        return ResponseEntity.ok(results);
    }

    @Operation(summary = "Get Dashboard Summaries", description = "Retrieves a paginated list of recent client interactions for the user's dashboard.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Dashboard summaries retrieved successfully")
    })
    @GetMapping("/dashboard")
    public ResponseEntity<Page<ClientDashboardSummary>> getRecentClientSummaries(
            @RequestParam(defaultValue = "10") int pageSize,
            Authentication authentication) {

        Page<ClientDashboardSummary> summaries = clientService.getRecentClientSummaries(
                authentication.getName(),
                isAdmin(authentication),
                pageSize
        );
        return ResponseEntity.ok(summaries);
    }

    /**
     * Helper method to determine if the authenticated user holds the ADMIN authority.
     */
    private boolean isAdmin(Authentication authentication) {
        if (authentication == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ADMIN"::equals);
    }
}
