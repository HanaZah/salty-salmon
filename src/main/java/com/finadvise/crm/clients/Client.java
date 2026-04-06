package com.finadvise.crm.clients;

import com.finadvise.crm.addresses.Address;
import com.finadvise.crm.users.Advisor;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.domain.Persistable;

import java.time.LocalDate;

@Entity
@Table(name = "CLIENTS")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Client implements Persistable<Long> {

    @Id
    @Column(name = "CLIENT_ID")
    private Long id;

    @Column(name = "CLIENT_UID", nullable = false, unique = true, length = 8)
    private String clientUid;

    @Column(name = "PERSONAL_ID", nullable = false, unique = true, length = 10)
    private String personalId;

    @Column(name = "BIRTH_DATE", nullable = false)
    private LocalDate birthDate;

    @Column(name = "FIRST_NAME", nullable = false, length = 50)
    private String firstName;

    @Column(name = "LAST_NAME", nullable = false, length = 50)
    private String lastName;

    @Column(name = "PHONE", nullable = false, length = 20)
    private String phone;

    @Column(name = "EMAIL", length = 254)
    private String email;

    // --- ID CARD DETAILS ---
    @Column(name = "ID_CARD_NUMBER", nullable = false, unique = true)
    private String idCardNumber;

    @Column(name = "ID_CARD_ISSUE_DATE", nullable = false)
    private LocalDate idCardIssueDate;

    @Column(name = "ID_CARD_EXPIRY_DATE", nullable = false)
    private LocalDate idCardExpiryDate;

    @Column(name = "ID_CARD_ISSUER", nullable = false, length = 100)
    private String idCardIssuer;

    // --- AUDITING & CONCURRENCY ---
    @Column(name = "LAST_UPDATE", nullable = false)
    private LocalDate lastUpdate;

    @Version
    @Column(name = "VERSION", nullable = false)
    @Builder.Default
    private Integer version = 0;

    @Column(name = "IS_ACTIVE", nullable = false)
    @JdbcTypeCode(SqlTypes.INTEGER)
    @Builder.Default
    private boolean isActive = true;

    // --- RELATIONSHIPS ---
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ADVISOR_ID", nullable = false)
    private Advisor advisor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PERMANENT_ADDR_ID", nullable = false)
    private Address permanentAddress;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CONTACT_ADDR_ID", nullable = false)
    private Address contactAddress;

    // --- HIBERNATE LIFECYCLE CONTROL ---
    @Transient
    @Builder.Default
    private boolean isNewRecord = true;

    @Override
    public boolean isNew() {
        return isNewRecord;
    }

    @PostPersist
    @PostLoad
    protected void markNotNew() {
        this.isNewRecord = false;
    }

    // Ensures JPA has a value for LAST_UPDATE before the DB trigger fires
    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        this.lastUpdate = LocalDate.now();
    }
}
