package com.project200.undabang.member.repository;

import com.project200.undabang.member.entity.Member;
import com.project200.undabang.member.enums.MemberGender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
class MemberRepositoryTest {
    @Autowired
    MemberRepository memberRepository;
    @BeforeEach
    void setUp(){
        Member member = new Member();
        member.setMemberId("user1");
        member.setMemberEmail("user@email.com");
        member.setMemberNickname("유저닉네임");
        member.setMemberGender(MemberGender.valueOf("M"));
        member.setMemberBday(LocalDate.of(1990, 1, 1));
        member.setMemberDesc("테스트 회원입니다.");
        member.setMemberScore((byte) 0);
        member.setMemberCreatedAt(LocalDateTime.now());

        memberRepository.save(member);
    }

    @Test
    @DisplayName("이메일이 이미 존재하는 회원의 경우")
    void existsByEmail_exists(){
        String email = "user@email.com";
        boolean check = memberRepository.existsByMemberEmail(email);

        assertTrue(check);
    }

    @Test
    @DisplayName("이메일이 존재하지 않는 회원의 경우")
    void existsByEmail_not_exists(){
        String email = "user@gmail.com";
        boolean check = memberRepository.existsByMemberEmail(email);

        assertFalse(check);
    }

    @Test
    @DisplayName("이미 존재하는 닉네임을 입력한 회원의 경우")
    void existsByMemberNickname_exists(){
        String name = "유저닉네임";
        boolean check = memberRepository.existsByMemberNickname(name);
        assertTrue(check);
    }

    @Test
    @DisplayName("닉네임을 중복없이 입력한 회원의 경우")
    void existsByMemberNickname_not_exists(){
        String name = "테스트유저닉네임";
        boolean check = memberRepository.existsByMemberNickname(name);
        assertFalse(check);
    }
}