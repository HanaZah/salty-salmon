package com.finadvise.crm.clients;

import java.time.LocalDate;

public record ClientSearchCriteriaDTO(
        String personalId,
        String fullName,
        String city,
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
