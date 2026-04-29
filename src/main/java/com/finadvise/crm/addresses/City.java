package com.finadvise.crm.addresses;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

@Entity
@Table(name = "CITIES")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class City {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "city_gen")
    @SequenceGenerator(name = "city_gen", sequenceName = "CITY_SEQ", allocationSize = 1)
    @Column(name = "CITY_ID")
    private Long id;

    @Column(nullable = false, length = 100, updatable = false)
    @NotBlank(message = "City name is required")
    @Size(max = 100, message = "City name cannot exceed 100 characters")
    private String name;

    @Column(nullable = false, length = 6, updatable = false)
    @NotBlank(message = "Postal code (PSČ) is required")
    @Pattern(regexp = "^\\d{3}\\s\\d{2}$", message = "Format required: 123 45")
    private String psc;
}
