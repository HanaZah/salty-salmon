package com.finadvise.crm.users;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.domain.Persistable;

@Entity
@Table(name = "USERS")
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "USER_TYPE", discriminatorType = DiscriminatorType.STRING)
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public abstract class User implements Persistable<Long> {

    @Id
    @Column(name = "USER_ID", nullable = false, unique = true)
    private Long id;

    @Column(name = "EMPLOYEE_ID", nullable = false, unique = true, length = 20)
    private String employeeId;

    @Column(name = "PASSWORD_HASH", nullable = false)
    private String passwordHash;

    @Column(name = "FIRST_NAME", nullable = false, length = 50)
    private String firstName;

    @Column(name = "LAST_NAME", nullable = false, length = 50)
    private String lastName;

    @Column(name = "PHONE", nullable = false, length = 20)
    private String phone;

    @Column(name = "EMAIL", nullable = false, length = 254)
    private String email;

    @Version
    @Column(name = "VERSION", nullable = false)
    private Integer version;

    @Column(name = "IS_ACTIVE", nullable = false)
    @JdbcTypeCode(SqlTypes.INTEGER)
    @Builder.Default
    private boolean isActive = true;

    @Transient // Tells Hibernate NOT to create a database column for this
    @Builder.Default
    private boolean isNewRecord = true;

    public abstract String getRole();

    @Override
    public boolean isNew() {
        return isNewRecord;
    }

    @PostPersist
    @PostLoad
    protected void markNotNew() {
        this.isNewRecord = false;
    }
}
