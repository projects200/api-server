package com.project200.undabang.exercise.service.impl;

import com.project200.undabang.common.context.UserContextHolder;
import com.project200.undabang.common.web.exception.CustomException;
import com.project200.undabang.common.web.exception.ErrorCode;
import com.project200.undabang.exercise.dto.request.CreateExerciseRequestDto;
import com.project200.undabang.exercise.dto.request.UpdateExerciseRequestDto;
import com.project200.undabang.exercise.dto.response.ExerciseIdResponseDto;
import com.project200.undabang.exercise.entity.Exercise;
import com.project200.undabang.exercise.repository.ExerciseRepository;
import com.project200.undabang.exercise.service.ExercisePictureService;
import com.project200.undabang.member.entity.Member;
import com.project200.undabang.member.enums.MemberGender;
import com.project200.undabang.member.repository.MemberRepository;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
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
            ExerciseIdResponseDto responseDto = exerciseCommandService.updateExercise(exerciseId, dto);

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
            Assertions.assertThatThrownBy(() -> exerciseCommandService.updateExercise(exerciseId, dto))
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

            Assertions.assertThatThrownBy(() -> exerciseCommandService.updateExercise(exerciseId,dto))
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

            Assertions.assertThatThrownBy(() -> exerciseCommandService.updateExercise(exerciseId,dto))
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

            Assertions.assertThatThrownBy(() -> exerciseCommandService.updateExercise(exerciseId,dto))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.EXERCISE_RECORD_NOT_FOUND);
        }
    }

    @Test
    @DisplayName("운동기록 이미지 삭제 _ 성공")
    void deleteExerciseImages(){
        // given
        Long testExerciseId = 1L;
        UUID testMemberId = UUID.randomUUID();
        List<Long> deletePictureIdList = List.of(1L,2L,3L);
        Member testMember = Member.builder().memberId(testMemberId).build();

        try(MockedStatic<UserContextHolder> mockedUserContextHolder = Mockito.mockStatic(UserContextHolder.class)){
            mockedUserContextHolder.when(UserContextHolder::getUserId).thenReturn(testMemberId);
            BDDMockito.given(memberRepository.findById(testMemberId)).willReturn(Optional.of(testMember));
            BDDMockito.given(exerciseRepository.existsByRecordIdAndMemberId(testMemberId,testExerciseId)).willReturn(true);
            // 반환타입이 void라 willDoNothing() 사용
            BDDMockito.willDoNothing().given(exercisePictureService).deleteExercisePictures(testMemberId,testExerciseId,deletePictureIdList);

            // when
            exerciseCommandService.deleteImages(testExerciseId,deletePictureIdList);

            // then
            BDDMockito.then(exercisePictureService).should(BDDMockito.times(1)).deleteExercisePictures(testMemberId,testExerciseId,deletePictureIdList);
        }
    }

    @Test
    @DisplayName("운동기록 이미지 삭제 _ 회원 인증 실패")
    void deleteExerciseImages_MemberNotFound(){
        // given
        UUID testMemberId = UUID.randomUUID();
        Long testExerciseId = 1L;
        List<Long> deletePictureIdList = List.of(1L, 2L, 3L);

        try(MockedStatic<UserContextHolder> mockedStatic = Mockito.mockStatic(UserContextHolder.class)){
            mockedStatic.when(UserContextHolder::getUserId).thenReturn(testMemberId);
            BDDMockito.given(memberRepository.findById(testMemberId)).willReturn(Optional.empty());

            //when, then
            Assertions.assertThatThrownBy(() -> exerciseCommandService.deleteImages(testExerciseId, deletePictureIdList))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MEMBER_NOT_FOUND);

            BDDMockito.then(exercisePictureService).should(BDDMockito.never()).deleteExercisePictures(testMemberId,testExerciseId,deletePictureIdList);
        }
    }

    @Test
    @DisplayName("운동기록 이미지 삭제 _ 타인의 운동기록, 사진 접근 확인")
    void deleteExerciseImages_AuthorizationDenied(){
        UUID testMemberId = UUID.randomUUID();
        Long testExerciseId = 1L;
        List<Long> deletePictureIdList = List.of(1L, 2L, 3L);
        Member testMember = Member.builder().memberId(testMemberId).build();

        try(MockedStatic<UserContextHolder> mockedStatic = Mockito.mockStatic(UserContextHolder.class)){
            mockedStatic.when(UserContextHolder::getUserId).thenReturn(testMemberId);
            BDDMockito.given(memberRepository.findById(testMemberId)).willReturn(Optional.of(testMember));
            BDDMockito.given(exerciseRepository.existsByRecordIdAndMemberId(testMemberId,testExerciseId)).willReturn(false);

            //when, then
            Assertions.assertThatThrownBy(() -> exerciseCommandService.deleteImages(testExerciseId, deletePictureIdList))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.AUTHORIZATION_DENIED);

            BDDMockito.then(exercisePictureService).should(BDDMockito.never()).deleteExercisePictures(testMemberId,testExerciseId,deletePictureIdList);
        }
    }

}