package com.finadvise.crm.products;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Entity
@Table(name = "PROVIDERS")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Provider {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "provider_gen")
    @SequenceGenerator(name = "provider_gen", sequenceName = "PROVIDER_SEQ", allocationSize = 1)
    @Column(name = "PROVIDER_ID")
    private Long id;

    @Column(name = "NAME", nullable = false, unique = true, length = 100)
    @NotBlank(message = "Provider name cannot be blank")
    private String name;
}
