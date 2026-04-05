package com.finadvise.crm.addresses;

import jakarta.persistence.*;
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
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CITY_ID", nullable = false, updatable = false)
    private City city;
}
