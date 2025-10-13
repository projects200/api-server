package com.project200.undabang.chat.repository;

import com.project200.undabang.chat.dto.response.GetMemberChatroomResponse;
import com.project200.undabang.member.entity.Member;

import java.util.List;

public interface ChatroomMemberRepositoryCustom {
    List<GetMemberChatroomResponse> getChatroomListByMemberId(Member member);
}
