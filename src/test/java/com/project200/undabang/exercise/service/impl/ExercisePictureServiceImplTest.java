package com.project200.undabang.exercise.service.impl;

import com.project200.undabang.common.context.UserContextHolder;
import com.project200.undabang.common.repository.PictureRepository;
import com.project200.undabang.common.service.S3Service;
import com.project200.undabang.common.web.exception.CustomException;
import com.project200.undabang.common.web.exception.ErrorCode;
import com.project200.undabang.common.web.exception.S3UploadFailedException;
import com.project200.undabang.exercise.dto.response.ExerciseIdResponseDto;
import com.project200.undabang.exercise.entity.Exercise;
import com.project200.undabang.exercise.repository.ExercisePictureRepository;
import com.project200.undabang.exercise.repository.ExerciseRepository;
import com.project200.undabang.member.entity.Member;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.BDDMockito;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
public class ExercisePictureServiceImplTest {

    @Mock
    private ExerciseRepository exerciseRepository;

    @Mock
    private PictureRepository pictureRepository;

    @Mock
    private ExercisePictureRepository exercisePictureRepository;

    @Mock
    private S3Service s3Service;

    @InjectMocks
    private ExercisePictureServiceImpl exercisePictureService;

    @Test
    @DisplayName("운동 사진 업로드 성공")
    void testUploadExercisePictures_Success() {
        // given
        Long testExerciseId = 1L;
        UUID testUserId = UUID.randomUUID();
        Member testMember = Member.builder().memberId(testUserId).build();
        Exercise testExercise = Exercise.builder().id(testExerciseId).member(testMember).build();

        List<MultipartFile> testFiles = List.of(
                new MockMultipartFile("file1.jpg", "file1.jpg", MediaType.IMAGE_JPEG_VALUE, new byte[0]),
                new MockMultipartFile("file2.png", "file2.png", MediaType.IMAGE_PNG_VALUE, new byte[0])
        );

        try (var ignored = BDDMockito.mockStatic(UserContextHolder.class)) {
            BDDMockito.given(UserContextHolder.getUserId()).willReturn(testUserId);
            BDDMockito.given(exerciseRepository.findById(testExerciseId)).willReturn(Optional.of(testExercise));
            BDDMockito.given(exercisePictureRepository.countByExercise_Id(testExerciseId)).willReturn(0L);
            BDDMockito.given(s3Service.generateObjectKey(BDDMockito.anyString(), BDDMockito.any())).willReturn("key");
            BDDMockito.given(s3Service.uploadImage(BDDMockito.any(), BDDMockito.anyString())).willReturn("url");

            // when
            ExerciseIdResponseDto result = exercisePictureService.uploadExercisePictures(testExerciseId, testFiles);

            // then
            SoftAssertions.assertSoftly(softAssertions -> {
                softAssertions.assertThat(result).as("운동 ID가 반환되어야 함").isNotNull();
                softAssertions.assertThat(result.exerciseId()).as("올바른 운동 ID가 반환되어야 함").isEqualTo(testExerciseId);
            });
        }
    }

