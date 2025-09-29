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

    @OneToOne(fetch = FetchType.LAZY, optional = false)
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
                .build();
    }

    public void updateOpenChatUrl(String openChatUrl) {
        this.url = openChatUrl;
        this.updatedAt = LocalDateTime.now();
    }

    public boolean isSameUrl(String urlToCompare) {
        return this.url.equals(urlToCompare);
    }

    // Todo : 논리적 삭제 구현시 두개의 UNIQUE에 모두 PK 값을 넣어주어야 한다. 그래서 회원은 새로운 채팅방을 만들 수 있고, 다른 회원은 URL을 재사용 할 수 있음(혹시 재사용 하게 된다면)
}
