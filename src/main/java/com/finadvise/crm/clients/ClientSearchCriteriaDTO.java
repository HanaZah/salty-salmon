package com.finadvise.crm.clients;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record ClientSearchCriteriaDTO(

        @Size(max = 10, message = "Personal ID must not exceed 10 digits")
        @Pattern(
                regexp = "^[0-9]+$",
                message = "Personal ID can only contain digits."
        )
        String personalId,

        @Size(max = 110, message = "Full name cannot exceed 110 characters")
        @Pattern(
                regexp = "^[\\p{L}\\p{M}\\s\\-']+$",
                message = "Full name contains invalid characters." +
                        "Please use only standard letters, possibly hyphen or apostrophe."
        )
        String fullName,

        @Size(max = 100)
        @Pattern(
                regexp = "^[\\p{L}\\p{M}\\s\\-']+$",
                message = "City name contains invalid characters." +
                        "Please use only standard letters, hyphen or apostrophes."
        )
        String city,

        @Size(max = 6, message = "Postal code must not exceed 6 digits")
        @Pattern(regexp = "^[0-9\\s]+$", message = "Postal code can only contain digits and space.")
        String postalCode,

        LocalDate idCardExpiryExact,
        LocalDate idCardExpiryBefore,
        LocalDate idCardExpiryAfter,

        LocalDate lastUpdateBefore,

        Integer minAge,
        Integer maxAge,

        Integer birthdayInNextDays,

        // Admins can populate this. For standard Advisors, the service layer
        // will overwrite this field with their own employeeId from the SecurityContext.
        @Size(max = 20, message = "Employee ID has wrong size or pattern")
        @Pattern(
                regexp = "^[a-zA-Z0-9\\-]+$",
                message = "Employee ID has wrong size or pattern"
        )
        String advisorEmployeeId
) {
    /**
     * Creates a secure copy of the criteria, forcibly assigning the provided advisor ID.
     * Intended solely for internal use by the service layer.
     */
    public ClientSearchCriteriaDTO withAdvisorEmployeeId(String forcedAdvisorId) {
        return new ClientSearchCriteriaDTO(
                this.personalId, this.fullName, this.city, this.postalCode,
                this.idCardExpiryExact, this.idCardExpiryBefore, this.idCardExpiryAfter,
                this.lastUpdateBefore, this.minAge, this.maxAge, this.birthdayInNextDays,
                forcedAdvisorId
        );
    }
}
