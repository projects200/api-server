package com.project200.undabang.timer.simple.service.impl;

import com.project200.undabang.common.context.UserContextHolder;
import com.project200.undabang.common.web.exception.CustomException;
import com.project200.undabang.common.web.exception.ErrorCode;
import com.project200.undabang.member.entity.Member;
import com.project200.undabang.member.repository.MemberRepository;
import com.project200.undabang.timer.simple.dto.GetSimpleTimerResponseDto;
import com.project200.undabang.timer.simple.entity.SimpleTimer;
import com.project200.undabang.timer.simple.repository.SimpleTimerRepository;
import org.assertj.core.api.Assertions;
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

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mockStatic;

@ExtendWith(MockitoExtension.class)
class SimpleTimerQueryServiceImplTest {

    @Mock
    private SimpleTimerRepository simpleTimerRepository;

    @Mock
    private MemberRepository memberRepository;

    @InjectMocks
    private SimpleTimerQueryServiceImpl simpleTimerQueryService;

    @Nested
    @DisplayName("getSimpleTimers() 메소드는")
    class Describe_getSimpleTimers {
        @Test
        @DisplayName("유효한 사용자로 호출될 때, 회원의 심플 타이머 정보를 조합하여 반환한다")
        void getSimpleTimers() {
            // given
            UUID testUserId = UUID.randomUUID();
            Member testUser = Member.builder().memberId(testUserId).build();
            List<SimpleTimer> simpleTimerList = createSimpleTimerList(testUser);

            try(MockedStatic<UserContextHolder> ignored = mockStatic(UserContextHolder.class)) {
                given(UserContextHolder.getUserId()).willReturn(testUserId);
                given(memberRepository.findById(testUserId)).willReturn(Optional.of(testUser));
                given(simpleTimerRepository.findByMemberAndSimpleTimerDeletedAtNull(testUser)).willReturn(simpleTimerList);

                // when
                GetSimpleTimerResponseDto responseDto = simpleTimerQueryService.getSimpleTimers();

                // then
                Assertions.assertThat(responseDto).isNotNull();
                Assertions.assertThat(responseDto.getSimpleTimerCount()).isEqualTo(simpleTimerList.size());
                Assertions.assertThat(responseDto.getSimpleTimers()).hasSize(simpleTimerList.size());
                Assertions.assertThat(responseDto.getSimpleTimers().get(0).simpleTimerId()).isEqualTo(simpleTimerList.get(0).getId());
                Assertions.assertThat(responseDto.getSimpleTimers().get(0).time()).isEqualTo(simpleTimerList.get(0).getSimpleTimerTime());
            }
        }

        @Test
        @DisplayName("사용자가 없을시 CustomException(MemberNotFound) 예외를 반환한다")
        void getSimpleTimers_MemberNotFound() {
            // given
            UUID testUserId = UUID.randomUUID();
            try(MockedStatic<UserContextHolder> ignored = mockStatic(UserContextHolder.class)) {
                given(UserContextHolder.getUserId()).willReturn(testUserId);
                given(memberRepository.findById(testUserId)).willReturn(Optional.empty());

                // when then
                Assertions.assertThatThrownBy(() -> simpleTimerQueryService.getSimpleTimers())
                        .isInstanceOf(CustomException.class)
                        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MEMBER_NOT_FOUND);
            }
        }

        private List<SimpleTimer> createSimpleTimerList(Member testUser) {
            return List.of(
                    SimpleTimer.builder()
                            .id(1L)
                            .member(testUser)
                            .simpleTimerTime(30)
                            .build(),
                    SimpleTimer.builder()
                            .id(2L)
                            .member(testUser)
                            .simpleTimerTime(60)
                            .build()
            );
        }
    }
}