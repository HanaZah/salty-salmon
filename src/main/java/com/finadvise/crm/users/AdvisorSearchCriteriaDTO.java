package com.finadvise.crm.users;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AdvisorSearchCriteriaDTO(

        @Size(max = 20, message = "Employee ID has wrong size or pattern")
        @Pattern(regexp = "^[A-Za-z0-9-]*$", message = "Employee ID has wrong size or pattern")
        String employeeId,

        @Size(max = 50, message = "Last name must not exceed 50 characters")
        @Pattern(regexp = "^[\\p{L}\\p{M}\\s\\-']+$",
                message = "Last name contains invalid characters" +
                        "Please use only standard letters, possibly hyphen or apostrophe."
        )
        String lastName,

        @Pattern(regexp = "^$|^\\d{8}$", message = "ICO must be exactly 8 digits")
        String ico,

        @Size(max = 20, message = "Manager ID has wrong size or pattern")
        @Pattern(regexp = "^[A-Za-z0-9-]*$", message = "Manager ID has wrong size or pattern")
        String managerId,

        Boolean isActive
) {}
