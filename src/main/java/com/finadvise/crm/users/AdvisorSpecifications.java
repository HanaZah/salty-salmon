package com.finadvise.crm.users;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class AdvisorSpecifications {

    public static Specification<Advisor> withCriteria(AdvisorSearchCriteriaDTO criteria) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (StringUtils.hasText(criteria.employeeId())) {
                // Join to the parent User table to access employeeId
                predicates.add(cb.equal(root.get("employeeId"), criteria.employeeId()));
            }

            if (StringUtils.hasText(criteria.lastName())) {
                predicates.add(cb.like(cb.lower(root.get("lastName")), "%" + criteria.lastName().toLowerCase() + "%"));
            }

            if (StringUtils.hasText(criteria.ico())) {
                predicates.add(cb.equal(root.get("ico"), criteria.ico()));
            }

            if (StringUtils.hasText(criteria.managerId())) {
                // Assuming Advisor has a 'manager' field mapped to another Advisor
                predicates.add(cb.equal(root.get("manager").get("employeeId"), criteria.managerId()));
            }

            if (criteria.isActive() != null) {
                predicates.add(cb.equal(root.get("isActive"), criteria.isActive()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
