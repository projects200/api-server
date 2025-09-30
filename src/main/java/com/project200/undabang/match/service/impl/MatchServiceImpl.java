package com.project200.undabang.match.service.impl;

import com.project200.undabang.common.web.exception.CustomException;
import com.project200.undabang.common.web.exception.ErrorCode;
import com.project200.undabang.match.entity.Match;
import com.project200.undabang.match.repository.MatchRepository;
import com.project200.undabang.match.service.MatchService;
import com.project200.undabang.member.entity.Member;
import com.project200.undabang.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MatchServiceImpl implements MatchService {

    private final MemberRepository memberRepository;
    private final MatchRepository matchRepository;

    /**
     * 두 회원 간의 매칭 기록을 생성합니다.
     */
    @Override
    @Async("generalPurposeAsyncExecutor")
    @Transactional
    public void createMatchRecordBetweenMembers(UUID requesterId, UUID receiverId) {
        Member requester = getMember(requesterId);
        Member receiver = getMember(receiverId);

        Match match = Match.from(requester, receiver);

        matchRepository.save(match);
    }

    /**
     * 주어진 memberId에 해당하는 회원 정보를 반환합니다.
     */
    private Member getMember(UUID memberId) {
        return memberRepository.findById(memberId).orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));
    }
}
