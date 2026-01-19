package com.project200.undabang.member.dto.event;

import com.project200.undabang.member.entity.Member;

public record MemberBlockedEvent(Member blocked, Member blocker) {
    public static MemberBlockedEvent of(Member blocked, Member blocker) {
        return new MemberBlockedEvent(blocked, blocker);
    }
}
