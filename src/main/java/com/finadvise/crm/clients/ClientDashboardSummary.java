package com.finadvise.crm.clients;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface ClientDashboardSummary {
    String getClientUid();
    String getFullName();
    Integer getActiveProductsCount();
    BigDecimal getTotalAssetsValue();
    BigDecimal getTotalIncome();
    BigDecimal getTotalExpense();
    LocalDate getLastUpdate();

    default BigDecimal getBudgetBalance() {
        BigDecimal income = getTotalIncome() != null ? getTotalIncome() : BigDecimal.ZERO;
        BigDecimal expense = getTotalExpense() != null ? getTotalExpense() : BigDecimal.ZERO;
        return income.subtract(expense);
    }
}
