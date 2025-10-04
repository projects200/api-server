package com.project200.undabang.alert.service.impl;

import com.project200.undabang.common.context.UserContextHolder;
import com.project200.undabang.common.web.exception.CustomException;
import com.project200.undabang.common.web.exception.ErrorCode;
import com.project200.undabang.member.entity.Member;
import com.project200.undabang.member.repository.MemberRepository;
import com.project200.undabang.notification.fcm.service.FcmTokenCommandService;
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

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
@DisplayName("AlertServiceImpl 테스트")
class AlertCommandServiceImplTest {

    private final UUID testUserId = UUID.randomUUID();
    private final Member member = createMember(testUserId);
    private final String fcmToken = "test-fcm-token";
    @InjectMocks
    private AlertCommandServiceImpl alertService;
    @Mock
    private FcmTokenCommandService fcmTokenCommandService;
    @Mock
    private MemberRepository memberRepository;

    private Member createMember(UUID memberId) {
        return Member.builder().memberId(memberId).build();
    }

    @Nested
    @DisplayName("알림 활성화 기능 테스트")
    class ActivateAlert {

        @Test
        @DisplayName("유효한 FCM 토큰으로 알림 활성화를 성공한다")
        void activateAlert_WithValidToken_Success() {
            try (MockedStatic<UserContextHolder> userContextHolderMock = mockStatic(UserContextHolder.class)) {
                // given
                userContextHolderMock.when(UserContextHolder::getUserId).thenReturn(testUserId);
                given(memberRepository.findById(testUserId)).willReturn(Optional.of(member));

                // when
                alertService.activateAlert(fcmToken);

                // then
                then(fcmTokenCommandService).should().activateFcmToken(member, fcmToken);
            }
        }

        @Test
        @DisplayName("FCM 토큰이 null이면 아무 작업도 수행하지 않는다")
        void activateAlert_WithNullToken_DoesNothing() {
            try (MockedStatic<UserContextHolder> userContextHolderMock = mockStatic(UserContextHolder.class)) {
                // given
                userContextHolderMock.when(UserContextHolder::getUserId).thenReturn(testUserId);
                given(memberRepository.findById(testUserId)).willReturn(Optional.of(member));

                // when
                alertService.activateAlert(null);

                // then
                then(fcmTokenCommandService).should(never()).activateFcmToken(BDDMockito.any(), BDDMockito.anyString());
            }
        }

        @Test
        @DisplayName("FCM 토큰이 비어있으면 아무 작업도 수행하지 않는다")
        void activateAlert_WithBlankToken_DoesNothing() {
            try (MockedStatic<UserContextHolder> userContextHolderMock = mockStatic(UserContextHolder.class)) {
                // given
                userContextHolderMock.when(UserContextHolder::getUserId).thenReturn(testUserId);
                given(memberRepository.findById(testUserId)).willReturn(Optional.of(member));

                // when
                alertService.activateAlert(" ");

                // then
                then(fcmTokenCommandService).should(never()).activateFcmToken(BDDMockito.any(), BDDMockito.anyString());
            }
        }

        @Test
        @DisplayName("사용자를 찾을 수 없으면 CustomException을 던진다")
        void activateAlert_MemberNotFound_ThrowsException() {
            try (MockedStatic<UserContextHolder> userContextHolderMock = mockStatic(UserContextHolder.class)) {
                // given
                userContextHolderMock.when(UserContextHolder::getUserId).thenReturn(testUserId);
                given(memberRepository.findById(testUserId)).willReturn(Optional.empty());

                // when & then
                assertThatThrownBy(() -> alertService.activateAlert(fcmToken))
                        .isInstanceOf(CustomException.class)
                        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MEMBER_NOT_FOUND);
            }
        }
    }

    @Nested
    @DisplayName("알림 비활성화 기능 테스트")
    class DeactivateAlert {

        @Test
        @DisplayName("유효한 FCM 토큰으로 알림 비활성화를 성공한다")
        void deactivateAlert_WithValidToken_Success() {
            try (MockedStatic<UserContextHolder> userContextHolderMock = mockStatic(UserContextHolder.class)) {
                // given
                userContextHolderMock.when(UserContextHolder::getUserId).thenReturn(testUserId);
                given(memberRepository.findById(testUserId)).willReturn(Optional.of(member));

                // when
                alertService.deactivateAlert(fcmToken);

                // then
                then(fcmTokenCommandService).should().deactivateFcmToken(member, fcmToken);
            }
        }

        @Test
        @DisplayName("FCM 토큰이 null이면 아무 작업도 수행하지 않는다")
        void deactivateAlert_WithNullToken_DoesNothing() {
            try (MockedStatic<UserContextHolder> userContextHolderMock = mockStatic(UserContextHolder.class)) {
                // given
                userContextHolderMock.when(UserContextHolder::getUserId).thenReturn(testUserId);
                given(memberRepository.findById(testUserId)).willReturn(Optional.of(member));

                // when
                alertService.deactivateAlert(null);

                // then
                then(fcmTokenCommandService).should(never()).deactivateFcmToken(BDDMockito.any(), BDDMockito.anyString());
            }
        }

        @Test
        @DisplayName("FCM 토큰이 비어있으면 아무 작업도 수행하지 않는다")
        void deactivateAlert_WithBlankToken_DoesNothing() {
            try (MockedStatic<UserContextHolder> userContextHolderMock = mockStatic(UserContextHolder.class)) {
                // given
                userContextHolderMock.when(UserContextHolder::getUserId).thenReturn(testUserId);
                given(memberRepository.findById(testUserId)).willReturn(Optional.of(member));

                // when
                alertService.deactivateAlert(" ");

                // then
                then(fcmTokenCommandService).should(never()).deactivateFcmToken(BDDMockito.any(), BDDMockito.anyString());
            }
        }

        @Test
        @DisplayName("사용자를 찾을 수 없으면 CustomException을 던진다")
        void deactivateAlert_MemberNotFound_ThrowsException() {
            try (MockedStatic<UserContextHolder> userContextHolderMock = mockStatic(UserContextHolder.class)) {
                // given
                userContextHolderMock.when(UserContextHolder::getUserId).thenReturn(testUserId);
                given(memberRepository.findById(testUserId)).willReturn(Optional.empty());

                // when & then
                assertThatThrownBy(() -> alertService.deactivateAlert(fcmToken))
                        .isInstanceOf(CustomException.class)
                        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MEMBER_NOT_FOUND);
            }
        }
    }
}