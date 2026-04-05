package com.finadvise.crm.users;

import jakarta.persistence.*;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "USERS") // Forces Hibernate to self-join rather than look for an ADMINS table
@PrimaryKeyJoinColumn(name = "USER_ID")
@DiscriminatorValue("ADMIN")
@NoArgsConstructor
@SuperBuilder
public class Admin extends User {
    @Override
    public String getRole() {
        return "ADMIN";
    }
}
