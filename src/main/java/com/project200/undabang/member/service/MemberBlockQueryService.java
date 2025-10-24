package com.project200.undabang.member.service;

import com.project200.undabang.member.dto.response.GetBlockedMembersResponse;

import java.util.List;

public interface MemberBlockQueryService {
    List<GetBlockedMembersResponse> getBlockedMembers();
}
