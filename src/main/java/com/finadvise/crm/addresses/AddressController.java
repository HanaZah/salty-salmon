package com.finadvise.crm.addresses;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/addresses")
@RequiredArgsConstructor
public class AddressController {

    private final AddressService addressService;

    @PostMapping
    public ResponseEntity<AddressDTO> create(@Valid @RequestBody AddressDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(addressService.findOrCreateAddress(dto));
    }

}
