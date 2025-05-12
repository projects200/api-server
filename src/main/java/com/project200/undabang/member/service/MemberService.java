package com.project200.undabang.member.service;

import com.project200.undabang.member.dto.request.SignUpRequestDto;
import com.project200.undabang.member.dto.response.SignUpResponseDto;

public interface MemberService {
    public boolean checkMemberEmail(String email);
    public boolean checkMemberNickname(String nickname);
    public SignUpResponseDto memberSignUp(SignUpRequestDto signUpRequestDto);
}
