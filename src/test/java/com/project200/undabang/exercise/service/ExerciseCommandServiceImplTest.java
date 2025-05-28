package com.project200.undabang.exercise.service;

import com.project200.undabang.common.context.UserContextHolder;
import com.project200.undabang.common.web.exception.CustomException;
import com.project200.undabang.common.web.exception.ErrorCode;
import com.project200.undabang.exercise.dto.request.UpdateExerciseRequestDto;
import com.project200.undabang.exercise.dto.response.ExerciseIdResponseDto;
import com.project200.undabang.exercise.entity.Exercise;
import com.project200.undabang.exercise.repository.ExerciseRepository;
import com.project200.undabang.exercise.service.impl.ExerciseCommandServiceImpl;
import com.project200.undabang.member.entity.Member;
import com.project200.undabang.member.enums.MemberGender;
import com.project200.undabang.member.repository.MemberRepository;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class ExerciseCommandServiceImplTest {

    @Mock
    private ExerciseRepository exerciseRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private ExercisePictureService exercisePictureService;

    @InjectMocks
    private ExerciseCommandServiceImpl service;

    @Test
    @DisplayName("운동기록 문자데이터 업데이트 _ 성공상황")
    void updateExercise() {
        // given
        UUID memberId = UUID.randomUUID();

        Member member = Member.builder()
                .memberId(memberId)
                .memberDesc("설명")
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
                .exerciseDetail("내용")
                .exerciseLocation("장소")
                .exercisePersonalType("헬스")
                .exerciseStartedAt(LocalDateTime.of(2025,5,27,00,00,00))
                .exerciseEndedAt(LocalDateTime.of(2025,5,27,01,00,00))
                .build();

        UpdateExerciseRequestDto dto = UpdateExerciseRequestDto.builder()
                .exerciseTitle("수정된 운동제목")
                .exerciseStartedAt(LocalDateTime.now().minusDays(1))
                .exerciseEndedAt(LocalDateTime.now())
                .build();

        try(MockedStatic<UserContextHolder> mockedUserContextHolder = Mockito.mockStatic(UserContextHolder.class)){
            mockedUserContextHolder.when(UserContextHolder::getUserId).thenReturn(memberId);

            BDDMockito.given(memberRepository.findById(memberId)).willReturn(Optional.of(member));
            BDDMockito.given(exerciseRepository.findById(exerciseId)).willReturn(Optional.of(exercise));
            // 예외처리 통과설정
            BDDMockito.given(exerciseRepository.existsByRecordIdAndMemberId(memberId,exerciseId)).willReturn(true);

            //when
            ExerciseIdResponseDto responseDto = service.updateExercise(exerciseId, dto);

            //then
            Assertions.assertThat(responseDto).isNotNull();
            Assertions.assertThat(responseDto.exerciseId()).isEqualTo(exerciseId);

            Assertions.assertThat(dto.getExerciseTitle()).isEqualTo(exercise.getExerciseTitle());
            Assertions.assertThat(dto.getExerciseStartedAt()).isEqualTo(exercise.getExerciseStartedAt());
            Assertions.assertThat(dto.getExerciseEndedAt()).isEqualTo(exercise.getExerciseEndedAt());
        }
    }

    @Test
    @DisplayName("운동기록 수정 _ 회원인증실패")
    void updateExercise_memberCheckFailed() {
        // given
        UUID memberId = UUID.randomUUID();
        Long exerciseId = 1L;
        UpdateExerciseRequestDto dto = UpdateExerciseRequestDto.builder()
                .exerciseTitle("수정된 운동제목")
                .exerciseStartedAt(LocalDateTime.now().minusDays(1))
                .exerciseEndedAt(LocalDateTime.now())
                .build();

        try(MockedStatic<UserContextHolder> mockedUserContextHolder = Mockito.mockStatic(UserContextHolder.class)){
            mockedUserContextHolder.when(UserContextHolder::getUserId).thenReturn(memberId);

            BDDMockito.given(memberRepository.findById(memberId)).willThrow(new CustomException(ErrorCode.MEMBER_NOT_FOUND));

            //when then
            Assertions.assertThatThrownBy(() -> service.updateExercise(exerciseId, dto))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MEMBER_NOT_FOUND);
        }
    }

    @Test
    @DisplayName("운동기록 수정 _ 날짜 입력 실패")
    void updateExercise_dateInputFailed(){
        //given
        UUID memberId = UUID.randomUUID();
        Long exerciseId = 1L;
        Member mockedMember = Member.builder().memberId(memberId).build();
        UpdateExerciseRequestDto dto = UpdateExerciseRequestDto.builder()
                .exerciseTitle("수정된 운동제목")
                .exerciseStartedAt(LocalDateTime.now().minusHours(1))
                .exerciseEndedAt(LocalDateTime.now().plusDays(1))
                .build();

        try(MockedStatic<UserContextHolder> mockedUserContextHolder = Mockito.mockStatic(UserContextHolder.class)){
            mockedUserContextHolder.when(UserContextHolder::getUserId).thenReturn(memberId);

            BDDMockito.given(memberRepository.findById(memberId)).willReturn(Optional.of(mockedMember));

            Assertions.assertThatThrownBy(() -> service.updateExercise(exerciseId,dto))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT_VALUE);
        }
    }

    @Test
    @DisplayName("운동기록 수정 _ 타인의 운동기록 접근")
    void updateExercise_wrongExerciseIdInput(){
        UUID memberId = UUID.randomUUID();
        Long exerciseId = 1L;
        Member mockedMember = Member.builder().memberId(memberId).build();
        UpdateExerciseRequestDto dto = UpdateExerciseRequestDto.builder()
                .exerciseTitle("수정된 운동제목")
                .exerciseStartedAt(LocalDateTime.now().minusHours(1))
                .exerciseEndedAt(LocalDateTime.now())
                .build();

        try(MockedStatic<UserContextHolder> mockedUserContextHolder = Mockito.mockStatic(UserContextHolder.class)){
            mockedUserContextHolder.when(UserContextHolder::getUserId).thenReturn(memberId);

            BDDMockito.given(memberRepository.findById(memberId)).willReturn(Optional.of(mockedMember));
            BDDMockito.given(exerciseRepository.existsByRecordIdAndMemberId(memberId,exerciseId)).willReturn(false);

            Assertions.assertThatThrownBy(() -> service.updateExercise(exerciseId,dto))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.AUTHORIZATION_DENIED);
        }
    }

    @Test
    @DisplayName("운동기록 수정 _ 운동기록 불러오기 실패")
    void updateExercise_recordNotFound(){
        UUID memberId = UUID.randomUUID();
        Long exerciseId = 1L;
        Member mockedMember = Member.builder().memberId(memberId).build();
        UpdateExerciseRequestDto dto = UpdateExerciseRequestDto.builder()
                .exerciseTitle("수정된 운동제목")
                .exerciseStartedAt(LocalDateTime.now().minusHours(1))
                .exerciseEndedAt(LocalDateTime.now())
                .build();

        try(MockedStatic<UserContextHolder> mockedUserContextHolder = Mockito.mockStatic(UserContextHolder.class)){
            mockedUserContextHolder.when(UserContextHolder::getUserId).thenReturn(memberId);

            BDDMockito.given(memberRepository.findById(memberId)).willReturn(Optional.of(mockedMember));
            BDDMockito.given(exerciseRepository.existsByRecordIdAndMemberId(memberId,exerciseId)).willReturn(true);
            BDDMockito.given(exerciseRepository.findById(exerciseId)).willThrow(new CustomException(ErrorCode.EXERCISE_RECORD_NOT_FOUND));

            Assertions.assertThatThrownBy(() -> service.updateExercise(exerciseId,dto))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.EXERCISE_RECORD_NOT_FOUND);
        }
    }

    @Test
    void deleteExercise() {
    }

    @Test
    void deleteImages() {
    }
}