package com.project200.undabang.member.service.impl;

import com.project200.undabang.common.context.UserContextHolder;
import com.project200.undabang.common.web.exception.CustomException;
import com.project200.undabang.common.web.exception.ErrorCode;
import com.project200.undabang.member.dto.record.MemberBlockRecord;
import com.project200.undabang.member.dto.response.GetBlockedMembersResponse;
import com.project200.undabang.member.entity.Member;
import com.project200.undabang.member.repository.MemberBlockRepository;
import com.project200.undabang.member.repository.MemberRepository;
import com.project200.undabang.member.service.MemberBlockQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberBlockQueryServiceImpl implements MemberBlockQueryService {
    private final MemberRepository memberRepository;
    private final MemberBlockRepository memberBlockRepository;

    /**
     * 사용자의 차단된 멤버 목록을 조회합니다.
     */
    @Override
    public List<GetBlockedMembersResponse> getBlockedMembers() {
        Member member = getMember(UserContextHolder.getUserId());

        List<MemberBlockRecord> record = memberBlockRepository.findAllMemberBlockRecordsByMember(member);

        return record.stream().map(GetBlockedMembersResponse::from).toList();
    }

    /**
     * 주어진 멤버 ID를 통해 멤버 정보를 조회합니다.
     * 멤버가 존재하지 않을 경우 예외를 발생시킵니다.
     */
    private Member getMember(UUID memberId) {
        return memberRepository.findById(memberId).orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));
    }
}
