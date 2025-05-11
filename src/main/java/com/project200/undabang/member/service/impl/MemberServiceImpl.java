package com.project200.undabang.member.service.impl;

import com.project200.undabang.member.dto.request.SignUpRequestDto;
import com.project200.undabang.member.dto.response.SignUpResponseDto;
import com.project200.undabang.member.entity.Member;
import com.project200.undabang.member.enums.MemberGender;
import com.project200.undabang.member.exceptions.DuplicateMemberEmailException;
import com.project200.undabang.member.exceptions.DuplicateMemberNicknameException;
import com.project200.undabang.member.exceptions.InvalidMemberGenderException;
import com.project200.undabang.member.exceptions.MemberNotSavedException;
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
        if(checkMemberEmail(signUpRequestDto.getUserEmail())){
            throw new DuplicateMemberEmailException("이미 사용중인 이메일 입니다.");
        }
        if(checkMemberNickname(signUpRequestDto.getMemberNickname())){
            throw new DuplicateMemberNicknameException("이미 사용중인 닉네임 입니다.");
        }

        MemberGender memberGender;
        try{
            memberGender = MemberGender.valueOf(signUpRequestDto.getMemberGender());
        } catch (IllegalArgumentException e){
            throw new InvalidMemberGenderException("유효하지 않은 성별을 입력하였습니다");
        }

        Member member = new Member();
        member.setMemberId(signUpRequestDto.getUserId());
        member.setMemberEmail(signUpRequestDto.getUserEmail());
        member.setMemberNickname(signUpRequestDto.getMemberNickname());
        member.setMemberGender(memberGender);
        member.setMemberBday(signUpRequestDto.getMemberBday());

        Member savedMember = memberRepository.save(member);

        if(Objects.isNull(savedMember)){
            throw new MemberNotSavedException("회원가입 오류");
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
