package com.finadvise.crm.assets;

import com.finadvise.crm.clients.Client;
import jakarta.persistence.*;
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
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ASSET_ID")
    private Long id;

    @Column(name = "NAME", nullable = false, length = 100)
    private String name;

    // The SQL schema limits this to 999,999,999
    @Column(name = "VALUE", nullable = false)
    private Integer value;

    @Column(name = "NOTE", length = 256)
    private String note;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ASSET_TYPE_ID", nullable = false)
    private AssetType assetType;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "CLIENT_ID", nullable = false)
    private Client client;
}