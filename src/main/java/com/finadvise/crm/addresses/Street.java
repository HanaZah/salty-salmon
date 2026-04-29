package com.finadvise.crm.addresses;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Entity
@Table(name = "STREETS")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Street {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "street_gen")
    @SequenceGenerator(name = "street_gen", sequenceName = "STREET_SEQ", allocationSize = 1)
    @Column(name = "STREET_ID")
    private Long id;

    @Column(nullable = false, length = 100, updatable = false)
    @NotBlank(message = "Street name is required")
    @Size(max = 100, message = "Street name cannot exceed 100 characters")
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CITY_ID", nullable = false, updatable = false)
    @NotNull(message = "City is required")
    private City city;
}
