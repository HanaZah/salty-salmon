package com.finadvise.crm.assets;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@Tag(name = "Client Assets", description = "Operations for managing a client's assets and properties")
@RestController
@RequestMapping("/api/v1/clients/{clientUid}/assets")
@RequiredArgsConstructor
public class AssetController {

    private final AssetService assetService;

    @Operation(summary = "Get Client Assets", description = "Retrieves a list of all assets and their total aggregated value for a specific client.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Assets retrieved successfully"),
            @ApiResponse(responseCode = "403", description = "Access denied (Not the assigned advisor)"),
            @ApiResponse(responseCode = "404", description = "Client not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping
    public ClientAssetsDTO getClientAssets(@PathVariable String clientUid, Principal principal) {
        return assetService.getClientsAssets(clientUid, principal.getName());
    }

    @Operation(summary = "Create Asset", description = "Adds a new asset to the client's portfolio.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Asset created successfully"),
            @ApiResponse(responseCode = "400", description = "Validation failed for the request body",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "Access denied (Not the assigned advisor)"),
            @ApiResponse(responseCode = "404", description = "Client or Asset Type not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping
    public ResponseEntity<AssetDTO> createAsset(
            @PathVariable String clientUid,
            @Valid @RequestBody AssetDTO request,
            Principal principal) {

        AssetDTO createdAsset = assetService.createAsset(clientUid, request, principal.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(createdAsset);
    }

    @Operation(summary = "Update Asset", description = "Updates the details of an existing asset.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Asset updated successfully"),
            @ApiResponse(responseCode = "400", description = "Validation failed for the request body",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "Access denied (Not the assigned advisor or asset does not belong to client)"),
            @ApiResponse(responseCode = "404", description = "Client, Asset, or Asset Type not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PutMapping("/{assetId}")
    public ResponseEntity<AssetDTO> updateAsset(
            @PathVariable String clientUid,
            @PathVariable Long assetId,
            @Valid @RequestBody AssetDTO request,
            Principal principal) {

        AssetDTO updatedAsset = assetService.updateAsset(clientUid, assetId, request, principal.getName());
        return ResponseEntity.ok(updatedAsset);
    }

    @Operation(summary = "Delete Asset", description = "Removes an asset from the client's portfolio.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Asset deleted successfully"),
            @ApiResponse(responseCode = "403", description = "Access denied (Not the assigned advisor or asset does not belong to client)"),
            @ApiResponse(responseCode = "404", description = "Client or Asset not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @DeleteMapping("/{assetId}")
    public ResponseEntity<Void> deleteAsset(
            @PathVariable String clientUid,
            @PathVariable Long assetId,
            Principal principal) {

        assetService.deleteAsset(clientUid, assetId, principal.getName());
        return ResponseEntity.noContent().build();
    }
}
