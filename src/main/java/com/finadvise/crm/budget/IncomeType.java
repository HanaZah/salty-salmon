package com.finadvise.crm.budget;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "INCOME_TYPES")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class IncomeType {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "income_type_gen")
    @SequenceGenerator(name = "income_type_gen", sequenceName = "INCOME_TYPE_SEQ", allocationSize = 1)
    @Column(name = "INCOME_TYPE_ID")
    private Long id;

    @Column(name = "NAME", nullable = false, unique = true, length = 50)
    private String name;

    @Column(name = "IS_ASSET_LINKED", nullable = false)
    @JdbcTypeCode(SqlTypes.INTEGER)
    @Builder.Default
    private boolean isAssetLinked = false;
}
