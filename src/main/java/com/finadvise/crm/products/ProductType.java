package com.finadvise.crm.products;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Entity
@Table(name = "PRODUCT_TYPES")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductType {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "product_type_gen")
    @SequenceGenerator(name = "product_type_gen", sequenceName = "PRODUCT_TYPE_SEQ", allocationSize = 1)
    @Column(name = "PRODUCT_TYPE_ID")
    private Long id;

    @Column(name = "NAME", nullable = false, unique = true, length = 50)
    @NotBlank(message = "Product type name cannot be blank")
    private String name;
}
