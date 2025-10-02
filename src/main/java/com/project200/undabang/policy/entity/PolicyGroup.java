package com.project200.undabang.policy.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity @Table(name = "policy_groups")
@Builder@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class PolicyGroup {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "policy_groups_id")
    private Integer id;

    @Column(name = "policy_groups_name", nullable = false, unique = true, length = 100)
    private String name;

    @Column(name = "policy_groups_created_at", nullable = false, updatable = false)
    @NotNull
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "policy_groups_updated_at", nullable = false)
    @NotNull
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @Builder.Default
    @OneToMany(mappedBy = "policyGroup")
    private List<PolicyGroupMapping> policyMappings = new ArrayList<>();
}
