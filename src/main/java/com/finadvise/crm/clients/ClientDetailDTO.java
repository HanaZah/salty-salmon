package com.finadvise.crm.clients;

import com.finadvise.crm.addresses.AddressDTO;
import com.finadvise.crm.assets.ClientAssetsDTO;
import com.finadvise.crm.budget.BudgetFullDTO;
import com.finadvise.crm.documents.ClientDocumentsDTO;
import com.finadvise.crm.products.ClientProductsDTO;

import java.time.LocalDate;

public record ClientDetailDTO(
        // Core Client Data
        String clientUid,
        String personalId,
        LocalDate birthDate,
        String firstName,
        String lastName,
        String occupation,
        String phone,
        String email,

        // ID Card Details
        String idCardNumber,
        LocalDate idCardIssueDate,
        LocalDate idCardExpiryDate,
        String idCardIssuer,

        // Addresses
        AddressDTO permanentAddress,
        AddressDTO contactAddress,

        // Auditing & Status
        LocalDate lastUpdate,
        Integer version,

        // Orchestrated Domain Summaries
        ClientAssetsDTO assetsSummary,
        ClientProductsDTO productsSummary,
        ClientDocumentsDTO documentsSummary,
        BudgetFullDTO fullBudget
) {}