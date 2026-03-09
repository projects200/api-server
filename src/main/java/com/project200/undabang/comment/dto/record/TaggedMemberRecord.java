package com.project200.undabang.comment.dto.record;

import java.util.UUID;

public record TaggedMemberRecord(
        UUID memberId,
        String memberNickname) {
}
