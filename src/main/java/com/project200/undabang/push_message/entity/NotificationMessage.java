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
@Table(name = "notification_messages")
public class NotificationMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("메시지 ID")
    @Column(name = "message_id", nullable = false)
    private Long id;

    @Size(max = 100)
    @Comment("알림 제목")
    @Column(name = "message_title", length = 100)
    private String messageTitle;

    @Size(max = 1000)
    @NotNull
    @Comment("알림 본문")
    @Column(name = "message_body", nullable = false, length = 1000)
    private String messageBody;

    @Size(max = 255)
    @Comment("알림 이미지 URL")
    @Column(name = "message_image_url")
    private String messageImageUrl;

    @Size(max = 255)
    @Comment("알림 클릭 시 이동할 URL")
    @Column(name = "message_link_url")
    private String messageLinkUrl;

    @Builder.Default
    @NotNull
    @Comment("생성일시")
    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "message_created_at", nullable = false)
    private LocalDateTime messageCreatedAt = LocalDateTime.now();

    @Builder.Default
    @NotNull
    @Comment("수정일시")
    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "message_updated_at", nullable = false)
    private LocalDateTime messageUpdatedAt = LocalDateTime.now();

    @Comment("삭제일시")
    @Column(name = "message_deleted_at")
    private LocalDateTime messageDeletedAt;

}