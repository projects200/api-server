package com.project200.undabang.auth.service.impl;

import com.project200.undabang.common.context.UserContextHolder;
import com.project200.undabang.common.web.exception.CustomException;
import com.project200.undabang.common.web.exception.ErrorCode;
import com.project200.undabang.member.entity.Member;
import com.project200.undabang.member.repository.MemberRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.BDDMockito;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthServiceImpl 테스트")
class AuthServiceImplTest {

    private final UUID testUserId = UUID.randomUUID();
    private final Member member = Member.builder().memberId(testUserId).build();
    @InjectMocks
    private AuthServiceImpl authService;
    @Mock
    private MemberRepository memberRepository;

    @Nested
    @DisplayName("로그인 기능 테스트")
    class Login {

        @Test
        @DisplayName("로그인 성공")
        void login_Success() {
            try (MockedStatic<UserContextHolder> ignored = BDDMockito.mockStatic(UserContextHolder.class)) {
                // given
                BDDMockito.given(UserContextHolder.getUserId()).willReturn(testUserId);
                BDDMockito.given(memberRepository.findByMemberIdAndMemberDeletedAtNull(testUserId))
                        .willReturn(Optional.of(member));

                // when
                Member result = authService.login();

                // then
                assertThat(result).as("반환된 Member 객체는 null이 아니어야 합니다.").isNotNull();
                assertThat(result.getMemberId()).as("반환된 Member의 ID는 예상과 같아야 합니다.").isEqualTo(testUserId);
                BDDMockito.then(memberRepository).should().findByMemberIdAndMemberDeletedAtNull(testUserId);
            }
        }

        @Test
        @DisplayName("로그인 실패 - 존재하지 않는 회원")
        void login_Fail_MemberNotFound() {
            try (MockedStatic<UserContextHolder> ignored = BDDMockito.mockStatic(UserContextHolder.class)) {
                // given
                BDDMockito.given(UserContextHolder.getUserId()).willReturn(testUserId);
                BDDMockito.given(memberRepository.findByMemberIdAndMemberDeletedAtNull(testUserId))
                        .willReturn(Optional.empty());

                // when & then
                assertThatThrownBy(() -> authService.login())
                        .as("존재하지 않는 회원으로 로그인 시 CustomException이 발생해야 합니다.")
                        .isInstanceOf(CustomException.class)
                        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.LOGIN_FAILED);

                BDDMockito.then(memberRepository).should().findByMemberIdAndMemberDeletedAtNull(testUserId);
            }
        }
    }

    @Nested
    @DisplayName("로그아웃 기능 테스트")
    class Logout {

        @Test
        @DisplayName("로그아웃 성공")
        void logout_Success() {
            try (MockedStatic<UserContextHolder> ignored = BDDMockito.mockStatic(UserContextHolder.class)) {
                // given
                BDDMockito.given(UserContextHolder.getUserId()).willReturn(testUserId);
                BDDMockito.given(memberRepository.findByMemberIdAndMemberDeletedAtNull(testUserId))
                        .willReturn(Optional.of(member));

                // when
                Member result = authService.logout();

                // then
                assertThat(result).as("반환된 Member 객체는 null이 아니어야 합니다.").isNotNull();
                assertThat(result.getMemberId()).as("반환된 Member의 ID는 예상과 같아야 합니다.").isEqualTo(testUserId);
                BDDMockito.then(memberRepository).should().findByMemberIdAndMemberDeletedAtNull(testUserId);
            }
        }

        @Test
        @DisplayName("로그아웃 실패 - 존재하지 않는 회원")
        void logout_Fail_MemberNotFound() {
            try (MockedStatic<UserContextHolder> ignored = BDDMockito.mockStatic(UserContextHolder.class)) {
                // given
                BDDMockito.given(UserContextHolder.getUserId()).willReturn(testUserId);
                BDDMockito.given(memberRepository.findByMemberIdAndMemberDeletedAtNull(testUserId))
                        .willReturn(Optional.empty());

                // when & then
                assertThatThrownBy(() -> authService.logout())
                        .as("존재하지 않는 회원으로 로그아웃 시 CustomException이 발생해야 합니다.")
                        .isInstanceOf(CustomException.class)
                        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.LOGOUT_FAILED);

                BDDMockito.then(memberRepository).should().findByMemberIdAndMemberDeletedAtNull(testUserId);
            }
        }
    }
}