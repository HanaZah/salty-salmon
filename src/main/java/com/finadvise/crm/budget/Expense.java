package com.finadvise.crm.budget;

import com.finadvise.crm.clients.Client;
import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "EXPENSES")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Expense {

    @Id
    @Column(name = "EXPENSE_ID")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "budget_item_gen")
    @SequenceGenerator(name = "budget_item_gen", sequenceName = "BUDGET_ITEM_SEQ", allocationSize = 1)
    private Long id;

    @Column(name = "AMOUNT", nullable = false)
    @Min(value = 1, message = "Amount must be at least 1")
    @Max(value = 999999999, message = "Amount cannot exceed 999,999,999")
    @NotNull(message = "Amount is required")
    private Integer amount;

    @Column(name = "IS_MANDATORY", nullable = false)
    @JdbcTypeCode(SqlTypes.INTEGER)
    @Builder.Default
    private boolean isMandatory = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CLIENT_ID", nullable = false, updatable = false)
    @NotNull(message = "Client is required")
    private Client client;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "EXPENSE_TYPE_ID", nullable = false)
    @Fetch(FetchMode.JOIN)
    @NotNull(message = "Expense type is required")
    private ExpenseType expenseType;

    @Version
    @Column(name = "VERSION", nullable = false)
    @Builder.Default
    private Integer version = 0;
}
