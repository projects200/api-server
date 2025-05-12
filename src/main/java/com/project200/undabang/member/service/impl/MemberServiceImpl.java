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

import java.util.Objects;

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
    public SignUpResponseDto completeMemberProfile(SignUpRequestDto signUpRequestDto){
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

        Member member = new Member();
        member.setMemberId(UserContextHolder.getUserId());
        member.setMemberEmail(UserContextHolder.getUserEmail());
        member.setMemberNickname(signUpRequestDto.getMemberNickname());
        member.setMemberGender(memberGender);
        member.setMemberScore((byte) 35);
        member.setMemberBday(signUpRequestDto.getMemberBday());

        Member savedMember = memberRepository.save(member);

        if(Objects.isNull(savedMember)){
            throw new CustomException(ErrorCode.MEMBER_NOT_FOUND);
        }

        return SignUpResponseDto.builder()
                .memberId(savedMember.getMemberId())
                .memberEmail(savedMember.getMemberEmail())
                .memberNickname(savedMember.getMemberNickname())
                .memberGender(savedMember.getMemberGender().getDescription())
                .memberBday(savedMember.getMemberBday())
                .memberDesc(savedMember.getMemberDesc())
                .memberScore(savedMember.getMemberScore())
                .memberCreatedAt(savedMember.getMemberCreatedAt())
                .build();
    }

}
