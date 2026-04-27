package com.finadvise.crm.products;

import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class ProductMapper {

    public ProductDTO toDto(Product product) {
        if (product == null) return null;

        String employeeId = product.getManagedBy() != null ?
                product.getManagedBy().getEmployeeId() : null;

        return new ProductDTO(
                product.getId(),
                product.getName(),
                product.getAmount(),
                product.getStartDate(),
                product.getEndDate(),
                product.getProductType().getId(),
                product.getProductType().getName(),
                product.getProvider().getId(),
                product.getProvider().getName(),
                employeeId
        );
    }

    public boolean isActive(Product product) {
        return (product.getEndDate() == null) || !product.getEndDate().isBefore(LocalDate.now());
    }
}
