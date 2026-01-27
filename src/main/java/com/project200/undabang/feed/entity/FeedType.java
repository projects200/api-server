package com.project200.undabang.feed.entity;

import jakarta.persistence.*;
import lombok.*;
import org.jetbrains.annotations.NotNull;

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
    private Long feedTypeId;

    @NotNull
    @Column(name = "feed_type_name", nullable = false)
    private String feedTypeName;

    @NotNull
    @Column(name = "feed_type_desc", nullable = false)
    private String feedTypeDesc;

    @NotNull
    @Column(name = "feed_type_is_active", nullable = false)
    private Boolean feedTypeIsActive;

    @NotNull
    @Builder.Default
    @Column(name = "feed_type_created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "feed_type_updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "feed_type_deleted_at")
    private LocalDateTime deletedAt;
}