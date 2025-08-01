package com.project200.undabang.policy.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Comment;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Entity
@Table(name = "policies")
public class Policy {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "policy_id")
    private Integer id;

    @Comment("정책을 식별하는 고유 값 (예: SCORE_INITIAL)")
    @Enumerated(EnumType.STRING)
    @Column(name = "policy_key", nullable = false, unique = true, length = 100)
    private PolicyKey policyKey;

    @Comment("정책 값")
    @Size(max = 255)
    @NotNull
    @Column(name = "policy_value", nullable = false)
    private String policyValue;

    @Comment("정책 값의 단위 (예: POINTS, DAYS)")
    @Size(max = 20)
    @Column(name = "policy_unit", length = 20)
    private String policyUnit;

    @Comment("관리자 페이지에 표시될 정책 설명")
    @Size(max = 500)
    @NotNull
    @Column(name = "policy_description", nullable = false, length = 500)
    private String policyDescription;

    @Comment("정책 생성 일시")
    @NotNull
    @Column(name = "policy_created_at", nullable = false)
    @Builder.Default
    private LocalDateTime policyCreatedAt = LocalDateTime.now();

    @Comment("마지막 수정 일시")
    @NotNull
    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "policy_updated_at", nullable = false)
    @Builder.Default
    private LocalDateTime policyUpdatedAt = LocalDateTime.now();

    @OneToMany(mappedBy = "policy")
    private List<PolicyGroupMapping> groupMappings = new ArrayList<>();
}