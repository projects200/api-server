package com.project200.undabang.member.service.impl;

import com.project200.undabang.common.context.UserContextHolder;
import com.project200.undabang.common.web.exception.CustomException;
import com.project200.undabang.common.web.exception.ErrorCode;
import com.project200.undabang.member.dto.request.SignUpRequestDto;
import com.project200.undabang.member.dto.response.SignUpResponseDto;
import com.project200.undabang.member.entity.Member;
import com.project200.undabang.member.enums.MemberGender;
import com.project200.undabang.member.repository.MemberRepository;
import com.project200.undabang.member.service.MemberService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Service
@Validated
@Transactional
@RequiredArgsConstructor
public class MemberServiceImpl implements MemberService {
    private final MemberRepository memberRepository;

    @Override
    public boolean checkMemberEmail(String email){
        return memberRepository.existsByMemberEmail(email);
    }

    @Override
    public boolean checkMemberNickname(String nickname){
        return memberRepository.existsByMemberNickname(nickname);
    }

    @Override
    public boolean checkMemberId(UUID memberId) {
        return memberRepository.existsByMemberId(memberId);
    }

    // 이름 변경
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

        MemberGender memberGender;
        try{
            memberGender = signUpRequestDto.getMemberGender();
        } catch (IllegalArgumentException e){
            throw new CustomException(ErrorCode.MEMBER_GENDER_ERROR);
        }

        Member member = Member.builder()
                .memberId(UserContextHolder.getUserId())
                .memberEmail(UserContextHolder.getUserEmail())
                .memberNickname(signUpRequestDto.getMemberNickname())
                .memberGender(memberGender)
                .memberWarnedCount((byte) 0)
                .memberCreatedAt(LocalDateTime.now())
                .memberScore((byte) 35)
                .memberBday(signUpRequestDto.getMemberBday())
                .build();

        Member savedMember = memberRepository.save(member);

        if(Objects.isNull(savedMember)){
            throw new CustomException(ErrorCode.MEMBER_NOT_FOUND);
        }

        return SignUpResponseDto.builder()
                .memberId(savedMember.getMemberId())
                .memberEmail(savedMember.getMemberEmail())
                .memberNickname(savedMember.getMemberNickname())
                .memberGender(savedMember.getMemberGender().getCode())
                .memberBday(savedMember.getMemberBday())
                .memberDesc(savedMember.getMemberDesc())
                .memberScore(savedMember.getMemberScore())
                .memberCreatedAt(savedMember.getMemberCreatedAt())
                .build();
    }

}
