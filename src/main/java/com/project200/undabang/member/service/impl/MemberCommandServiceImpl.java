package com.project200.undabang.member.service.impl;

import com.project200.undabang.common.context.UserContextHolder;
import com.project200.undabang.common.web.exception.CustomException;
import com.project200.undabang.common.web.exception.ErrorCode;
import com.project200.undabang.member.dto.command.SignUpMemberCommand;
import com.project200.undabang.member.dto.event.MemberSignedUpEvent;
import com.project200.undabang.member.dto.request.SignUpRequestDto;
import com.project200.undabang.member.dto.response.SignUpResponseDto;
import com.project200.undabang.member.entity.Member;
import com.project200.undabang.member.repository.MemberRepository;
import com.project200.undabang.member.service.MemberCommandService;
import com.project200.undabang.policy.entity.PolicyKey;
import com.project200.undabang.policy.service.PolicyService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.UUID;

/**
 * 회원 관련 비즈니스 로직을 처리하는 서비스 구현체입니다.
 * 회원 가입, 회원 정보 검증 등의 기능을 제공합니다.
 */
@Service
@Validated
@Transactional
@RequiredArgsConstructor
public class MemberCommandServiceImpl implements MemberCommandService {

    private final MemberRepository memberRepository;
    private final PolicyService policyService;
    private final ApplicationEventPublisher eventPublisher; // EVENT를 발행하는 역할 수행.

    /**
     * 회원 가입 처리를 수행합니다.
     * 회원 ID, 이메일, 닉네임의 중복 여부를 확인하고, 정책에 따라 초기 가입 포인트를 설정하며,
     * 회원 엔티티를 데이터베이스에 저장한 후 회원 가입 이벤트를 발행합니다.
     *
     * @param signUpRequestDto 회원 가입 요청 정보를 담고 있는 객체
     * @return 회원 가입 처리 결과를 담은 응답 DTO
     * @throws CustomException 회원 ID, 이메일, 닉네임이 중복된 경우 예외를 발생시킵니다.
     */
    @Override
    public SignUpResponseDto memberSignUp(SignUpRequestDto signUpRequestDto){
        if(checkMemberId(UserContextHolder.getUserId())){
            throw new CustomException(ErrorCode.MEMBER_ID_DUPLICATED);
        }
        if(checkMemberEmail(UserContextHolder.getUserEmail())){
            throw new CustomException(ErrorCode.MEMBER_EMAIL_DUPLICATED);
        }
        if(checkMemberNickname(signUpRequestDto.getMemberNickname())){
            throw new CustomException(ErrorCode.MEMBER_NICKNAME_DUPLICATED);
        }

        byte initialSignupPoints = policyService.getPolicyValueAsByte(PolicyKey.SIGNUP_INITIAL_POINTS);

        Member member = Member.signUp(
                SignUpMemberCommand.builder()
                        .memberId(UserContextHolder.getUserId())
                        .memberEmail(UserContextHolder.getUserEmail())
                        .memberNickname(signUpRequestDto.getMemberNickname())
                        .memberGender(signUpRequestDto.getMemberGender())
                        .initialSignupPoints(initialSignupPoints)
                        .memberBday(signUpRequestDto.getMemberBday())
                        .build());

        memberRepository.save(member);

        // SimpleTimerCommandService 직접 호출 대신 이벤트 발행 (비동기로 구현됨에 따라 회원 객체가 DB에 저장되기 전 조회하는 경우 방지)
        // memberSignUp 트랜잭션이 성공적으로 커밋된 후에 처리될 리스너를 예약함
        eventPublisher.publishEvent(new MemberSignedUpEvent(member.getMemberId()));

        return SignUpResponseDto.of(member);
    }

    /**
     * 이메일 중복 여부를 확인합니다.
     */
    @Override
    public boolean checkMemberEmail(String email){
        return memberRepository.existsByMemberEmail(email);
    }

    /**
     * 닉네임 중복 여부를 확인합니다.
     */
    @Override
    public boolean checkMemberNickname(String nickname){
        return memberRepository.existsByMemberNickname(nickname);
    }

    /**
     * 회원 ID 존재 여부를 확인합니다.
     */
    @Override
    public boolean checkMemberId(UUID memberId) {
        return memberRepository.existsByMemberId(memberId);
    }

}
