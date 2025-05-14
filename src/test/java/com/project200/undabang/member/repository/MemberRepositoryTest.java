package com.project200.undabang.member.repository;

import com.project200.undabang.member.entity.Member;
import com.project200.undabang.member.enums.MemberGender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
class MemberRepositoryTest {

    @Autowired
    MemberRepository memberRepository;

    private UUID testUUID;
    @BeforeEach
    void setUp(){
        testUUID = UUID.randomUUID();

        Member member = Member.builder()
                .memberId(testUUID)
                .memberEmail("user@email.com")
                .memberNickname("유저닉네임")
                .memberGender(MemberGender.M)
                .memberBday(LocalDate.of(1990, 1, 1))
                .memberDesc("테스트 회원입니다ㄲ.")
                .memberScore((byte) 35)
                .memberCreatedAt(LocalDateTime.now())
                .memberWarnedCount((byte)0)
                .memberDeletedAt(null)
                .build();

        memberRepository.save(member);
    }

    @AfterEach
    void tearDown(){
        memberRepository.deleteAll();
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

    /**
     * 회원의 ID가 데이터베이스에 존재하는 경우를 테스트합니다.
     */
    @Test
    @DisplayName("회원 ID가 존재하는 경우")
    void existsByMemberId_exists() {
        // given
        UUID existingMemberId = testUUID;

        // when
        boolean result = memberRepository.existsByMemberId(existingMemberId);

        // then
        assertThat(result).isTrue();
    }

    /**
     * 회원의 ID가 데이터베이스에 존재하지 않는 경우를 테스트합니다.
     */
    @Test
    @DisplayName("회원 ID가 존재하지 않는 경우")
    void existsByMemberId_not_exists() {
        // given
        UUID nonExistingMemberId = UUID.randomUUID();

        // when
        boolean result = memberRepository.existsByMemberId(nonExistingMemberId);

        // then
        assertThat(result).isFalse();
    }


}