package com.project200.undabang.member.service.impl;

import com.project200.undabang.common.context.UserContextHolder;
import com.project200.undabang.common.web.exception.CustomException;
import com.project200.undabang.common.web.exception.ErrorCode;
import com.project200.undabang.member.dto.request.SignUpRequestDto;
import com.project200.undabang.member.dto.response.SignUpResponseDto;
import com.project200.undabang.member.entity.Member;
import com.project200.undabang.member.repository.MemberRepository;
import com.project200.undabang.member.service.MemberService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDate;
import java.util.UUID;

/**
 * 회원 관련 비즈니스 로직을 처리하는 서비스 구현체입니다.
 * 회원 가입, 회원 정보 검증 등의 기능을 제공합니다.
 */
@Service
@Validated
@Transactional
@RequiredArgsConstructor
public class MemberServiceImpl implements MemberService {
    private final MemberRepository memberRepository;

    /**
     * 회원 가입을 처리합니다.
     * 아이디, 이메일, 닉네임 중복 체크 및 생년월일, 성별 유효성 검증 후
     * 회원 정보를 저장하고 응답 DTO를 반환합니다.
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
        if(checkMemberBday(signUpRequestDto.getMemberBday())){
            throw new CustomException((ErrorCode.MEMBER_BDAY_ERROR));
        }
        if(signUpRequestDto.getMemberGender() == null){
            throw new IllegalArgumentException("성별을 입력해주세요");
        }

        Member member = Member.createFromSignUp(
                UserContextHolder.getUserId(),
                UserContextHolder.getUserEmail(),
                signUpRequestDto.getMemberNickname(),
                signUpRequestDto.getMemberGender(),
                signUpRequestDto.getMemberBday()
        );

        /**
         * 저장 실패시 에러처리
         */
        Member savedMember = memberRepository.save(member);
        return SignUpResponseDto.of(savedMember);
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

    /**
     * 생년월일이 유효한지 확인합니다.
     * 현재 날짜보다 미래인 경우 유효하지 않음(true 반환)
     */
    @Override
    public boolean checkMemberBday(LocalDate memberBday){
        return !memberBday.isBefore(LocalDate.now());
    }


}
