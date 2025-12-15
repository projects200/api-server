package com.project200.undabang.member.service.impl;

import com.project200.undabang.common.context.UserContextHolder;
import com.project200.undabang.common.web.exception.CustomException;
import com.project200.undabang.common.web.exception.ErrorCode;
import com.project200.undabang.exercise.entity.ExerciseType;
import com.project200.undabang.exercise.repository.ExerciseTypeRepository;
import com.project200.undabang.member.dto.response.AvailableExerciseTypeResponse;
import com.project200.undabang.member.dto.response.MyPreferredExerciseResponse;
import com.project200.undabang.member.entity.Member;
import com.project200.undabang.member.entity.PreferredExercise;
import com.project200.undabang.member.enums.ExerciseSkillLevel;
import com.project200.undabang.member.repository.MemberRepository;
import com.project200.undabang.member.repository.PreferredExerciseRepository;
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
import static org.mockito.BDDMockito.mockStatic;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class PreferredExerciseQueryServiceImplTest {

    @InjectMocks
    private PreferredExerciseQueryServiceImpl preferredExerciseQueryService;

    @Mock
    private ExerciseTypeRepository exerciseTypeRepository;

    @Mock
    private PreferredExerciseRepository preferredExerciseRepository;

    @Mock
    private MemberRepository memberRepository;

    // ============== 테스트 헬퍼 메소드 ==============

    private Member createMember(UUID memberId) {
        return Member.builder()
                .memberId(memberId)
                .memberEmail("test@email.com")
                .memberNickname("테스트유저")
                .build();
    }

    private ExerciseType createExerciseType(Long id, String name) {
        return ExerciseType.builder()
                .id(id)
                .exerciseName(name)
                .exerciseTypeImageUrl("http://example.com/" + name + ".jpg")
                .build();
    }

    private PreferredExercise createPreferredExercise(Long id, Member member, ExerciseType exerciseType) {
        PreferredExercise preferredExercise = PreferredExercise.builder()
                .id(id)
                .member(member)
                .exercise(exerciseType)
                .preferredExerciseSkillLevel(ExerciseSkillLevel.INTERMEDIATE)
                .build();
        preferredExercise.setDaysOfWeek(new boolean[] { true, false, true, false, true, false, false });
        return preferredExercise;
    }

    @Nested
    @DisplayName("getAvailableExerciseTypes 메소드는")
    class GetAvailableExerciseTypes {

        @Test
        @DisplayName("삭제되지 않은 모든 운동 종류를 조회한다")
        void getAvailableExerciseTypes_Success() {
            // given
            ExerciseType exerciseType1 = createExerciseType(1L, "헬스");
            ExerciseType exerciseType2 = createExerciseType(2L, "러닝");

            given(exerciseTypeRepository.findAllByExerciseTypeDeletedAtNullOrderBySelectionCountDesc())
                    .willReturn(List.of(exerciseType1, exerciseType2));

            // when
            List<AvailableExerciseTypeResponse> result = preferredExerciseQueryService.getAvailableExerciseTypes();

            // then
            assertThat(result).hasSize(2);
            assertThat(result).extracting("exerciseName").containsExactly("헬스", "러닝");

            then(exerciseTypeRepository).should(times(1)).findAllByExerciseTypeDeletedAtNullOrderBySelectionCountDesc();
        }
    }

    @Nested
    @DisplayName("getMyPreferredExercises 메소드는")
    class GetMyPreferredExercises {

        @Test
        @DisplayName("현재 사용자의 선호 운동 목록을 조회한다")
        void getMyPreferredExercises_Success() {
            UUID memberId = UUID.randomUUID();
            Member member = createMember(memberId);
            ExerciseType exerciseType = createExerciseType(1L, "헬스");
            PreferredExercise preferredExercise = createPreferredExercise(1L, member, exerciseType);

            try (MockedStatic<UserContextHolder> ignored = mockStatic(UserContextHolder.class)) {
                // given
                given(UserContextHolder.getUserId()).willReturn(memberId);
                given(memberRepository.findById(memberId)).willReturn(Optional.of(member));
                given(preferredExerciseRepository.findAllByMemberAndPreferredExerciseDeletedAtNull(member))
                        .willReturn(List.of(preferredExercise));

                // when
                List<MyPreferredExerciseResponse> result = preferredExerciseQueryService.getMyPreferredExercises();

                // then
                assertThat(result).hasSize(1);
                assertThat(result.get(0).getExerciseName()).isEqualTo("헬스");
                assertThat(result.get(0).getSkillLevel()).isEqualTo(ExerciseSkillLevel.INTERMEDIATE);

                then(memberRepository).should(times(1)).findById(memberId);
                then(preferredExerciseRepository).should(times(1))
                        .findAllByMemberAndPreferredExerciseDeletedAtNull(member);
            }
        }

        @Test
        @DisplayName("존재하지 않는 사용자의 경우 예외를 발생시킨다")
        void getMyPreferredExercises_MemberNotFound() {
            UUID memberId = UUID.randomUUID();

            try (MockedStatic<UserContextHolder> ignored = mockStatic(UserContextHolder.class)) {
                // given
                given(UserContextHolder.getUserId()).willReturn(memberId);
                given(memberRepository.findById(memberId)).willReturn(Optional.empty());

                // when & then
                assertThatThrownBy(() -> preferredExerciseQueryService.getMyPreferredExercises())
                        .isInstanceOf(CustomException.class)
                        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MEMBER_NOT_FOUND);

                then(memberRepository).should(times(1)).findById(memberId);
                then(preferredExerciseRepository).shouldHaveNoInteractions();
            }
        }
    }
}
