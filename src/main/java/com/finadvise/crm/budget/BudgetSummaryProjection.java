package com.finadvise.crm.budget;

import java.math.BigDecimal;

public interface BudgetSummaryProjection {
    BigDecimal getTotalIncomes();   // 50000
    BigDecimal getTotalExpenses();  // 35000

    default BigDecimal getNetCashflow() {
        // Now subtracting positive numbers
        return getTotalIncomes().subtract(getTotalExpenses()); // 15000
    }
}
