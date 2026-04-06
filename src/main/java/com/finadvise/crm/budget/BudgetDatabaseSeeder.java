package com.finadvise.crm.budget;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class BudgetDatabaseSeeder implements CommandLineRunner {

    private final IncomeTypeRepository incomeTypeRepository;
    private final ExpenseTypeRepository expenseTypeRepository;

    @Override
    @Transactional
    public void run(String @NonNull ... args) {
        seedIncomeTypes();
        seedExpenseTypes();
    }

    private void seedIncomeTypes() {
        if (incomeTypeRepository.count() == 0) {
            log.info("Seeding Czech Income Types...");
            incomeTypeRepository.saveAll(List.of(
                    IncomeType.builder().name("Zaměstnání").isAssetLinked(false).build(),
                    IncomeType.builder().name("Podnikání / OSVČ").isAssetLinked(false).build(),
                    IncomeType.builder().name("Příjem z pronájmu").isAssetLinked(true).build(),
                    IncomeType.builder().name("Dividendy / Investice").isAssetLinked(true).build(),
                    IncomeType.builder().name("Důchod / Dávky").isAssetLinked(false).build()
            ));
        }
    }

    private void seedExpenseTypes() {
        if (expenseTypeRepository.count() == 0) {
            log.info("Seeding Czech Expense Types...");
            expenseTypeRepository.saveAll(List.of(
                    ExpenseType.builder().name("Bydlení a hypotéka").build(),
                    ExpenseType.builder().name("Energie a služby").build(),
                    ExpenseType.builder().name("Doprava").build(),
                    ExpenseType.builder().name("Pojištění").build(),
                    ExpenseType.builder().name("Potraviny a drogerie").build(),
                    ExpenseType.builder().name("Volný čas a hobby").build()
            ));
        }
    }
}
