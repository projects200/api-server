package com.project200.undabang.timer.custom.service.impl;

import com.project200.undabang.common.context.UserContextHolder;
import com.project200.undabang.common.web.exception.CustomException;
import com.project200.undabang.common.web.exception.ErrorCode;
import com.project200.undabang.member.entity.Member;
import com.project200.undabang.member.repository.MemberRepository;
import com.project200.undabang.timer.custom.dto.response.CustomTimerDetailResponse;
import com.project200.undabang.timer.custom.dto.response.CustomTimerListResponse;
import com.project200.undabang.timer.custom.entity.CustomTimer;
import com.project200.undabang.timer.custom.entity.CustomTimerStep;
import com.project200.undabang.timer.custom.repository.CustomTimerRepository;
import com.project200.undabang.timer.custom.repository.CustomTimerStepRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mockStatic;

@ExtendWith(MockitoExtension.class)
class CustomTimerQueryServiceImplTest {

    @Mock
    private CustomTimerStepRepository customTimerStepRepository;

    @Mock
    private CustomTimerRepository customTimerRepository;

    @Mock
    private MemberRepository memberRepository;

    @InjectMocks
    private CustomTimerQueryServiceImpl customTimerQueryService;

    @Nested
    @DisplayName("getCustomTimerDetail() 메소드는")
    class Describe_getCustomTimerDetail {

        @Test
        @DisplayName("정상적으로 타이머 상세 정보를 반환한다")
        void returns_detail_response() {
            // given
            UUID userId = UUID.randomUUID();
            Member member = Member.builder().memberId(userId).build();
            CustomTimer timer = CustomTimer.builder().id(1L).member(member).customTimerName("타이머").build();
            CustomTimerStep step = CustomTimerStep.builder().id(10L).customTimer(timer).customTimerStepName("스텝1").customTimerStepOrder((byte) 1).customTimerStepTime(60).build();

            try (MockedStatic<UserContextHolder> ignored = mockStatic(UserContextHolder.class)) {
                given(UserContextHolder.getUserId()).willReturn(userId);
                given(memberRepository.findById(userId)).willReturn(Optional.of(member));
                given(customTimerRepository.findById(1L)).willReturn(Optional.of(timer));
                given(customTimerStepRepository.findAllByCustomTimerAndCustomTimerStepDeletedAtNull(timer)).willReturn(List.of(step));

                // when
                CustomTimerDetailResponse response = customTimerQueryService.getCustomTimerDetail(1L);

                // then
                assertThat(response).isNotNull();
                assertThat(response.getCustomTimerId()).isEqualTo(timer.getId());
                assertThat(response.getCustomTimerName()).isEqualTo(timer.getCustomTimerName());
                assertThat(response.getCustomTimerSteps()).hasSize(1);
                assertThat(response.getCustomTimerSteps().get(0).customTimerStepName()).isEqualTo("스텝1");
            }
        }

        @Test
        @DisplayName("타이머가 존재하지 않으면 예외를 던진다")
        void throws_when_timer_not_found() {
            UUID userId = UUID.randomUUID();
            Member member = Member.builder().memberId(userId).build();

            try (MockedStatic<UserContextHolder> ignored = mockStatic(UserContextHolder.class)) {
                given(UserContextHolder.getUserId()).willReturn(userId);
                given(memberRepository.findById(userId)).willReturn(Optional.of(member));
                given(customTimerRepository.findById(1L)).willReturn(Optional.empty());

                assertThatThrownBy(() -> customTimerQueryService.getCustomTimerDetail(1L))
                        .isInstanceOf(CustomException.class)
                        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CUSTOM_TIMER_NOT_FOUND);
            }
        }

        @Test
        @DisplayName("타이머 소유자가 아니면 권한 예외를 던진다")
        void throws_when_not_owner() {
            UUID userId = UUID.randomUUID();
            UUID otherUserId = UUID.randomUUID();
            Member member = Member.builder().memberId(userId).build();
            Member otherMember = Member.builder().memberId(otherUserId).build();
            CustomTimer timer = CustomTimer.builder().id(1L).member(otherMember).customTimerName("타이머").build();

            try (MockedStatic<UserContextHolder> ignored = mockStatic(UserContextHolder.class)) {
                given(UserContextHolder.getUserId()).willReturn(userId);
                given(memberRepository.findById(userId)).willReturn(Optional.of(member));
                given(customTimerRepository.findById(1L)).willReturn(Optional.of(timer));

                assertThatThrownBy(() -> customTimerQueryService.getCustomTimerDetail(1L))
                        .isInstanceOf(CustomException.class)
                        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.AUTHORIZATION_DENIED);
            }
        }
    }

    @Nested
    @DisplayName("getCustomTimerList() 메소드는")
    class Describe_getCustomTimerList {

        @Test
        @DisplayName("유효한 사용자로 호출될 때, 회원의 커스텀 타이머 정보를 조합하여 반환한다")
        void getCustomTimerList() {
            // given
            UUID testUserId = UUID.randomUUID();
            Member testUser = Member.builder().memberId(testUserId).build();
            List<CustomTimer> customTimerList = createCustomTimerList(testUser);

            try (MockedStatic<UserContextHolder> ignored = mockStatic(UserContextHolder.class)) {
                given(UserContextHolder.getUserId()).willReturn(testUserId);
                given(memberRepository.findById(testUserId)).willReturn(Optional.of(testUser));
                given(customTimerRepository.findAllByMemberAndCustomTimerDeletedAtNull(testUser)).willReturn(customTimerList);

                // when
                CustomTimerListResponse response = customTimerQueryService.getCustomTimerList();

                // then
                assertThat(response).isNotNull();
                assertThat(response.getCustomTimerCount()).isEqualTo(customTimerList.size());
                assertThat(response.getCustomTimers()).hasSize(customTimerList.size());
                assertThat(response.getCustomTimers().get(0).customTimerId()).isEqualTo(customTimerList.get(0).getId());
                assertThat(response.getCustomTimers().get(0).customTimerName()).isEqualTo(customTimerList.get(0).getCustomTimerName());
            }
        }

        @Test
        @DisplayName("사용자가 없을시 CustomException(MEMBER_NOT_FOUND) 예외를 반환한다")
        void getCustomTimerList_MemberNotFound() {
            // given
            UUID testUserId = UUID.randomUUID();
            try (MockedStatic<UserContextHolder> ignored = mockStatic(UserContextHolder.class)) {
                given(UserContextHolder.getUserId()).willReturn(testUserId);
                given(memberRepository.findById(testUserId)).willReturn(Optional.empty());

                // when & then
                assertThatThrownBy(() -> customTimerQueryService.getCustomTimerList())
                        .isInstanceOf(CustomException.class)
                        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MEMBER_NOT_FOUND);
            }
        }

        private List<CustomTimer> createCustomTimerList(Member testUser) {
            return List.of(
                    CustomTimer.builder().id(1L).member(testUser).customTimerName("공부").build(),
                    CustomTimer.builder().id(2L).member(testUser).customTimerName("운동").build()
            );
        }
    }


}