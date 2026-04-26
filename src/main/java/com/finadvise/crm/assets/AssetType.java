package com.finadvise.crm.assets;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "ASSET_TYPES")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssetType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ASSET_TYPE_ID")
    private Long id;

    @Column(name = "NAME", nullable = false, unique = true, length = 50)
    private String name;
}
