package com.finadvise.crm.products;

import com.finadvise.crm.clients.Client;
import com.finadvise.crm.users.Advisor;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "PRODUCTS")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "product_gen")
    @SequenceGenerator(name = "product_gen", sequenceName = "PRODUCT_SEQ", allocationSize = 1)
    @Column(name = "PRODUCT_ID")
    private Long id;

    @Column(name = "NAME", nullable = false, length = 150)
    @NotBlank(message = "Product name cannot be blank")
    @Size(max = 150, message = "Product name cannot exceed 150 characters")
    private String name;

    @Column(name = "AMOUNT", nullable = false, precision = 10, scale = 2)
    @DecimalMin(value = "0.00", message = "Amount cannot be negative")
    @DecimalMax(value = "99999999.99", message = "Amount exceeds maximum limit")
    @NotNull(message = "Amount is required")
    private BigDecimal amount;

    @Column(name = "START_DATE", nullable = false)
    @NotNull(message = "Start date is required")
    private LocalDate startDate;

    @Column(name = "END_DATE")
    private LocalDate endDate;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "PRODUCT_TYPE_ID", nullable = false)
    @NotNull(message = "Product type is required")
    private ProductType productType;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "CLIENT_ID", nullable = false, updatable = false)
    @NotNull(message = "Client is required")
    private Client client;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "PROVIDER_ID", nullable = false)
    @NotNull(message = "Provider is required")
    private Provider provider;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MANAGED_BY_ID", updatable = false)
    private Advisor managedBy;

    @Column(name = "NEXT_ANNIVERSARY", nullable = false)
    @NotNull(message = "Next anniversary is required")
    private LocalDate nextAnniversary;
}
