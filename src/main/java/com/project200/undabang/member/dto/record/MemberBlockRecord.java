package com.project200.undabang.member.dto.record;

import java.time.LocalDateTime;
import java.util.UUID;

public record MemberBlockRecord(Long memberBlockId, UUID memberId, String nickname, String profileImageUrl,
                                String thumbnailImageUrl, LocalDateTime blockedAt) {
}
