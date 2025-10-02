package com.project200.undabang.openchat.entity;

import com.project200.undabang.member.entity.Member;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Entity
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(name = "open_chatrooms",
        uniqueConstraints = {
                @UniqueConstraint(name = "UK_active_member_id", columnNames = {"member_id", "member_id_unique_key"}),
                @UniqueConstraint(name = "UK_active_url", columnNames = {"open_chatroom_url", "open_chatroom_url_unique_key"})
        }
)
public class OpenChatRoom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "open_chatroom_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(name = "open_chatroom_url", length = 255)
    private String url;

    @Column(name = "open_chatroom_created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "open_chatroom_updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "open_chatroom_deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "member_id_unique_key", nullable = false)
    @Builder.Default
    private Long memberIdUniqueKey = 0L;

    @Column(name = "open_chatroom_url_unique_key", nullable = false)
    @Builder.Default
    private Long urlUniqueKey = 0L;

    /**
     * 주어진 멤버와 오픈 채팅방 URL을 기반으로 OpenChatRoom 객체를 생성합니다.
     */
    public static OpenChatRoom of(Member member, String openChatroomUrl) {
        return OpenChatRoom.builder()
                .member(member)
                .url(openChatroomUrl)
                .createdAt(LocalDateTime.now())
                .memberIdUniqueKey(0L)
                .urlUniqueKey(0L)
                .build();
    }

    /**
     * 현재 객체의 URL과 주어진 URL이 동일한지 확인합니다.
     */
    public boolean isSameUrl(String urlToCompare) {
        return this.url.equals(urlToCompare);
    }

    /**
     * 오픈 채팅방 URL을 업데이트하고, 업데이트 시간을 현재 시간으로 설정합니다.
     */
    public void updateOpenChatUrl(String openChatUrl) {
        this.url = openChatUrl;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 특정 오픈 채팅방을 삭제 처리합니다. 삭제 시 해당 오픈 채팅방의 삭제 시간을 현재 시간으로 설정하며,
     * 고유 멤버 아이디 키와 URL 고유 키를 주어진 ID 값으로 업데이트합니다.
     */
    public void softDelete() {
        this.deletedAt = LocalDateTime.now();
        this.memberIdUniqueKey = this.id;
        this.urlUniqueKey = this.id;
    }
}
