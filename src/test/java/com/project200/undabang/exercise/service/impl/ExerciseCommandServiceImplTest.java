package com.project200.undabang.exercise.service.impl;

import com.project200.undabang.common.context.UserContextHolder;
import com.project200.undabang.common.web.exception.CustomException;
import com.project200.undabang.common.web.exception.ErrorCode;
import com.project200.undabang.exercise.dto.request.CreateExerciseRequestDto;
import com.project200.undabang.exercise.dto.response.ExerciseIdResponseDto;
import com.project200.undabang.exercise.entity.Exercise;
import com.project200.undabang.exercise.repository.ExerciseRepository;
import com.project200.undabang.exercise.service.ExercisePictureService;
import com.project200.undabang.member.entity.Member;
import com.project200.undabang.member.repository.MemberRepository;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.BDDMockito;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class ExerciseCommandServiceImplTest {

    @InjectMocks
    private ExerciseCommandServiceImpl exerciseCommandService;

    @Mock
    private ExerciseRepository exerciseRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private ExercisePictureService exercisePictureService;

    // 운동 필드 생성 관련 테스트들
    // 성공적으로 운동 데이터를 생성하는 테스트
    @Test
    @DisplayName("운동 생성 성공 테스트")
    void createExercise_Success() {
        // given
        UUID testUserId = UUID.randomUUID();
        Member mockMember = BDDMockito.mock(Member.class);
        CreateExerciseRequestDto requestDto = BDDMockito.mock(CreateExerciseRequestDto.class);
        Exercise mockExercise = BDDMockito.mock(Exercise.class);
        ExerciseIdResponseDto expectedResponse = new ExerciseIdResponseDto(1L);

        try (MockedStatic<UserContextHolder> ignored = BDDMockito.mockStatic(UserContextHolder.class)) {
            BDDMockito.given(UserContextHolder.getUserId()).willReturn(testUserId);
            BDDMockito.given(memberRepository.findById(testUserId)).willReturn(Optional.of(mockMember));
            BDDMockito.given(requestDto.toEntity(mockMember)).willReturn(mockExercise);
            BDDMockito.given(exerciseRepository.save(mockExercise)).willReturn(mockExercise);
            BDDMockito.given(mockExercise.getId()).willReturn(1L);

            // when
            ExerciseIdResponseDto actualResponse = exerciseCommandService.createExercise(requestDto);

            // then
            SoftAssertions.assertSoftly(softAssertions -> {
                softAssertions.assertThat(actualResponse).as("응답 객체가 null이 아님").isNotNull();
                softAssertions.assertThat(actualResponse.exerciseId()).as("운동 ID가 일치함").isEqualTo(expectedResponse.exerciseId());
            });
        }
    }

    // 회원 정보를 찾을 수 없어 운동 데이터를 생성할 수 없는 경우의 테스트
    @Test
    @DisplayName("운동 생성 실패 테스트 - 회원 정보 없음")
    void createExercise_Failure_WhenMemberNotFound() {
        // given
        UUID testUserId = UUID.randomUUID();
        CreateExerciseRequestDto requestDto = BDDMockito.mock(CreateExerciseRequestDto.class);

        try (MockedStatic<UserContextHolder> ignored = BDDMockito.mockStatic(UserContextHolder.class)) {
            BDDMockito.given(UserContextHolder.getUserId()).willReturn(testUserId);
            BDDMockito.given(memberRepository.findById(testUserId)).willReturn(Optional.empty());

            // when and then
            Assertions.assertThatThrownBy(() -> exerciseCommandService.createExercise(requestDto))
                    .as("회원 정보를 찾을 수 없으면 예외가 발생해야 함")
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.MEMBER_NOT_FOUND.getMessage());
        }
    }

    // 운동 이미지 업로드 관련 테스트들
    // 운동 이미지 업로드가 성공적으로 완료되는 경우의 테스트
    @Test
    @DisplayName("운동 이미지 업로드 성공 테스트")
    void uploadExerciseImages_Success() {
        // given
        Long exerciseId = 1L;
        List<MultipartFile> mockFiles = List.of(BDDMockito.mock(MultipartFile.class), BDDMockito.mock(MultipartFile.class));
        ExerciseIdResponseDto expectedResponse = new ExerciseIdResponseDto(exerciseId);

        BDDMockito.given(exercisePictureService.uploadExercisePictures(exerciseId, mockFiles))
                .willReturn(expectedResponse);

        // when
        ExerciseIdResponseDto actualResponse = exerciseCommandService.uploadExerciseImages(exerciseId, mockFiles);

        // then
        SoftAssertions.assertSoftly(softAssertions -> {
            softAssertions.assertThat(actualResponse).as("응답 객체가 null이 아님").isNotNull();
            softAssertions.assertThat(actualResponse.exerciseId()).as("운동 ID가 일치함").isEqualTo(expectedResponse.exerciseId());
        });
    }


}