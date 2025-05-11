package com.project200.undabang.member.service.impl;

import com.project200.undabang.member.repository.MemberRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MemberServiceImplTest {
    @InjectMocks
    private MemberServiceImpl memberService;
    @Mock
    private MemberRepository memberRepository;


    @Test
    @DisplayName("회원 정보를 입력받아 회원 생성")
    public void createUserTest() throws Exception{

    }
}