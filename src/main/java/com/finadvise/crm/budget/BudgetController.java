package com.finadvise.crm.budget;

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

@Tag(name = "Client Budget", description = "Operations for managing a client's income and expenses")
@RestController
@RequestMapping("/api/v1/clients/{clientId}/budget")
@RequiredArgsConstructor
public class BudgetController {

    private final BudgetService budgetService;

    @Operation(summary = "Update Client Budget", description = "Synchronizes the client's budget. Creates, updates, or removes items based on the payload.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Budget successfully updated"),
            @ApiResponse(responseCode = "400", description = "Validation failed (e.g., Missing sync version)",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "Access denied (Not the assigned advisor)"),
            @ApiResponse(responseCode = "409", description = "Optimistic locking failure (Concurrent modification)",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PutMapping
    public ResponseEntity<Void> updateBudget(
            @PathVariable Long clientId,
            @Valid @RequestBody BudgetFullDTO request,
            Principal principal) {

        budgetService.updateFullBudget(clientId, request, principal.getName());
        return ResponseEntity.noContent().build();
    }
}
