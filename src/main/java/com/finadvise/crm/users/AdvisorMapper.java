package com.finadvise.crm.users;

import org.springframework.stereotype.Component;

@Component
public class AdvisorMapper {

    // Overloaded method for the lightweight version (no stats)
    public AdvisorDTO toDto(Advisor advisor) {
        return toDto(advisor, null);
    }

    // Overloaded method for the heavy version (with stats)
    public AdvisorDTO toDto(Advisor advisor, AdvisorStatisticsDTO stats) {
        if (advisor == null) {
            return null;
        }

        Long managerId = advisor.getManager() != null ? advisor.getManager().getId() : null;

        return new AdvisorDTO(
                advisor.getVersion(),
                advisor.getEmployeeId(),
                advisor.getIco(),
                advisor.getFirstName(),
                advisor.getLastName(),
                advisor.getPhone(),
                advisor.getEmail(),
                managerId,
                stats
        );
    }
}