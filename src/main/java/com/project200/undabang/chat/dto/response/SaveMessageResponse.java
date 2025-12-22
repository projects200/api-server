package com.project200.undabang.chat.dto.response;

import com.project200.undabang.chat.entity.Chat;
import com.project200.undabang.chat.entity.ChatType;
import com.project200.undabang.member.entity.Member;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor
public class SaveMessageResponse {
    Long chatId;
    UUID senderId;
    String senderNickname;
    String senderProfileUrl;
    String senderThumbnailUrl;
    String chatContent;
    ChatType chatType;
    LocalDateTime sentAt;

    public static SaveMessageResponse from(Member member, Chat chat) {
        String thumbnailUrl = null;
        String profileUrl = null;

        // 프로필 사진이 없을 경우의 null 체크
        if (member.getMemberPicture() != null) {
            thumbnailUrl = member.getMemberPicture().getMemberPicturesUrl();

            if (member.getMemberPicture().getPicture() != null) {
                profileUrl = member.getMemberPicture().getPicture().getPictureUrl();
            }
        }

        return SaveMessageResponse.builder()
                .chatId(chat.getId())
                .senderId(member.getMemberId())
                .senderNickname(member.getMemberNickname())
                .senderProfileUrl(profileUrl)
                .senderThumbnailUrl(thumbnailUrl)
                .chatContent(chat.getChatContent())
                .chatType(chat.getChatType())
                .sentAt(chat.getChatCreatedAt())
                .build();
    }
}
