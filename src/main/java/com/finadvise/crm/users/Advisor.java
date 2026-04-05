package com.finadvise.crm.users;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "ADVISORS")
@PrimaryKeyJoinColumn(name = "USER_ID")
@DiscriminatorValue("ADVISOR")
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class Advisor extends User {

    @Column(nullable = false, unique = true, length = 8)
    private String ico;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MANAGER_ID")
    private Advisor manager;

    @Override
    public String getRole() {
        return "ADVISOR";
    }
}
