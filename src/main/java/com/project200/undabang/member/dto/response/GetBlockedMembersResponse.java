package com.project200.undabang.member.dto.response;

import com.project200.undabang.member.dto.record.MemberBlockRecord;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GetBlockedMembersResponse {
    private long memberBlockId;
    private UUID memberId;
    private String nickname;
    private String profileImageUrl;
    private String thumbnailImageUrl;
    private LocalDateTime blockedAt;

    public static GetBlockedMembersResponse from(MemberBlockRecord record) {
        return GetBlockedMembersResponse.builder()
                .memberBlockId(record.memberBlockId())
                .memberId(record.memberId())
                .nickname(record.nickname())
                .profileImageUrl(record.profileImageUrl())
                .thumbnailImageUrl(record.thumbnailImageUrl())
                .blockedAt(record.blockedAt())
                .build();
    }
}
