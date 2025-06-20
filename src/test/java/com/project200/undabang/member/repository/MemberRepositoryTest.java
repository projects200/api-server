package com.project200.undabang.member.repository;

import com.project200.undabang.configuration.TestQuerydslConfig;
import com.project200.undabang.member.entity.Member;
import com.project200.undabang.member.enums.MemberGender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@Import(TestQuerydslConfig.class)
class MemberRepositoryTest {

    @Autowired
    MemberRepository memberRepository;

    private UUID testUUID;

    @BeforeEach
    void setUp() {
        testUUID = UUID.randomUUID();
        Member member = Member.createFromSignUp(
                testUUID,
                "user@email.com",
                "유저닉네임",
                MemberGender.M,
                LocalDate.of(1990, 1, 1)
        );

        memberRepository.save(member);
    }

    @Test
    @DisplayName("이메일이 이미 존재하는 회원의 경우")
    void existsByEmail_exists() {
        String email = "user@email.com";
        boolean check = memberRepository.existsByMemberEmail(email);

        assertTrue(check);
    }

    @Test
    @DisplayName("이메일이 존재하지 않는 회원의 경우")
    void existsByEmail_not_exists() {
        String email = "user@gmail.com";
        boolean check = memberRepository.existsByMemberEmail(email);

        assertFalse(check);
    }

    @Test
    @DisplayName("이미 존재하는 닉네임을 입력한 회원의 경우")
    void existsByMemberNickname_exists() {
        String name = "유저닉네임";
        boolean check = memberRepository.existsByMemberNickname(name);
        assertTrue(check);
    }

    @Test
    @DisplayName("닉네임을 중복없이 입력한 회원의 경우")
    void existsByMemberNickname_not_exists() {
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

    @Test
    @DisplayName("회원의 운동점수 조회")
    void findMemberScore_Success() {
        // given
        UUID memberId = testUUID;

        // when
        Optional<Member> foundMember = memberRepository.findByMemberIdAndMemberDeletedAtNull(memberId);

        // then
        assertThat(foundMember).isPresent();
        assertThat(foundMember.get().getMemberDeletedAt()).isNull();
    }

    @Test
    @DisplayName("회원의 운동점수 조회 _ 탈퇴한 경우")
    void findMemberScore_WithdrawnMember() {
        UUID testMemberId = UUID.randomUUID();
        Member withdrawnMember = Member.builder()
                .memberId(testMemberId)
                .memberEmail("e@eail.com")
                .memberNickname("탈퇴유저닉테임")
                .memberDeletedAt(LocalDateTime.now()) // 빌더를 통해 탈퇴 시간 설정
                .build();
        memberRepository.save(withdrawnMember);

        // when
        Optional<Member> foundMember = memberRepository.findByMemberIdAndMemberDeletedAtNull(testMemberId);

        // then
        assertThat(foundMember).isNotPresent();
    }

    @Test
    @DisplayName("회원의 운동점수 조회 _ 회원이 없는 경우")
    void findMemberScore_NonExistentMember() {
        // given
        UUID nonExistentMemberId = UUID.randomUUID();

        // when
        Optional<Member> foundMember = memberRepository.findByMemberIdAndMemberDeletedAtNull(nonExistentMemberId);

        // then
        assertThat(foundMember).isNotPresent();
    }
}