package com.finadvise.crm.budget;

import com.finadvise.crm.clients.Client;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

@Entity
@Table(name = "INCOMES")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Income {

    @Id
    @Column(name = "INCOME_ID")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "budget_item_gen")
    @SequenceGenerator(name = "budget_item_gen", sequenceName = "BUDGET_ITEM_SEQ", allocationSize = 1)
    private Long id;

    @Column(name = "AMOUNT", nullable = false)
    private Integer amount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CLIENT_ID", nullable = false, updatable = false)
    private Client client;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "INCOME_TYPE_ID", nullable = false)
    @Fetch(FetchMode.JOIN)
    private IncomeType incomeType;

    @Version
    @Column(name = "VERSION", nullable = false)
    @Builder.Default
    private Integer version = 0;
}
