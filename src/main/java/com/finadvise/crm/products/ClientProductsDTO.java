package com.finadvise.crm.products;

import java.util.List;

public record ClientProductsDTO(
        String clientUid,
        List<ProductDTO> products,
        Integer totalActive
) {}
