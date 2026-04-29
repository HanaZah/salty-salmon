package com.finadvise.crm.users;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
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
    @NotBlank(message = "ICO is required")
    @Pattern(regexp = "^\\d{8}$", message = "ICO must be exactly 8 digits")
    private String ico;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MANAGER_ID")
    private Advisor manager;

    @Override
    public String getRole() {
        return "ADVISOR";
    }
}
