package com.project200.undabang.member.service.impl;

import com.project200.undabang.common.context.UserContextHolder;
import com.project200.undabang.common.web.exception.CustomException;
import com.project200.undabang.common.web.exception.ErrorCode;
import com.project200.undabang.member.dto.response.CreateMemberBlockResponse;
import com.project200.undabang.member.entity.Member;
import com.project200.undabang.member.entity.MemberBlock;
import com.project200.undabang.member.repository.MemberBlockRepository;
import com.project200.undabang.member.repository.MemberRepository;
import com.project200.undabang.member.service.MemberBlockCommandService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MemberBlockCommandServiceImpl implements MemberBlockCommandService {
    private final MemberBlockRepository memberBlockRepository;
    private final MemberRepository memberRepository;

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

        Optional<MemberBlock> optionalMemberBlock = memberBlockRepository.findByBlockerAndBlocked(member, blockedMember);

        if (optionalMemberBlock.isPresent()) {
            return handleMemberBlockExist(optionalMemberBlock.get());
        }

        MemberBlock savedMemberBlock = memberBlockRepository.save(MemberBlock.of(member, blockedMember));
        return CreateMemberBlockResponse.of(savedMemberBlock.getId());
    }

    /**
     * 회원 차단 해제를 수행하는 메서드.
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
     * 주어진 회원 ID와 차단 대상 회원 ID가 동일한지 검증하는 메서드.
     * 동일할 경우 예외를 발생시킵니다.
     */
    private void validateSelfRequest(UUID memberId, UUID blockMemberId) {
        if (memberId.equals(blockMemberId)) {
            throw new CustomException(ErrorCode.MEMBER_SELF_REQUEST_NOT_ALLOWED);
        }
    }

    /**
     * 기존의 MemberBlock이 존재할 경우 해당 상태를 처리하고 응답을 반환하는 메서드.
     * 존재할 경우 409 Duplicated
     * 존재하지만, 삭제한 경우 deletedAt = null 업데이트
     */
    private CreateMemberBlockResponse handleMemberBlockExist(MemberBlock memberBlock) {

        if (memberBlock.getMemberBlockDeletedAt() == null) {
            throw new CustomException(ErrorCode.MEMBER_BLOCK_DUPLICATED);
        } else {
            memberBlock.reBlock();
            return CreateMemberBlockResponse.of(memberBlock.getId());
        }
    }

    /**
     * 주어진 회원 ID를 기반으로 회원 정보를 조회하는 메서드.
     * 만약 회원 정보가 존재하지 않을 경우 MEMBER_NOT_FOUND 예외를 발생시킴.
     */
    private Member getMember(UUID memberId) {
        return memberRepository.findById(memberId).orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));
    }
}
