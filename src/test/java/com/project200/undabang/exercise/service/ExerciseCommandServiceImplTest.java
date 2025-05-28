package com.project200.undabang.exercise.service;

import com.project200.undabang.exercise.dto.request.UpdateExerciseRequestDto;
import com.project200.undabang.exercise.entity.Exercise;
import com.project200.undabang.exercise.repository.ExerciseRepository;
import com.project200.undabang.member.entity.Member;
import com.project200.undabang.member.enums.MemberGender;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;

@ExtendWith(MockitoExtension.class)
class ExerciseCommandServiceImplTest {

    @Mock
    private ExerciseRepository exerciseRepository;

    @InjectMocks
    private ExerciseCommandService exerciseCommandService;

    @Test
    @DisplayName("운동기록 문자데이터 업데이트 _ 성공상황")
    void updateExercise() {
        // given
        Member member = Member.builder()
                .memberBday(LocalDate.now().minusDays(1))
                .memberGender(MemberGender.M)
                .memberNickname("nickname")
                .memberEmail("email.com")
                .memberDesc("memberDesc")
                .build();

        Long exerciseId = 1L;
        Exercise exercise = Exercise.builder()
                .member(member)
                .exerciseTitle("title")
                .exerciseStartedAt(LocalDateTime.of(2025,5,27,00,00,00))
                .exerciseEndedAt(LocalDateTime.of(2025,5,27,01,00,00))
                .build();


        UpdateExerciseRequestDto dto = UpdateExerciseRequestDto.builder()
                .exerciseTitle("수정된 운동제목")
                .exerciseStartedAt(LocalDateTime.now().minusDays(1))
                .exerciseEndedAt(LocalDateTime.now())
                .build();

//        BDDMockito.given(exerciseCommandService.updateExercise(BDDMockito.anyLong(), BDDMockito.any())).willReturn()
    }

    @Test
    void deleteExercise() {
    }

    @Test
    void deleteImages() {
    }
}