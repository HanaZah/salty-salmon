package com.finadvise.crm.clients;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;

@Entity
@Table(name = "V_CLIENT_SEARCH_MINIMAL")
@Immutable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class ClientSearchMinimal {

    @Id
    @Column(name = "CLIENT_UID", updatable = false)
    private String clientUid;

    @Column(name = "ADVISOR_EMPLOYEE_ID", updatable = false)
    private String advisorEmployeeId;

    @Column(name = "PERSONAL_ID", updatable = false)
    private String personalId;

    @Column(name = "FULL_NAME", updatable = false)
    private String fullName;

    @Column(name = "IS_ACTIVE", updatable = false)
    @JdbcTypeCode(SqlTypes.INTEGER)
    private boolean isActive;

    @Column(name = "BIRTH_DATE", updatable = false)
    private LocalDate birthDate;

    @Column(name = "NEXT_BIRTHDAY", updatable = false)
    private LocalDate nextBirthday;

    @Column(name = "ID_CARD_EXPIRY_DATE", updatable = false)
    private LocalDate idCardExpiryDate;

    @Column(name = "LAST_UPDATE", updatable = false)
    private LocalDate lastUpdate;

    @Column(name = "CONTACT_CITY_NAME", updatable = false)
    private String contactCityName;

    @Column(name = "CONTACT_PSC", updatable = false)
    private String contactPsc;
}