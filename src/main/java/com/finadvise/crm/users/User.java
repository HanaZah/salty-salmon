package com.finadvise.crm.users;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
    @NotNull(message = "User ID cannot be null")
    private Long id;

    @Column(name = "EMPLOYEE_ID", nullable = false, unique = true, length = 20)
    @NotBlank(message = "Employee ID cannot be blank")
    @Size(max = 20, message = "Employee ID cannot exceed 20 characters")
    private String employeeId;

    @Column(name = "PASSWORD_HASH", nullable = false, length = 255)
    @NotBlank(message = "Password hash cannot be blank")
    @Size(max = 255, message = "Password hash cannot exceed 255 characters")
    private String passwordHash;

    @Column(name = "FIRST_NAME", nullable = false, length = 50)
    @NotBlank(message = "First name cannot be blank")
    @Size(max = 50, message = "First name cannot exceed 50 characters")
    private String firstName;

    @Column(name = "LAST_NAME", nullable = false, length = 50)
    @NotBlank(message = "Last name cannot be blank")
    @Size(max = 50, message = "Last name cannot exceed 50 characters")
    private String lastName;

    @Column(name = "PHONE", nullable = false, length = 20)
    @NotBlank(message = "Phone number cannot be blank")
    @Size(max = 20, message = "Phone number cannot exceed 20 characters")
    private String phone;

    @Column(name = "EMAIL", nullable = false, length = 254)
    @NotBlank(message = "Email cannot be blank")
    @Size(max = 254, message = "Email cannot exceed 254 characters")
    @Email(message = "Invalid email format")
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
