package com.finadvise.crm.assets;

import com.finadvise.crm.clients.Client;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

@Entity
@Table(name = "ASSETS")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Asset {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "asset_gen")
    @SequenceGenerator(name = "asset_gen", sequenceName = "ASSET_SEQ", allocationSize = 1)
    @Column(name = "ASSET_ID")
    private Long id;

    @Column(name = "NAME", nullable = false, length = 100)
    @NotBlank(message = "Asset name is required")
    @Size(max = 100, message = "Asset name cannot exceed 100 characters")
    private String name;

    // The SQL schema limits this to 999,999,999
    @Column(name = "VALUE", nullable = false)
    @Min(value = 1, message = "Asset value must be at least 1")
    @Max(value = 999999999, message = "Asset value cannot exceed 999,999,999")
    @NotNull(message = "Asset value is required")
    private Integer value;

    @Column(name = "NOTE", length = 256)
    @Size(max = 256, message = "Note cannot exceed 256 characters")
    private String note;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ASSET_TYPE_ID", nullable = false)
    @NotNull(message = "Asset type is required")
    private AssetType assetType;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "CLIENT_ID", nullable = false)
    @NotNull(message = "Client is required")
    private Client client;
}