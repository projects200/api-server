package com.project200.undabang.member.dto.response;

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
}
