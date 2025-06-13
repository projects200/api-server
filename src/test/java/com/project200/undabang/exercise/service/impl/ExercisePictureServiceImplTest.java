package com.project200.undabang.exercise.service.impl;

import com.project200.undabang.common.context.UserContextHolder;
import com.project200.undabang.common.entity.Picture;
import com.project200.undabang.common.repository.PictureRepository;
import com.project200.undabang.common.service.S3Service;
import com.project200.undabang.common.web.exception.CustomException;
import com.project200.undabang.common.web.exception.ErrorCode;
import com.project200.undabang.common.web.exception.S3UploadFailedException;
import com.project200.undabang.exercise.dto.response.ExerciseIdResponseDto;
import com.project200.undabang.exercise.entity.Exercise;
import com.project200.undabang.exercise.entity.ExercisePicture;
import com.project200.undabang.exercise.repository.ExercisePictureRepository;
import com.project200.undabang.exercise.repository.ExerciseRepository;
import com.project200.undabang.member.entity.Member;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collections;
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
            BDDMockito.given(exercisePictureRepository.countNotDeletedPicturesByExerciseId(testExerciseId)).willReturn(0L);
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
            BDDMockito.given(exercisePictureRepository.countNotDeletedPicturesByExerciseId(testExerciseId)).willReturn(3L);

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
            BDDMockito.given(exercisePictureRepository.countNotDeletedPicturesByExerciseId(testExerciseId)).willReturn(0L);
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
            BDDMockito.given(exercisePictureRepository.countNotDeletedPicturesByExerciseId(testExerciseId)).willReturn(2L);
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

            // 다음 호출에서 예외 발생 설정4스
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

    @Test
    @DisplayName("운동기록 이미지 삭제 _ 성공")
    void deleteExerciseImages() {
        // given
        UUID testMemberId = UUID.randomUUID();
        Long testExerciseId = 1L;

        Member testMember = Member.builder().memberId(testMemberId).build();
        Exercise testExercise = Exercise.builder().id(testExerciseId).build();

        Picture picture1 = Picture.builder().id(1L).pictureUrl("s3://bucket/uploads/key1.png").build();
        Picture picture2 = Picture.builder().id(2L).pictureUrl("s3://bucket/uploads/key2.png").build();
        Picture picture3 = Picture.builder().id(3L).pictureUrl("s3://bucket/uploads/key3.png").build();

        ExercisePicture exercisePicture1 = ExercisePicture.builder().id(picture1.getId()).picture(picture1).exercise(testExercise).build();
        ExercisePicture exercisePicture2 = ExercisePicture.builder().id(picture2.getId()).picture(picture2).exercise(testExercise).build();
        ExercisePicture exercisePicture3 = ExercisePicture.builder().id(picture3.getId()).picture(picture3).exercise(testExercise).build();

        List<Long> testDeleteIdList = List.of(1L, 2L, 3L);
        List<ExercisePicture> exercisePicturesForAuthCheck = List.of(exercisePicture1, exercisePicture2, exercisePicture3);
        List<ExercisePicture> exercisePicturesToDelete = List.of(exercisePicture1, exercisePicture2, exercisePicture3);

        BDDMockito.given(exerciseRepository.existsByRecordIdAndMemberId(testMemberId, testExerciseId)).willReturn(true);
        BDDMockito.given(exercisePictureRepository.findAllByExercise_Id(testExerciseId)).willReturn(exercisePicturesForAuthCheck);

        BDDMockito.given(exercisePictureRepository.findAllById(testDeleteIdList)).willReturn(exercisePicturesToDelete);
        BDDMockito.given(s3Service.extractObjectKeyFromUrl(picture1.getPictureUrl())).willReturn("key1.png");
        BDDMockito.given(s3Service.extractObjectKeyFromUrl(picture2.getPictureUrl())).willReturn("key2.png");
        BDDMockito.given(s3Service.extractObjectKeyFromUrl(picture3.getPictureUrl())).willReturn("key3.png");
        // s3service.deleteImage() 가 호출될 때, 실제로 호출되지 않도록 구현
        // 반환값이 없는데 Stubbing을 쓰는게 맞는지 모르겠음
        BDDMockito.willDoNothing().given(s3Service).deleteImage(BDDMockito.anyString());

        //when
        exercisePictureService.deleteExercisePictures(testMemberId, testExerciseId, testDeleteIdList);

        //then
        BDDMockito.then(s3Service).should(BDDMockito.times(1)).deleteImage("key1.png");
        BDDMockito.then(s3Service).should(BDDMockito.times(1)).deleteImage("key2.png");
        BDDMockito.then(s3Service).should(BDDMockito.times(1)).deleteImage("key3.png");

        // softdelete 진행됬어야함
        Assertions.assertThat(picture1.getPictureDeletedAt()).isNotNull();
        Assertions.assertThat(picture2.getPictureDeletedAt()).isNotNull();
        Assertions.assertThat(picture3.getPictureDeletedAt()).isNotNull();

        BDDMockito.then(exercisePictureRepository).should(BDDMockito.never()).saveAll(BDDMockito.anyList()); // softDelete는 saveAll 호출 안함
        BDDMockito.then(exercisePictureRepository).should(BDDMockito.never()).save(BDDMockito.any()); // softDelete는 명시적으로 Save()를 호출하지 않음
    }

    @Test
    @DisplayName("운동기록 이미지 삭제 _ 자신의 운동기록이 아닌경우")
    void deleteExerciseImage_FailedNotOwner() {
        // given
        UUID testMemberId = UUID.randomUUID();
        Long testExerciseId = 1L;
        List<Long> pictureIdDeleteList = List.of(1L, 2L, 3L);

        BDDMockito.given(exerciseRepository.existsByRecordIdAndMemberId(testMemberId, testExerciseId)).willReturn(false);

        // when, then
        Assertions.assertThatThrownBy(() ->
                        exercisePictureService.deleteExercisePictures(testMemberId, testExerciseId, pictureIdDeleteList))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.AUTHORIZATION_DENIED);

        // 그 후 메소드 들은 수행되면 안됨
        BDDMockito.then(exercisePictureRepository).should(BDDMockito.never()).findAllByExercise_Id(BDDMockito.anyLong());
        BDDMockito.then(exercisePictureRepository).should(BDDMockito.never()).findAllById(BDDMockito.anyList());
        BDDMockito.then(s3Service).should(BDDMockito.never()).deleteImage(BDDMockito.anyString());
    }

    @Test
    @DisplayName("운동기록 이미지 삭제 _ 사진이 해당 운동 기록에 속하지 않는 경우")
    void deleteExerciseImage_FailedPictureNotBelongsToExercise() {
        // given
        UUID testMemberId = UUID.randomUUID();
        Long testExerciseId = 1L;
        List<Long> testDeletePictureIdList = List.of(1L); // 입력받은 (삭제가 필요한) PictureId
        Long actualPictureId = 2L;   // 실제 운동 기록에 있는 Picture ID

        Member member = Member.builder().memberId(testMemberId).build();
        Exercise exercise = Exercise.builder().id(testExerciseId).member(member).build();

        Picture picture = Picture.builder().id(actualPictureId).pictureUrl("s3://bucket/uploads/actual_key").build();
        ExercisePicture exercisePicture = ExercisePicture.builder()
                .id(actualPictureId) // ID는 Picture의 ID와 동일
                .exercise(exercise)
                .picture(picture)
                .build();

        BDDMockito.given(exerciseRepository.existsByRecordIdAndMemberId(testMemberId, testExerciseId)).willReturn(true);
        // 입력받은 pictureId가 아니라 실제 Id반환. 따라서 에러가 발생해야 함
        BDDMockito.given(exercisePictureRepository.findAllByExercise_Id(testExerciseId)).willReturn(List.of(exercisePicture));

        //when then
        Assertions.assertThatThrownBy(() ->
                        exercisePictureService.deleteExercisePictures(testMemberId, testExerciseId, testDeletePictureIdList))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.AUTHORIZATION_DENIED);

        BDDMockito.then(exercisePictureRepository).should(BDDMockito.never()).findAllById(BDDMockito.anyList());
        BDDMockito.then(s3Service).should(BDDMockito.never()).deleteImage(BDDMockito.anyString());
    }

    @Test
    @DisplayName("운동기록 이미지 삭제 _ S3에 있는 이미지 삭제 실패")
    void deleteExerciseImage_FailedToDeletePicturesInS3() {
        // given
        UUID testMemberId = UUID.randomUUID();
        Long testExerciseId = 1L;

        Member member = Member.builder().memberId(testMemberId).build();
        Exercise exercise = Exercise.builder().id(testExerciseId).member(member).build();

        Picture picture = Picture.builder().id(1L).pictureUrl("s3://bucket/uploads/key1.png").build();
        ExercisePicture exercisePicture = ExercisePicture.builder().id(picture.getId()).picture(picture).exercise(exercise).build();
        List<Long> testDeletePictureList = List.of(exercisePicture.getId());

        BDDMockito.given(exerciseRepository.existsByRecordIdAndMemberId(testMemberId, testExerciseId)).willReturn(true);
        BDDMockito.given(exercisePictureRepository.findAllByExercise_Id(testExerciseId)).willReturn(List.of(exercisePicture));
        BDDMockito.given(exercisePictureRepository.findAllById(testDeletePictureList)).willReturn(List.of(exercisePicture));
        BDDMockito.given(s3Service.extractObjectKeyFromUrl(picture.getPictureUrl())).willReturn("key1.png");

        // S3 삭제시 S3UploadFailedException 발생
        BDDMockito.willThrow(new S3UploadFailedException("S3 Exception")).given(s3Service).deleteImage("key1.png");

        Assertions.assertThatThrownBy(() ->
                        exercisePictureService.deleteExercisePictures(testMemberId, testExerciseId, testDeletePictureList))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.EXERCISE_PICTURE_DELETE_FAILED);

        BDDMockito.then(s3Service).should(BDDMockito.times(1)).deleteImage("key1.png");
        // DB 저장된 softDelete 내용 롤백되었는지 확인
        Assertions.assertThat(picture.getPictureDeletedAt()).isNull();
    }

    @Test
    @DisplayName("운동기록과 연계된 이미지 조회 성공케이스")
    void getAllImagesFromExercise() {
        UUID testMemberId = UUID.randomUUID();
        Long testExerciseId = 1L;

        Member member = Member.builder().memberId(testMemberId).build();
        Exercise exercise = Exercise.builder().id(testExerciseId).member(member).build();

        Picture picture1 = Picture.builder().id(1L).pictureUrl("s3://bucket/uploads/key1.png").build();
        Picture picture2 = Picture.builder().id(1L).pictureUrl("s3://bucket/uploads/key2.png").build();

        ExercisePicture exercisePicture1 = ExercisePicture.builder()
                .id(picture1.getId())
                .exercise(exercise)
                .picture(picture1)
                .build();

        ExercisePicture exercisePicture2 = ExercisePicture.builder()
                .id(picture2.getId())
                .exercise(exercise)
                .picture(picture2)
                .build();

        List<ExercisePicture> testExercisePictures = List.of(exercisePicture1, exercisePicture2);
        List<Long> testPictureIds = testExercisePictures.stream().map(ExercisePicture::getId).toList();

        BDDMockito.given(exerciseRepository.findById(testExerciseId)).willReturn(Optional.of(exercise));
        BDDMockito.given(exercisePictureRepository.findAllByExercise_Id(testExerciseId)).willReturn(testExercisePictures);

        // when
        List<Long> pictureIds = exercisePictureService.getAllImagesFromExercise(testMemberId, testExerciseId);

        // then
        Assertions.assertThat(pictureIds.size()).isEqualTo(testPictureIds.size());
        Assertions.assertThat(pictureIds.containsAll(testExercisePictures));

        BDDMockito.then(exerciseRepository).should(BDDMockito.times(1)).findById(testExerciseId);
        BDDMockito.then(exercisePictureRepository).should(BDDMockito.times(1)).findAllByExercise_Id(testExerciseId);
    }

    @Test
    @DisplayName("운동기록과 연계된 이미지 조회 실패 _ 운동기록 없음")
    void getAllImagesFromExercise_NoExerciseExist() {
        UUID testMemberId = UUID.randomUUID();
        Long testExerciseId = 1L;

        BDDMockito.given(exerciseRepository.findById(testExerciseId)).willThrow(new CustomException(ErrorCode.EXERCISE_NOT_FOUND));

        Assertions.assertThatThrownBy(() ->
                        exercisePictureService.getAllImagesFromExercise(testMemberId, testExerciseId))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.EXERCISE_NOT_FOUND);

        BDDMockito.then(exercisePictureRepository).should(BDDMockito.never()).findAllByExercise_Id(testExerciseId);
    }

    @Test
    @DisplayName("운동기록과 연계된 이미지 조회 실패 _  운동 기록이 회원의 것이 아닌 경우")
    void getAllImagesFromExercise_NotRelatedMemberExercise() {
        // given
        UUID testMemberId = UUID.randomUUID();
        Long testExerciseId = 1L;

        Member member = Member.builder().memberId(testMemberId).build();
        Exercise exercise = Exercise.builder().id(testExerciseId).member(member).build();

        BDDMockito.given(exerciseRepository.findById(testExerciseId)).willReturn(Optional.of(exercise));

        // 운동기록과 연관되지 않은
        Assertions.assertThatThrownBy(() ->
                        exercisePictureService.getAllImagesFromExercise(UUID.randomUUID(), testExerciseId))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.AUTHORIZATION_DENIED);

        BDDMockito.then(exerciseRepository).should(BDDMockito.times(1)).findById(testExerciseId);
        BDDMockito.then(exercisePictureRepository).should(BDDMockito.never()).findAllByExercise_Id(testExerciseId);
    }

    @Test
    @DisplayName("운동기록과 연계된 이미지 조회 실패 _ 운동 기록이 없는 경우 빈 리스트 발생")
    void getAllImagesFromExercise_SucceedNoExercisePicture() {
        // given
        UUID testMemberId = UUID.randomUUID();
        Long testExerciseId = 1L;

        Member member = Member.builder().memberId(testMemberId).build();
        Exercise exercise = Exercise.builder().id(testExerciseId).member(member).build();

        BDDMockito.given(exerciseRepository.findById(testExerciseId)).willReturn(Optional.of(exercise));
        // 빈 리스트 반환
        BDDMockito.given(exercisePictureRepository.findAllByExercise_Id(testExerciseId)).willReturn(Collections.emptyList());

        // when
        List<Long> testPictureIds = exercisePictureService.getAllImagesFromExercise(testMemberId, testExerciseId);

        // then
        Assertions.assertThat(testPictureIds).isNotNull();
        Assertions.assertThat(testPictureIds).isEmpty();

        BDDMockito.then(exerciseRepository).should(BDDMockito.times(1)).findById(testExerciseId);
        BDDMockito.then(exercisePictureRepository).should(BDDMockito.times(1)).findAllByExercise_Id(testExerciseId);

    }

    @Nested
    class test_for_image_upload_error {
        @Test
        @DisplayName("운동 사진 관리 - 사진 3개 추가, 2개 삭제, 3개 추가 시나리오")
            // 처음 작성시에는 디비를 모킹으로 넣어주었으므로 에러가 안남(확인됨)
            // 함수 수정으로 인한 정상 동작 확인
        void manageExerciseImages_AddDeleteAddScenario() {
            // given
            UUID testMemberId = UUID.randomUUID();
            Long testExerciseId = 1L;
            Member member = Member.builder().memberId(testMemberId).build();
            Exercise exercise = Exercise.builder().id(testExerciseId).member(member).build();

            // 처음 업로드할 사진 3개
            List<MultipartFile> initialFiles = List.of(
                    new MockMultipartFile("file1", "file1.jpg", MediaType.IMAGE_JPEG_VALUE, "test1".getBytes()),
                    new MockMultipartFile("file2", "file2.jpg", MediaType.IMAGE_JPEG_VALUE, "test2".getBytes()),
                    new MockMultipartFile("file3", "file3.jpg", MediaType.IMAGE_JPEG_VALUE, "test3".getBytes())
            );

            // 삭제할 사진 ID 목록
            List<Long> pictureIdsToDelete = List.of(1L, 2L);

            // 나중에 추가할 사진 3개
            List<MultipartFile> additionalFiles = List.of(
                    new MockMultipartFile("file4", "file4.jpg", MediaType.IMAGE_JPEG_VALUE, "test4".getBytes()),
                    new MockMultipartFile("file5", "file5.jpg", MediaType.IMAGE_JPEG_VALUE, "test5".getBytes()),
                    new MockMultipartFile("file6", "file6.jpg", MediaType.IMAGE_JPEG_VALUE, "test6".getBytes())
            );

            try (MockedStatic<UserContextHolder> mockedStatic = Mockito.mockStatic(UserContextHolder.class)) {
                mockedStatic.when(UserContextHolder::getUserId).thenReturn(testMemberId);
                BDDMockito.given(exerciseRepository.findById(testExerciseId)).willReturn(Optional.of(exercise));

                BDDMockito.given(exercisePictureRepository.countNotDeletedPicturesByExerciseId(testExerciseId))
                        .willReturn(0L) // 맨 처음 조회시 디비엔 아무것도 없으므로 0 반환
                        .willReturn(1L);// 사진 3개 삽입 후 2개 삭제했으므로 1 반환

                // S3 업로드 설정
                BDDMockito.given(s3Service.generateObjectKey(BDDMockito.anyString(), BDDMockito.any())).willReturn("key");
                BDDMockito.given(s3Service.uploadImage(BDDMockito.any(), BDDMockito.anyString())).willReturn("url");

                // 2단계: 사진 2개 삭제 준비
                BDDMockito.given(exerciseRepository.existsByRecordIdAndMemberId(testMemberId, testExerciseId)).willReturn(true);

                Picture picture1 = Picture.builder().id(1L).pictureUrl("url1").build();
                Picture picture2 = Picture.builder().id(2L).pictureUrl("url2").build();
                ExercisePicture exercisePicture1 = ExercisePicture.builder().id(1L).exercise(exercise).picture(picture1).build();
                ExercisePicture exercisePicture2 = ExercisePicture.builder().id(2L).exercise(exercise).picture(picture2).build();

                List<ExercisePicture> exercisePictureList = List.of(exercisePicture1, exercisePicture2);

                BDDMockito.given(exercisePictureRepository.findAllByExercise_Id(testExerciseId)).willReturn(exercisePictureList);
                BDDMockito.given(exercisePictureRepository.findAllById(pictureIdsToDelete)).willReturn(exercisePictureList);

                BDDMockito.given(s3Service.extractObjectKeyFromUrl(BDDMockito.anyString())).willReturn("key");
                BDDMockito.willDoNothing().given(s3Service).deleteImage(BDDMockito.anyString());

                // when
                // 사진 3개 추가
                exercisePictureService.uploadExercisePictures(testExerciseId, initialFiles);

                // 사진 2개 삭제
                exercisePictureService.deleteExercisePictures(testMemberId, testExerciseId, pictureIdsToDelete);

                // 사진 3개 추가
                ExerciseIdResponseDto uploadResult = exercisePictureService.uploadExercisePictures(testExerciseId, additionalFiles);

                // then
                Assertions.assertThat(uploadResult.exerciseId()).isEqualTo(testExerciseId);
                // 내가 작성한 함수가 두번 호출되었는지 확인
                BDDMockito.then(exercisePictureRepository).should(BDDMockito.times(2)).countNotDeletedPicturesByExerciseId(testExerciseId);
            }
        }
    }
}