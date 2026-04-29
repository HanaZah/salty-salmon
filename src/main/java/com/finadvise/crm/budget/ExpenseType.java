package com.finadvise.crm.budget;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Entity
@Table(name = "EXPENSE_TYPES")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class ExpenseType {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "expense_type_gen")
    @SequenceGenerator(name = "expense_type_gen", sequenceName = "EXPENSE_TYPE_SEQ", allocationSize = 1)
    @Column(name = "EXPENSE_TYPE_ID")
    private Long id;

    @Column(name = "NAME", nullable = false, unique = true, length = 50)
    @NotBlank(message = "Expense type name cannot be blank")
    @Size(max = 50, message = "Expense type name cannot exceed 50 characters")
    private String name;
}
