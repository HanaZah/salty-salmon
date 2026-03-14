package com.finadvise.crm.addresses;

import jakarta.persistence.*;
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

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false)
    private Integer psc;
}
