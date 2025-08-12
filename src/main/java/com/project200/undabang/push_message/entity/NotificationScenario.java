package com.project200.undabang.push_message.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Comment;

import java.time.LocalDateTime;

@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Entity
@Table(name = "notification_scenarios")
public class NotificationScenario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("시나리오 ID")
    @Column(name = "scenario_id", nullable = false)
    private Long id;

    @Size(max = 50)
    @NotNull
    @Comment("시나리오 코드 (애플리케이션에서 사용)")
    @Column(name = "scenario_code", nullable = false, length = 50)
    private String scenarioCode;

    @Size(max = 255)
    @NotNull
    @Comment("시나리오 설명")
    @Column(name = "scenario_description", nullable = false)
    private String scenarioDescription;

    @Builder.Default
    @NotNull
    @Comment("시나리오 활성화 여부")
    @ColumnDefault("1")
    @Column(name = "scenario_is_enabled", nullable = false)
    private Boolean scenarioIsEnabled = true;

    @Builder.Default
    @NotNull
    @Comment("생성일시")
    @Column(name = "scenario_created_at", nullable = false, columnDefinition = "DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP")
    private LocalDateTime scenarioCreatedAt = LocalDateTime.now();

    @Builder.Default
    @NotNull
    @Comment("수정일시")
    @Column(name = "scenario_updated_at", nullable = false, columnDefinition = "DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP")
    private LocalDateTime scenarioUpdatedAt = LocalDateTime.now();

    @Comment("삭제일시")
    @Column(name = "scenario_deleted_at")
    private LocalDateTime scenarioDeletedAt;

}