package com.finadvise.crm.products;

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
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@Tag(name = "Client Products", description = "Operations for managing client financial products and executing product searches")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @Operation(summary = "Get Client Products", description = "Retrieves a list of all products and the total active count for a specific client.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Products retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Client not found or access denied (opaque response)",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping("/clients/{clientUid}/products")
    public ResponseEntity<ClientProductsDTO> getClientProducts(
            @PathVariable String clientUid,
            Principal principal) {

        return ResponseEntity.ok(productService.getClientProducts(clientUid, principal.getName()));
    }

    @Operation(summary = "Create Product", description = "Arranges a new financial product for the client.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Product created successfully"),
            @ApiResponse(responseCode = "400", description = "Validation failed for the request body",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "404", description = "Product Type or Provider not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping("/clients/{clientUid}/products")
    public ResponseEntity<ProductDTO> createProduct(
            @PathVariable String clientUid,
            @Valid @RequestBody ProductDTO request,
            Principal principal) {

        ProductDTO createdProduct = productService.createProduct(clientUid, request, principal.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(createdProduct);
    }

    @Operation(summary = "Update Product", description = "Updates the mutable details of an existing product.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Product updated successfully"),
            @ApiResponse(responseCode = "400", description = "Validation failed for the request body",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Product/Client not found or access denied (opaque response)",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PutMapping("/clients/{clientUid}/products/{productId}")
    public ResponseEntity<ProductDTO> updateProduct(
            @PathVariable String clientUid,
            @PathVariable Long productId,
            @Valid @RequestBody ProductDTO request,
            Principal principal) {

        ProductDTO updatedProduct = productService.updateProduct(clientUid, productId, request, principal.getName());
        return ResponseEntity.ok(updatedProduct);
    }

    @Operation(summary = "Delete Product", description = "Removes a product from the client's portfolio.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Product deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Product/Client not found or access denied (opaque response)",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @DeleteMapping("/clients/{clientUid}/products/{productId}")
    public ResponseEntity<Void> deleteProduct(
            @PathVariable String clientUid,
            @PathVariable Long productId,
            Principal principal) {

        productService.deleteProduct(clientUid, productId, principal.getName());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Search Products", description = "Searches for products based on dynamic criteria with pagination support.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Search executed successfully"),
            @ApiResponse(responseCode = "400", description = "Validation failed for the search criteria",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping("/products/search")
    public ResponseEntity<Page<ProductDTO>> searchProducts(
            @Valid @RequestBody ProductSearchCriteriaDTO criteria,
            Pageable pageable,
            Principal principal) {

        return ResponseEntity.ok(productService.searchProducts(criteria, principal.getName(), pageable));
    }
}
