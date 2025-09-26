package com.project200.undabang.member.entity;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;
import org.locationtech.jts.geom.Point;

import java.time.LocalDateTime;

@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "exercise_locations")
public class ExerciseLocation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long exerciseLocationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Comment("운동장소 상호명, 없으면 직접 입력")
    @Column(nullable = false, length = 100)
    private String exerciseLocationName;

    @Comment("API 에서 반환하는 도로명주소")
    @Column(nullable = false, length = 255)
    private String exerciseLocationAddress;

    @Comment("단일 점 을 나타냄 (X,Y) 위도와 경도를 저장합니다")
    @Column(nullable = false)
    private Point exerciseLocationPoint;

    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime exerciseLocationCreatedAt = LocalDateTime.now();

    @Comment("운동 장소 수정 일시 기록")
    private LocalDateTime exerciseLocationUpdatedAt;

    @Comment("운동 장소 삭제 일시 기록")
    private LocalDateTime exerciseLocationDeletedAt;
}