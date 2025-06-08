package com.project200.undabang.member.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Entity
@Table(name = "member_locations")
public class MemberLocation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_location_id", nullable = false, updatable = false)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false, updatable = false)
    private Member member;

    @Size(max = 255)
    @NotNull
    @org.hibernate.annotations.Comment("사용자 지정 명칭")
    @Column(name = "member_location_title", nullable = false)
    private String memberLocationTitle;

    @Size(max = 30)
    @NotNull
    @org.hibernate.annotations.Comment("위도(18자)")
    @Column(name = "member_location_latitude", nullable = false, length = 30)
    private String memberLocationLatitude;

    @Size(max = 30)
    @NotNull
    @org.hibernate.annotations.Comment("경도(18자)")
    @Column(name = "member_location_longitude", nullable = false, length = 30)
    private String memberLocationLongitude;

    @Size(max = 255)
    @NotNull
    @org.hibernate.annotations.Comment("주소(34자)")
    @Column(name = "member_location_address", nullable = false)
    private String memberLocationAddress;

    @NotNull
    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "member_location_created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime memberLocationCreatedAt = LocalDateTime.now();

    @Column(name = "member_location_deleted_at")
    private LocalDateTime memberLocationDeletedAt;

}