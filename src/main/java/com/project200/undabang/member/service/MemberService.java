package com.project200.undabang.member.service;

import com.project200.undabang.member.dto.request.SignUpRequestDto;
import com.project200.undabang.member.dto.response.SignUpResponseDto;

import java.util.UUID;

public interface MemberService {
    boolean checkMemberEmail(String email);
    boolean checkMemberNickname(String nickname);
    boolean checkMemberId(UUID memberId);
    SignUpResponseDto memberSignUp(SignUpRequestDto signUpRequestDto);
}
