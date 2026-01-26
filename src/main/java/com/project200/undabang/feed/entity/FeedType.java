package com.project200.undabang.feed.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Entity
@Table(name = "feed_types")
public class FeedType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "feed_type_id", nullable = false, updatable = false)
    private Long id;

    @Column(name = "feed_type_name", nullable = false)
    private String name;

    @Column(name = "feed_type_desc", nullable = false)
    private String desc;

    @Column(name = "feed_type_is_active", nullable = false)
    private Boolean isActive;

    @Builder.Default
    @Column(name = "feed_type_created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "feed_type_updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "feed_type_deleted_at")
    private LocalDateTime deletedAt;
}