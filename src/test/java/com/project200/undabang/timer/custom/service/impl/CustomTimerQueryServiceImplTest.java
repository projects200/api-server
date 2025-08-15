package com.project200.undabang.timer.custom.service.impl;

import com.project200.undabang.common.context.UserContextHolder;
import com.project200.undabang.common.web.exception.CustomException;
import com.project200.undabang.common.web.exception.ErrorCode;
import com.project200.undabang.member.entity.Member;
import com.project200.undabang.member.repository.MemberRepository;
import com.project200.undabang.timer.custom.dto.response.GetCustomTimerListResponse;
import com.project200.undabang.timer.custom.entity.CustomTimer;
import com.project200.undabang.timer.custom.repository.CustomTimerRepository;
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
    private CustomTimerRepository customTimerRepository;

    @Mock
    private MemberRepository memberRepository;

    @InjectMocks
    private CustomTimerQueryServiceImpl customTimerQueryService;

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
                given(customTimerRepository.findByMemberAndCustomTimerDeletedAtNull(testUser)).willReturn(customTimerList);

                // when
                GetCustomTimerListResponse response = customTimerQueryService.getCustomTimerList();

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