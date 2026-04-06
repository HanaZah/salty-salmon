package com.finadvise.crm.budget;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BudgetRepository extends JpaRepository<Income, Long> {
    @Query(value = """
        SELECT
            (SELECT NVL(SUM(AMOUNT), 0) FROM INCOMES WHERE CLIENT_ID = :clientId) as totalIncomes,
            (SELECT NVL(SUM(AMOUNT), 0) FROM EXPENSES WHERE CLIENT_ID = :clientId) as totalExpenses
        FROM DUAL
        """, nativeQuery = true)
    BudgetSummaryProjection getSummaryByClientId(@Param("clientId") Long clientId);
}
