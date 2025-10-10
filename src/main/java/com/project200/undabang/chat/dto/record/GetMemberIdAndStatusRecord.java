package com.project200.undabang.chat.dto.record;

import com.project200.undabang.chat.entity.ChatroomMemberStatus;

import java.util.UUID;

public record GetMemberIdAndStatusRecord(UUID memberId, ChatroomMemberStatus status) {
}
