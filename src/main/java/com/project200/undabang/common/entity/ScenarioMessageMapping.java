package com.project200.undabang.common.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.Comment;

@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Entity
@Table(name = "scenario_message_mappings")
public class ScenarioMessageMapping {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("시나리오 메시지 매핑 ID")
    @Column(name = "mapping_id", nullable = false)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @Comment("메시지 ID")
    @JoinColumn(name = "message_id", nullable = false)
    private NotificationMessage message;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @Comment("시나리오 ID")
    @JoinColumn(name = "scenario_id", nullable = false)
    private NotificationScenario scenario;

}