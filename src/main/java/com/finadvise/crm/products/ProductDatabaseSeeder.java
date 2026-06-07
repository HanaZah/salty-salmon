package com.finadvise.crm.products;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductDatabaseSeeder implements CommandLineRunner {

    private final ProductTypeRepository productTypeRepository;
    private final ProviderRepository providerRepository;

    @Override
    @Transactional
    public void run(String... args) {
        if (productTypeRepository.count() == 0) {
            List<String> defaultProductTypes = List.of(
                    "Pojištění životní",
                    "Pojištění neživotní",
                    "Povinné ručení",
                    "Úvěr spotřebitelský",
                    "Úvěr na bydlení",
                    "Investice",
                    "Spoření",
                    "Penzijní připojištění"
            );

            for (String typeName : defaultProductTypes) {
                productTypeRepository.save(ProductType.builder().name(typeName).build());
            }

            log.info("Seeded default ProductTypes into the database.");
        }

        if (providerRepository.count() == 0) {
            List<String> defaultProviders = List.of(
                    "Česká spořitelna",
                    "Komerční banka",
                    "ČSOB",
                    "Raiffeisenbank",
                    "Moneta Money Bank",
                    "Kooperativa",
                    "Generali Česká pojišťovna",
                    "Allianz",
                    "ČPP",
                    "Conseq",
                    "Amundi"
            );

            for (String providerName : defaultProviders) {
                providerRepository.save(Provider.builder().name(providerName).build());
            }

            log.info("Seeded default Providers into the database.");
        }
    }
}
