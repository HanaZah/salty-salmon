package com.finadvise.crm.products;

import com.finadvise.crm.clients.Client;
import com.finadvise.crm.users.Advisor;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class ProductSpecifications {

    // Private constructor to prevent instantiation of utility class
    private ProductSpecifications() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    public static Specification<Product> withCriteria(ProductSearchCriteriaDTO criteria, String advisorEmployeeId) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            Join<Product, Client> clientJoin = root.join("client", JoinType.INNER);
            Join<Client, Advisor> clientAdvisorJoin = clientJoin.join("advisor", JoinType.INNER);

            if (Boolean.TRUE.equals(criteria.includeManagedProducts())) {
                // LEFT JOIN because managedBy is nullable
                Join<Product, Advisor> managedByJoin = root.join("managedBy", JoinType.LEFT);

                predicates.add(cb.or(
                        cb.equal(clientAdvisorJoin.get("employeeId"), advisorEmployeeId),
                        cb.equal(managedByJoin.get("employeeId"), advisorEmployeeId)
                ));
            } else {
                predicates.add(cb.equal(clientAdvisorJoin.get("employeeId"), advisorEmployeeId));
            }

            if (criteria.providerIds() != null && !criteria.providerIds().isEmpty()) {
                // We don't need a full join here because we are just comparing the Foreign Key ID
                predicates.add(root.get("provider").get("id").in(criteria.providerIds()));
            }

            if (criteria.productTypeIds() != null && !criteria.productTypeIds().isEmpty()) {
                predicates.add(root.get("productType").get("id").in(criteria.productTypeIds()));
            }

            if (criteria.anniversaryDateFrom() != null && criteria.anniversaryDateTo() != null) {
                predicates.add(cb.between(
                        root.get("nextAnniversary"),
                        criteria.anniversaryDateFrom(),
                        criteria.anniversaryDateTo()
                ));
            } else if (criteria.anniversaryDateFrom() != null) {
                predicates.add(cb.greaterThanOrEqualTo(
                        root.get("nextAnniversary"),
                        criteria.anniversaryDateFrom()
                ));
            } else if (criteria.anniversaryDateTo() != null) {
                predicates.add(cb.lessThanOrEqualTo(
                        root.get("nextAnniversary"),
                        criteria.anniversaryDateTo()
                ));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