    @Test
    @DisplayName("운동을 찾을 수 없을 때 CustomException 발생")
    void testUploadExercisePictures_ExerciseNotFound() {
        // given
        Long testExerciseId = 1L;
        List<MultipartFile> testFiles = List.of(BDDMockito.mock(MultipartFile.class));
        BDDMockito.given(exerciseRepository.findById(testExerciseId)).willReturn(Optional.empty());

        // when and then
        assertThatThrownBy(() -> exercisePictureService.uploadExercisePictures(testExerciseId, testFiles))
                .as("운동을 찾을 수 없을 때 예외가 발생해야 함")
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.EXERCISE_NOT_FOUND);
    }

    @Test
    @DisplayName("운동에 대해 권한이 없을 때 CustomException 발생")
    void testUploadExercisePictures_AuthorizationDenied() {
        // given
        Long testExerciseId = 1L;
        UUID testUserId = UUID.randomUUID();
        UUID differentUserId = UUID.randomUUID();
        Member differentTestMember = Member.builder().memberId(differentUserId).build();
        Exercise testExercise = Exercise.builder().id(testExerciseId).member(differentTestMember).build();

        List<MultipartFile> testFiles = List.of(
                new MockMultipartFile("file1.jpg", "file1.jpg", MediaType.IMAGE_JPEG_VALUE, new byte[0]),
                new MockMultipartFile("file2.png", "file2.png", MediaType.IMAGE_PNG_VALUE, new byte[0])
        );

        try (var ignored = BDDMockito.mockStatic(UserContextHolder.class)) {
            BDDMockito.given(UserContextHolder.getUserId()).willReturn(testUserId);
            BDDMockito.given(exerciseRepository.findById(testExerciseId)).willReturn(Optional.of(testExercise));

            // when and then
            assertThatThrownBy(() -> exercisePictureService.uploadExercisePictures(testExerciseId, testFiles))
                    .as("권한이 없으면 예외가 발생해야 함")
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.AUTHORIZATION_DENIED);
        }
    }

    @Test
    @DisplayName("운동 사진 개수 제한 초과 시 CustomException 발생")
    void testUploadExercisePictures_PictureCountExceeded() {
        // given
        Long testExerciseId = 1L;
        UUID testUserId = UUID.randomUUID();
        Member testMember = Member.builder().memberId(testUserId).build();
        Exercise testExercise = Exercise.builder().id(testExerciseId).member(testMember).build();

        List<MultipartFile> testFiles = List.of(
                new MockMultipartFile("file1.jpg", "file1.jpg", MediaType.IMAGE_JPEG_VALUE, new byte[0]),
                new MockMultipartFile("file2.png", "file2.png", MediaType.IMAGE_PNG_VALUE, new byte[0]),
                new MockMultipartFile("file3.jpeg", "file2.jpeg", MediaType.IMAGE_JPEG_VALUE, new byte[0])
        );

        try (var ignored = BDDMockito.mockStatic(UserContextHolder.class)) {
            BDDMockito.given(UserContextHolder.getUserId()).willReturn(testUserId);
            BDDMockito.given(exerciseRepository.findById(testExerciseId)).willReturn(Optional.of(testExercise));
            BDDMockito.given(exercisePictureRepository.countByExercise_Id(testExerciseId)).willReturn(3L);

            // when and then
            assertThatThrownBy(() -> exercisePictureService.uploadExercisePictures(testExerciseId, testFiles))
                    .as("운동 사진 개수 제한 초과 시 예외가 발생해야 함")
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.EXERCISE_PICTURE_COUNT_EXCEEDED);
        }
    }

    @Test
    @DisplayName("DB 저장 중 예외 발생")
    void testUploadExercisePictures_DatabaseSaveException() {
        // given
        Long testExerciseId = 1L;
        UUID testUserId = UUID.randomUUID();
        Member testMember = Member.builder().memberId(testUserId).build();
        Exercise testExercise = Exercise.builder().id(testExerciseId).member(testMember).build();

        List<MultipartFile> testFiles = List.of(
                new MockMultipartFile("file1.jpg", "file1.jpg", MediaType.IMAGE_JPEG_VALUE, new byte[0]),
                new MockMultipartFile("file2.png", "file2.png", MediaType.IMAGE_PNG_VALUE, new byte[0]),
                new MockMultipartFile("file3.jpeg", "file2.jpeg", MediaType.IMAGE_JPEG_VALUE, new byte[0])
        );

        try (var ignored = BDDMockito.mockStatic(UserContextHolder.class)) {
            BDDMockito.given(UserContextHolder.getUserId()).willReturn(testUserId);
            BDDMockito.given(exerciseRepository.findById(testExerciseId)).willReturn(Optional.of(testExercise));
            BDDMockito.given(exercisePictureRepository.countByExercise_Id(testExerciseId)).willReturn(0L);
            BDDMockito.given(s3Service.generateObjectKey(BDDMockito.anyString(), BDDMockito.any())).willReturn("key");
            BDDMockito.given(s3Service.uploadImage(BDDMockito.any(), BDDMockito.anyString())).willReturn("url");
            BDDMockito.doThrow(new RuntimeException("Database error")).when(pictureRepository).saveAll(BDDMockito.any());

            // when and then
            assertThatThrownBy(() -> exercisePictureService.uploadExercisePictures(testExerciseId, testFiles))
                    .as("EXERCISE_PICTURE_UPLOAD_FAILED 예외가 발생해야 함")
                    .isInstanceOf(CustomException.class)
                    .hasMessage("운동 이미지 업로드에 실패했습니다.");

            BDDMockito.then(s3Service).should(BDDMockito.times(testFiles.size())).deleteImage(BDDMockito.anyString());
        }
    }

    @Test
    @DisplayName("S3 마지막 이미지 업로드 중 예외 발생")
    void testUploadExercisePictures_S3UploadException() {
        // given
        Long testExerciseId = 1L;
        UUID testUserId = UUID.randomUUID();
        Member testMember = Member.builder().memberId(testUserId).build();
        Exercise testExercise = Exercise.builder().id(testExerciseId).member(testMember).build();

        List<MultipartFile> testFiles = List.of(
                new MockMultipartFile("file1.jpg", "file1.jpg", MediaType.IMAGE_JPEG_VALUE, new byte[0]),
                new MockMultipartFile("file2.png", "file2.png", MediaType.IMAGE_PNG_VALUE, new byte[0]),
                new MockMultipartFile("file3.jpeg", "file2.jpeg", MediaType.IMAGE_JPEG_VALUE, new byte[0])
        );

        try (var ignored = BDDMockito.mockStatic(UserContextHolder.class)) {
            BDDMockito.given(UserContextHolder.getUserId()).willReturn(testUserId);
            BDDMockito.given(exerciseRepository.findById(testExerciseId)).willReturn(Optional.of(testExercise));
            BDDMockito.given(exercisePictureRepository.countByExercise_Id(testExerciseId)).willReturn(2L);
            BDDMockito.given(s3Service.generateObjectKey(BDDMockito.anyString(), BDDMockito.any())).willReturn("key");

            // s3Service.uploadImage 모의 설정 시작
            // 예외를 발생시키기 전 성공적으로 호출될 횟수
            int successCallCount = testFiles.size() - 1;
            BDDMockito.BDDMyOngoingStubbing<String> s3UploadStubbing =
                    BDDMockito.given(s3Service.uploadImage(BDDMockito.any(), BDDMockito.anyString()));

            // successCallCount 만큼 성공적인 반환값 설정
            for (int i = 0; i < successCallCount; ++i) {
                s3UploadStubbing = s3UploadStubbing.willReturn("url" + (i + 1));
            }

            // 다음 호출에서 예외 발생 설정
            s3UploadStubbing.willThrow(new S3UploadFailedException("S3 upload error"));

            // when and then
            assertThatThrownBy(() -> exercisePictureService.uploadExercisePictures(testExerciseId, testFiles))
                    .as("S3 업로드 중 예외가 발생해야 함")
                    .isInstanceOf(CustomException.class)
                    .hasMessage("운동 이미지 업로드에 실패했습니다.");
            BDDMockito.then(s3Service).should(BDDMockito.times(testFiles.size()))
                    .uploadImage(BDDMockito.any(), BDDMockito.anyString());
        }
    }
}