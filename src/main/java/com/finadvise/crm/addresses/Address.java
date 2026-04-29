package com.finadvise.crm.addresses;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.*;

@Entity
@Table(name = "ADDRESSES")
@Getter
@Setter // Triggers will stop DB updates, but setters are needed for JPA/MapStruct
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Address {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "addr_gen")
    @SequenceGenerator(name = "addr_gen", sequenceName = "ADDR_SEQ", allocationSize = 1)
    @Column(name = "ADDRESS_ID", updatable = false)
    private Long id;

    @Column(name = "HOUSE_NUMBER", nullable = false, length = 10, updatable = false)
    @NotBlank(message = "House number is required")
    @Pattern(
            regexp = "^[1-9]\\d{0,3}(/[1-9]\\d{0,3}[a-z]?)?$",
            message = "Invalid Czech house number format (e.g., 1234 or 1234/15a)."
    )
    private String houseNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "STREET_ID", nullable = false, updatable = false)
    @NotNull(message = "Street is required")
    private Street street;
}
