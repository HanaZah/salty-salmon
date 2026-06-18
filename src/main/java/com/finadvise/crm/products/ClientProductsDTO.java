package com.finadvise.crm.products;

import org.springframework.data.domain.Page;

public record ClientProductsDTO(
        String clientUid,
        Page<ProductDTO> products,
        Integer totalActive
) {}
