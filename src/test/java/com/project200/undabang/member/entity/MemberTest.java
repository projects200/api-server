package com.project200.undabang.member.entity;

import com.project200.undabang.common.web.exception.CustomException;
import com.project200.undabang.common.web.exception.ErrorCode;
import com.project200.undabang.member.dto.command.SignUpMemberCommand;
import com.project200.undabang.member.enums.MemberGender;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

public class MemberTest {

    @Test
    @DisplayName("회원가입 성공 - 올바른 정보 제공 시")
    public void testSignUpSuccess() {
        // given
        UUID testUserId = UUID.randomUUID();
        SignUpMemberCommand command = SignUpMemberCommand.builder()
                .memberId(testUserId)
                .memberEmail("test@example.com")
                .memberNickname("testNickname")
                .memberGender(MemberGender.UNKNOWN)
                .initialSignupPoints((byte) 35)
                .memberBday(LocalDate.of(2000, 1, 1))
                .build();

        // when
        Member member = Member.signUp(command);

        // then
        assertSoftly(softly -> {
            softly.assertThat(member.getMemberId())
                    .as("memberId가 command와 동일해야 합니다.")
                    .isEqualTo(testUserId);
            softly.assertThat(member.getMemberEmail())
                    .as("memberEmail이 command와 동일해야 합니다.")
                    .isEqualTo("test@example.com");
            softly.assertThat(member.getMemberNickname())
                    .as("memberNickname이 command와 동일해야 합니다.")
                    .isEqualTo("testNickname");
            softly.assertThat(member.getMemberGender())
                    .as("memberGender가 command와 동일해야 합니다.")
                    .isEqualTo(MemberGender.UNKNOWN);
            softly.assertThat(member.getMemberScore())
                    .as("memberScore가 command의 초기 포인트와 동일해야 합니다.")
                    .isEqualTo((byte) 35);
            softly.assertThat(member.getMemberBday())
                    .as("memberBday가 command의 값과 동일해야 합니다.")
                    .isEqualTo(LocalDate.of(2000, 1, 1));
        });
    }

    @Test
    @DisplayName("회원가입 실패 - 생년월일이 오늘 이후인 경우")
    public void testSignUpFailDueToFutureBirthday() {
        // given
        UUID testUserId = UUID.randomUUID();
        SignUpMemberCommand command = SignUpMemberCommand.builder()
                .memberId(testUserId)
                .memberEmail("test@example.com")
                .memberNickname("testNickname")
                .memberGender(MemberGender.MALE)
                .initialSignupPoints((byte) 10)
                .memberBday(LocalDate.now().plusDays(1))
                .build();

        // when & then
        assertThatThrownBy(() -> Member.signUp(command))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MEMBER_BDAY_ERROR);
    }

    @Test
    @DisplayName("회원가입 실패 - 필수 필드가 null인 경우")
    public void testSignUpFailDueToNullFields() {
        // given
        UUID testUserId = UUID.randomUUID();

        // when, then
        assertSoftly(softly -> {

            softly.assertThatThrownBy(() -> Member.signUp(SignUpMemberCommand.builder()
                    .memberId(null)
                    .memberEmail("test@example.com")
                    .memberNickname("testNickname")
                    .memberGender(MemberGender.UNKNOWN)
                    .initialSignupPoints((byte) 10)
                    .memberBday(LocalDate.of(2000, 1, 1))
                    .build())).isInstanceOf(NullPointerException.class);

            softly.assertThatThrownBy(() -> Member.signUp(SignUpMemberCommand.builder()
                    .memberId(testUserId)
                    .memberEmail(null)
                    .memberNickname("testNickname")
                    .memberGender(MemberGender.UNKNOWN)
                    .initialSignupPoints((byte) 10)
                    .memberBday(LocalDate.of(2000, 1, 1))
                    .build())).isInstanceOf(NullPointerException.class);

            softly.assertThatThrownBy(() -> Member.signUp(SignUpMemberCommand.builder()
                    .memberId(testUserId)
                    .memberEmail("test@example.com")
                    .memberNickname(null)
                    .memberGender(MemberGender.UNKNOWN)
                    .initialSignupPoints((byte) 10)
                    .memberBday(LocalDate.of(2000, 1, 1))
                    .build())).isInstanceOf(NullPointerException.class);

            softly.assertThatThrownBy(() -> Member.signUp(SignUpMemberCommand.builder()
                    .memberId(testUserId)
                    .memberEmail("test@example.com")
                    .memberNickname("testNickname")
                    .memberGender(null)
                    .initialSignupPoints((byte) 10)
                    .memberBday(LocalDate.of(2000, 1, 1))
                    .build())).isInstanceOf(NullPointerException.class);


            softly.assertThatThrownBy(() -> Member.signUp(SignUpMemberCommand.builder()
                    .memberId(testUserId)
                    .memberEmail("test@example.com")
                    .memberNickname("testNickname")
                    .memberGender(MemberGender.UNKNOWN)
                    .initialSignupPoints((byte) 10)
                    .memberBday(null)
                    .build())).isInstanceOf(NullPointerException.class);
        });
    }
}