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
        if (documentTypeRepository.count() > 0) {
            return;
        }

        List<String> defaultTypes = List.of(
                "Doklad totožnosti",
                "Smlouva",
                "Výpis z účtu",
                "Daňové přiznání",
                "Záznam z jednání",
                "GDPR",
                "Ostatní"
        );

        for (String typeName : defaultTypes) {
            documentTypeRepository.save(DocumentType.builder().name(typeName).build());
        }
        log.info("Seeded default DocumentTypes into the database.");

    }
}
