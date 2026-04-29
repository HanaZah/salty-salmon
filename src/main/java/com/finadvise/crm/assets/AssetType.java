package com.finadvise.crm.assets;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
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
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "asset_type_gen")
    @SequenceGenerator(name = "asset_type_gen", sequenceName = "ASSET_TYPE_SEQ", allocationSize = 1)
    @Column(name = "ASSET_TYPE_ID")
    private Long id;

    @Column(name = "NAME", nullable = false, unique = true, length = 50)
    @NotBlank(message = "Asset type name is required")
    @Size(max = 50, message = "Asset type name cannot exceed 50 characters")
    private String name;
}
