package com.finadvise.crm.clients;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ClientSpecifications {

    // Private constructor to prevent instantiation of utility class
    private ClientSpecifications() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    public static Specification<ClientSearchMinimal> withCriteria(ClientSearchCriteriaDTO criteria, LocalDate today, boolean isActive) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.equal(root.get("isActive"), isActive));

            if (criteria.advisorEmployeeId() != null && !criteria.advisorEmployeeId().isBlank()) {
                predicates.add(cb.equal(root.get("advisorEmployeeId"), criteria.advisorEmployeeId()));
            }

            if (criteria.personalId() != null && !criteria.personalId().isBlank()) {
                predicates.add(cb.equal(root.get("personalId"), criteria.personalId()));
            }

            if (criteria.postalCode() != null && !criteria.postalCode().isBlank()) {
                predicates.add(cb.equal(root.get("contactPsc"), criteria.postalCode()));
            }

            if (criteria.fullName() != null && !criteria.fullName().isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("fullName")), "%" + criteria.fullName().toLowerCase() + "%"));
            }

            if (criteria.city() != null && !criteria.city().isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("contactCityName")), "%" + criteria.city().toLowerCase() + "%"));
            }

            if (criteria.idCardExpiryExact() != null) {
                predicates.add(cb.equal(root.get("idCardExpiryDate"), criteria.idCardExpiryExact()));
            }
            if (criteria.idCardExpiryBefore() != null) {
                predicates.add(cb.lessThan(root.get("idCardExpiryDate"), criteria.idCardExpiryBefore()));
            }
            if (criteria.idCardExpiryAfter() != null) {
                predicates.add(cb.greaterThan(root.get("idCardExpiryDate"), criteria.idCardExpiryAfter()));
            }

            if (criteria.lastUpdateBefore() != null) {
                predicates.add(cb.lessThan(root.get("lastUpdate"), criteria.lastUpdateBefore()));
            }

            if (criteria.minAge() != null) {
                LocalDate maxBirthDate = today.minusYears(criteria.minAge());
                predicates.add(cb.lessThanOrEqualTo(root.get("birthDate"), maxBirthDate));
            }

            if (criteria.maxAge() != null) {
                LocalDate minBirthDate = today.minusYears(criteria.maxAge() + 1);
                predicates.add(cb.greaterThan(root.get("birthDate"), minBirthDate));
            }

            if (criteria.birthdayInNextDays() != null) {
                LocalDate maxBirthday = today.plusDays(criteria.birthdayInNextDays());
                predicates.add(cb.between(root.get("nextBirthday"), today, maxBirthday));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}