package com.project200.undabang.chat.service.impl;

import com.project200.undabang.chat.dto.response.GetMemberChatroomResponse;
import com.project200.undabang.chat.repository.ChatroomMemberRepository;
import com.project200.undabang.chat.service.ChatQueryService;
import com.project200.undabang.common.context.UserContextHolder;
import com.project200.undabang.common.web.exception.CustomException;
import com.project200.undabang.common.web.exception.ErrorCode;
import com.project200.undabang.member.entity.Member;
import com.project200.undabang.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatQueryServiceImpl implements ChatQueryService {

    private final MemberRepository memberRepository;
    private final ChatroomMemberRepository chatroomMemberRepository;

    @Override
    public List<GetMemberChatroomResponse> getMemberChatroomList() {
        Member member = getMember(UserContextHolder.getUserId());

        return chatroomMemberRepository.getChatroomListByMemberId(member);
    }

    private Member getMember(UUID memberId) {
        return memberRepository.findById(memberId).orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));
    }

}
