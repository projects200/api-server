package com.project200.undabang.auth.service.impl;

import com.project200.undabang.auth.service.AuthService;
import com.project200.undabang.common.context.UserContextHolder;
import com.project200.undabang.common.web.exception.CustomException;
import com.project200.undabang.common.web.exception.ErrorCode;
import com.project200.undabang.member.entity.Member;
import com.project200.undabang.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final MemberRepository memberRepository;

    @Override
    public Member login() {
        return memberRepository.findByMemberIdAndMemberDeletedAtNull(UserContextHolder.getUserId())
                .orElseThrow(() -> new CustomException(ErrorCode.LOGIN_FAILED));
    }

    @Override
    public Member logout() {
        return memberRepository.findByMemberIdAndMemberDeletedAtNull(UserContextHolder.getUserId())
                .orElseThrow(() -> new CustomException(ErrorCode.LOGOUT_FAILED));
    }
}
