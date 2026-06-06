package com.finadvise.crm.documents;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentTypeDatabaseSeeder implements CommandLineRunner {

    private final DocumentTypeRepository documentTypeRepository;

    @Override
    @Transactional
    public void run(String @NonNull ... args) {
        List<String> defaultTypes = List.of(
                "Doklad totožnosti",
                "Smlouva",
                "Výpis z účtu",
                "Daňové přiznání",
                "Záznam z jednání",
                "GDPR",
                "Ostatní"
        );

        int seededCount = 0;
        for (String typeName : defaultTypes) {
            if (documentTypeRepository.findByName(typeName).isEmpty()) {
                documentTypeRepository.save(DocumentType.builder().name(typeName).build());
                seededCount++;
            }
        }

        if (seededCount > 0) {
            log.info("Seeded {} new DocumentTypes into the database.", seededCount);
        }
    }
}
