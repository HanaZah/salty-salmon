package com.finadvise.crm.addresses;

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

@Tag(name = "Address Services", description = "Operations for normalized address management")
@RestController
@RequestMapping("/api/v1/addresses")
@RequiredArgsConstructor
public class AddressController {

    private final AddressService addressService;

    @Operation(summary = "Find or Create Address",
            description = "Checks for an existing identical address to prevent duplicates. If not found, validates and creates a new one.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Address successfully retrieved or created"),
            @ApiResponse(responseCode = "422", description = "Address failed external validation (e.g., RÚIAN check)",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "400", description = "Invalid address format or missing fields")
    })
    @PostMapping
    public ResponseEntity<AddressDTO> create(@Valid @RequestBody AddressDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(addressService.findOrCreateAddress(dto));
    }

}
