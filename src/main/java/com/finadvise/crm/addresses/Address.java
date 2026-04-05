package com.finadvise.crm.addresses;

import jakarta.persistence.*;
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

    @Column(name = "HOUSE_NUMBER", nullable = false, updatable = false)
    private String houseNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "STREET_ID", nullable = false, updatable = false)
    private Street street;
}
