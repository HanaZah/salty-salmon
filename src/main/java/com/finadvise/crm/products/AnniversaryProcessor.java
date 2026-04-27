package com.finadvise.crm.products;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnniversaryProcessor {

    private final ProductRepository productRepository;

    @Transactional
    public void processChunk(List<Product> chunk) {
        for (Product product : chunk) {
            LocalDate newAnniversary = product.getNextAnniversary().plusYears(1);
            product.setNextAnniversary(newAnniversary);
        }

        productRepository.saveAll(chunk);
    }
}
