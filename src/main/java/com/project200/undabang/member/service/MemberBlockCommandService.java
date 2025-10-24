package com.project200.undabang.member.service;

import com.project200.undabang.member.dto.response.CreateMemberBlockResponse;

import java.util.UUID;

public interface MemberBlockCommandService {
    CreateMemberBlockResponse createMemberBlock(UUID blockMemberId);

    void unBlockMember(UUID blockMemberId);
}
