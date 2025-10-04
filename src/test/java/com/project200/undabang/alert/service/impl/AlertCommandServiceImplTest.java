package com.project200.undabang.alert.service.impl;

import com.project200.undabang.alert.dto.response.UpdateExerciseEncouragementResponse;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mockStatic;

@ExtendWith(MockitoExtension.class)
class AlertCommandServiceImplTest {

    private final UUID testUserId = UUID.randomUUID();
    private final Member member = createMember(testUserId);

    @InjectMocks
    private AlertCommandServiceImpl alertService;

    @Mock
    private FcmTokenCommandService fcmTokenCommandService;

    @Mock
    private MemberRepository memberRepository;

    private Member createMember(UUID memberId) {
        return Member.builder()
                .memberId(memberId)
                .build();
    }

    @Nested
    @DisplayName("activateAllExerciseEncouragementToken")
    class ActivateAll {

        @Test
        @DisplayName("성공: 레포지토리에서 멤버 조회 후 fcmTokenCommandService.activateAllTokens 호출 및 응답 반환")
        void activateAll_Success() {
            try (MockedStatic<UserContextHolder> userContext = mockStatic(UserContextHolder.class)) {
                userContext.when(UserContextHolder::getUserId).thenReturn(testUserId);
                given(memberRepository.findById(testUserId)).willReturn(Optional.of(member));
                given(fcmTokenCommandService.activateAllTokens(member)).willReturn(2L);

                UpdateExerciseEncouragementResponse resp = alertService.activateAllExerciseEncouragementToken();

                assertThat(resp).isNotNull();
                then(fcmTokenCommandService).should().activateAllTokens(member);
            }
        }

        @Test
        @DisplayName("실패: 멤버를 찾지 못하면 CustomException(MEMBER_NOT_FOUND) 발생")
        void activateAll_MemberNotFound_Throws() {
            try (MockedStatic<UserContextHolder> userContext = mockStatic(UserContextHolder.class)) {
                userContext.when(UserContextHolder::getUserId).thenReturn(testUserId);
                given(memberRepository.findById(testUserId)).willReturn(Optional.empty());

                assertThatThrownBy(() -> alertService.activateAllExerciseEncouragementToken())
                        .isInstanceOf(CustomException.class)
                        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MEMBER_NOT_FOUND);
            }
        }
    }

    @Nested
    @DisplayName("deactivateAllExerciseEncouragementToken")
    class DeactivateAll {

        @Test
        @DisplayName("성공: 레포지토리에서 멤버 조회 후 fcmTokenCommandService.deactivateAllTokens 호출 및 응답 반환")
        void deactivateAll_Success() {
            try (MockedStatic<UserContextHolder> userContext = mockStatic(UserContextHolder.class)) {
                userContext.when(UserContextHolder::getUserId).thenReturn(testUserId);
                given(memberRepository.findById(testUserId)).willReturn(Optional.of(member));
                given(fcmTokenCommandService.deactivateAllTokens(member)).willReturn(5L);

                UpdateExerciseEncouragementResponse resp = alertService.deactivateAllExerciseEncouragementToken();

                assertThat(resp).isNotNull();
                then(fcmTokenCommandService).should().deactivateAllTokens(member);
            }
        }

        @Test
        @DisplayName("실패: 멤버를 찾지 못하면 CustomException(MEMBER_NOT_FOUND) 발생")
        void deactivateAll_MemberNotFound_Throws() {
            try (MockedStatic<UserContextHolder> userContext = mockStatic(UserContextHolder.class)) {
                userContext.when(UserContextHolder::getUserId).thenReturn(testUserId);
                given(memberRepository.findById(testUserId)).willReturn(Optional.empty());

                assertThatThrownBy(() -> alertService.deactivateAllExerciseEncouragementToken())
                        .isInstanceOf(CustomException.class)
                        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MEMBER_NOT_FOUND);
            }
        }
    }
}