package com.finadvise.crm.clients;

import com.finadvise.crm.addresses.AddressInputDTO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.time.LocalDate;

public record ClientCreateRequestDTO(
        @NotBlank(message = "Personal ID is required")
        @Size(max = 10, message = "Personal ID cannot exceed 10 characters")
        String personalId,

        @NotNull(message = "Birth date is required")
        @Past(message = "Birth date must be in the past")
        LocalDate birthDate,

        @NotBlank(message = "First name is required")
        @Size(max = 50, message = "First name cannot exceed 50 characters")
        String firstName,

        @NotBlank(message = "Last name is required")
        @Size(max = 50, message = "Last name cannot exceed 50 characters")
        String lastName,

        @NotBlank(message = "Occupation is required")
        @Size(max = 100, message = "Occupation cannot exceed 100 characters")
        String occupation,

        @NotBlank(message = "Phone number is required")
        @Size(max = 20, message = "Phone number cannot exceed 20 characters")
        String phone,

        @Email(message = "Invalid email format")
        @Size(max = 254, message = "Email cannot exceed 254 characters")
        String email,

        @NotBlank(message = "ID card number is required")
        String idCardNumber,

        @NotNull(message = "ID card issue date is required")
        @PastOrPresent(message = "ID card issue date cannot be in the future")
        LocalDate idCardIssueDate,

        @NotNull(message = "ID card expiry date is required")
        @Future(message = "ID card expiry date must be in the future")
        LocalDate idCardExpiryDate,

        @NotBlank(message = "ID card issuer is required")
        @Size(max = 100, message = "ID card issuer cannot exceed 100 characters")
        String idCardIssuer,

        @NotNull(message = "Permanent address must be provided")
        @Valid
        AddressInputDTO permanentAddress,

        @NotNull(message = "Contact address must be provided")
        @Valid
        AddressInputDTO contactAddress
) {}