package com.project200.undabang.member.service.impl;

import com.project200.undabang.common.context.UserContextHolder;
import com.project200.undabang.common.web.exception.CustomException;
import com.project200.undabang.common.web.exception.ErrorCode;
import com.project200.undabang.member.dto.event.MemberBlockedEvent;
import com.project200.undabang.member.dto.response.CreateMemberBlockResponse;
import com.project200.undabang.member.entity.Member;
import com.project200.undabang.member.entity.MemberBlock;
import com.project200.undabang.member.repository.MemberBlockRepository;
import com.project200.undabang.member.repository.MemberRepository;
import com.project200.undabang.member.service.MemberBlockCommandService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MemberBlockCommandServiceImpl implements MemberBlockCommandService {
    private final MemberBlockRepository memberBlockRepository;
    private final MemberRepository memberRepository;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 회원 차단(Block) 요청을 생성하는 메서드.
     * 현재 사용자와 차단할 사용자가 동일한 경우 예외를 발생시키며,
     * 기존 차단 정보가 존재하는 경우 이를 처리하거나 새로운 차단 정보를 저장합니다.
     */
    @Override
    @Transactional
    public CreateMemberBlockResponse createMemberBlock(UUID blockMemberId) {
        UUID memberId = UserContextHolder.getUserId();
        validateSelfRequest(memberId, blockMemberId);

        Member member = getMember(memberId);
        Member blockedMember = getMember(blockMemberId);

        MemberBlock savedMemberBlock = validateAndSaveMemberBlock(member, blockedMember);
        eventPublisher.publishEvent(MemberBlockedEvent.of(blockedMember, member));

        return CreateMemberBlockResponse.of(savedMemberBlock.getId());
    }

    /**
     * 회원 차단 해제를 수행하는 메소드.
     * 현재 로그인한 회원과 차단 해제를 요청한 회원 간의 관계를 확인하고,
     * 유효한 차단 정보를 찾아 해당 차단을 해제합니다.
     */
    @Override
    @Transactional
    public void unBlockMember(UUID blockMemberId) {
        UUID memberId = UserContextHolder.getUserId();
        validateSelfRequest(memberId, blockMemberId);

        Member member = getMember(memberId);
        Member blockedMember = getMember(blockMemberId);

        MemberBlock memberBlock = memberBlockRepository.findByBlockerAndBlockedAndMemberBlockDeletedAtNull(member, blockedMember).orElseThrow(
                () -> new CustomException(ErrorCode.MEMBER_BLOCK_NOT_FOUND)
        );

        memberBlock.unBlock();
    }

    /**
     * 주어진 회원 ID와 차단 대상 회원 ID가 동일한지 검증하는 메소드.
     * 동일할 경우 예외를 발생시킵니다.
     */
    private void validateSelfRequest(UUID memberId, UUID blockMemberId) {
        if (memberId.equals(blockMemberId)) {
            throw new CustomException(ErrorCode.MEMBER_SELF_REQUEST_NOT_ALLOWED);
        }
    }

    /**
     * 주어진 MemberBlock 객체가 이미 존재하는지 확인하고, 기존 차단 상태를 처리하거나 재활성화하는 메서드.
     */
    private MemberBlock handleMemberBlockExist(MemberBlock memberBlock) {
        if (memberBlock.getMemberBlockDeletedAt() == null) {
            throw new CustomException(ErrorCode.MEMBER_BLOCK_DUPLICATED);
        }

        // 차단 해제 상태 였다면 다시 활성화해줌
        memberBlock.reBlock();
        return memberBlock;
    }

    /**
     * 회원 차단 정보를 검증하고 저장하는 메서드. 차단 정보가 이미 존재하는 경우 이를 처리하거나, 새롭게 차단 정보를 생성하여 저장합니다.
     */
    private MemberBlock validateAndSaveMemberBlock(Member member, Member blockedMember) {
        return memberBlockRepository.findByBlockerAndBlocked(member, blockedMember)
                .map(this::handleMemberBlockExist) // 기존 차단 정보가 있을 경우, 활성 차단이면 예외를 반환하고, 해제된 차단이면 재활성화
                .orElseGet(() -> memberBlockRepository.save(MemberBlock.of(member, blockedMember)));
    }

    /**
     * 주어진 회원 ID를 기반으로 회원 정보를 조회하는 메서드.
     * 만약 회원 정보가 존재하지 않을 경우 MEMBER_NOT_FOUND 예외를 발생시킴.
     */
    private Member getMember(UUID memberId) {
        return memberRepository.findById(memberId).orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));
    }
}
