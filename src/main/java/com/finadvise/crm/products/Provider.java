package com.finadvise.crm.products;

import jakarta.persistence.*;
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
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "PROVIDER_ID")
    private Long id;

    @Column(name = "NAME", nullable = false, unique = true, length = 100)
    private String name;
}
