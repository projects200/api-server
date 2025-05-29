package com.project200.undabang.exercise.service.impl;

import com.project200.undabang.common.context.UserContextHolder;
import com.project200.undabang.common.entity.Picture;
import com.project200.undabang.common.repository.PictureRepository;
import com.project200.undabang.common.service.FileType;
import com.project200.undabang.common.service.S3Service;
import com.project200.undabang.common.web.exception.CustomException;
import com.project200.undabang.common.web.exception.ErrorCode;
import com.project200.undabang.common.web.exception.FileProcessingException;
import com.project200.undabang.common.web.exception.S3UploadFailedException;
import com.project200.undabang.exercise.dto.response.ExerciseIdResponseDto;
import com.project200.undabang.exercise.entity.Exercise;
import com.project200.undabang.exercise.entity.ExercisePicture;
import com.project200.undabang.exercise.repository.ExercisePictureRepository;
import com.project200.undabang.exercise.repository.ExerciseRepository;
import com.project200.undabang.exercise.service.ExercisePictureService;
import io.awspring.cloud.s3.S3Exception;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.exception.SdkException;

import java.util.*;

@Validated
@Slf4j
@RequiredArgsConstructor
@Service
public class ExercisePictureServiceImpl implements ExercisePictureService {

    private final ExerciseRepository exerciseRepository;
    private final PictureRepository pictureRepository;
    private final ExercisePictureRepository exercisePictureRepository;
    private final S3Service s3Service;

    @Override
    @Transactional
    public ExerciseIdResponseDto uploadExercisePictures(Long exerciseId, List<MultipartFile> exercisePictureList) throws CustomException {
        // 운동 ID로 운동 엔티티 조회
        // 운동이 존재하지 않으면 예외 발생
        Exercise exercise = exerciseRepository.findById(exerciseId)
                .orElseThrow(() -> new CustomException(ErrorCode.EXERCISE_NOT_FOUND));

        // 권한 검증
        if (!exercise.isOwnedBy(UserContextHolder.getUserId())) {
            throw new CustomException(ErrorCode.AUTHORIZATION_DENIED);
        }

        // 운동 이미지와 관련된 데이터를 처리하기 위한 컨텍스트 객체 생성
        CreateExerciseContext context = new CreateExerciseContext(exercise);

        try {
            // 운동 이미지 파일 리스트를 S3에 업로드하고 Picture, ExercisePicture 엔티티 생성
            processImages(exercisePictureList, context);

            // DB에 이미지 정보(Picture, ExercisePicture 엔티티)를 저장
            saveToDatabase(context);

            return new ExerciseIdResponseDto(exercise.getId());
        } catch (FileProcessingException | S3UploadFailedException ex) {
            // 이미지 처리 또는 S3 업로드 중 예외 발생 시 S3 업로드 롤백
            handleS3AndFileException(context);
        } catch (Exception ex) {
            // DB 저장 중 예외 발생 시 S3 업로드 롤백
            handleDBSaveException(ex, context);
        }
        return null; // 이 줄은 실제로 도달하지 않음
    }

    @Override
    @Transactional
    public List<Long> getAllImagesFromExercise(UUID memberId, Long exerciseId) {
        Exercise exercise = exerciseRepository.findById(exerciseId).orElseThrow(() -> new CustomException(ErrorCode.EXERCISE_NOT_FOUND));

        if(!exercise.isOwnedBy(memberId)){
            throw new CustomException(ErrorCode.AUTHORIZATION_DENIED);
        }

        return exercisePictureRepository.findAllByExercise_Id(exerciseId).stream().map(ExercisePicture::getId).toList();
    }

    @Override
    @Transactional
    public void deleteExercisePictures(UUID memberId, Long exerciseId, List<Long> deletePictureIdList) {
        checkMemberExerciseId(memberId, exerciseId, deletePictureIdList);

        List<ExercisePicture> exercisePictureList = exercisePictureRepository.findAllById(deletePictureIdList);
        List<Picture> pictureListForDelete = new ArrayList<>();

        for (ExercisePicture picture : exercisePictureList) {
            pictureListForDelete.add(picture.getPicture());
        }

        try {
            // s3, db에서 데이터 삭제 시도
            deletePictures(pictureListForDelete);

        } catch (S3Exception | SdkException ex) {
            // 클라이언트 레벨 에러 및 Sdk 에러 처리
            log.error("S3 Exception: {}", ex.getMessage());

        } catch (Exception e) {
            log.error("이미지 삭제 처리중 에러 발생 {}", e.getMessage(), e);
            throw new CustomException(ErrorCode.EXERCISE_PICTURE_DELETE_FAILED);
        }
    }

    private void deletePictures(List<Picture> pictureList) throws S3UploadFailedException{
        for (Picture picture : pictureList) {
            // S3에서 데이터 삭제
            String s3Url = s3Service.extractObjectKeyFromUrl(picture.getPictureUrl());
            s3Service.deleteImage(s3Url);
        }

        for (Picture picture : pictureList) {
            // 그 후 db에서 soft delete 진행
            picture.softDelete();
        }
    }

    // 운동 이미지 파일 리스트를 S3에 업로드하고 Picture, ExercisePicture 엔티티를 생성하는 메서드
    private void processImages(List<MultipartFile> fileList, CreateExerciseContext context)
            throws FileProcessingException, S3UploadFailedException {
        for (MultipartFile file : fileList) {
            String imageUrl = uploadImageToS3(file);

            // S3에 업로드된 이미지 URL을 사용하여 Picture 엔티티 생성
            Picture picture = Picture.of(file, imageUrl);
            context.addPicture(picture);

            // ExercisePicture 엔티티 생성
            ExercisePicture exercisePicture = ExercisePicture.builder()
                    .exercise(context.getExercise())
                    .picture(picture)
                    .build();
            context.addExercisePicture(exercisePicture);
        }
    }

    // S3에 이미지를 업로드하는 메서드
    private String uploadImageToS3(@NotNull MultipartFile file)
            throws FileProcessingException, S3UploadFailedException {
        // S3에 업로드할 객체 키 생성
        String objectKey = s3Service.generateObjectKey(file.getOriginalFilename(), FileType.EXERCISE);

        // S3에 업로드 요청 생성
        return s3Service.uploadImage(file, objectKey);
    }

    // DB에 이미지 정보를 저장하는 메서드
    private void saveToDatabase(CreateExerciseContext context) {
        pictureRepository.saveAll(context.getPictureList());
        exercisePictureRepository.saveAll(context.getExercisePictureList());
    }

    // 예외 발생 시 S3에서 이미지를 삭제하는 메서드
    private void rollbackS3Upload(List<Picture> pictureList) {
        for (Picture picture : pictureList) {
            try {
                s3Service.deleteImage(picture.getPictureUrl());
            } catch (S3UploadFailedException ignored) {
                // 예외는 무시하고 다음 이미지 삭제 시도
            }
        }
    }

    // 컨텍스트 클래스로 관련 데이터를 그룹화
    @Getter
    private static class CreateExerciseContext {
        @NotNull
        private final Exercise exercise;

        private final List<Picture> pictureList = new ArrayList<>();

        private final List<ExercisePicture> exercisePictureList = new ArrayList<>();

        public CreateExerciseContext(Exercise exercise) {
            this.exercise = exercise;
        }

        public void addPicture(Picture picture) {
            pictureList.add(picture);
        }

        public void addExercisePicture(ExercisePicture exercisePicture) {
            exercisePictureList.add(exercisePicture);
        }
    }

    // 예외 처리 메서드들
    private void handleDBSaveException(Exception ex, CreateExerciseContext context) {
        log.error("DB 저장 중 예외 발생: {}", ex.getMessage(), ex);
        rollbackS3Upload(context.getPictureList());
        throw new CustomException(ErrorCode.EXERCISE_PICTURE_UPLOAD_FAILED);
    }

    private void handleS3AndFileException(CreateExerciseContext context) {
        rollbackS3Upload(context.getPictureList());
        throw new CustomException(ErrorCode.EXERCISE_PICTURE_UPLOAD_FAILED);
    }

    private void checkMemberExerciseId(UUID memberId, Long exerciseId, List<Long> pictureIdDeleteList) {
        if (!exerciseRepository.existsByRecordIdAndMemberId(memberId, exerciseId)) {
            throw new CustomException(ErrorCode.AUTHORIZATION_DENIED);
        }

        if (pictureIdDeleteList == null || pictureIdDeleteList.isEmpty()) {
            return;
        }

        // 삭제하려는 사진이 자신의 운동 기록에 있는지 검증
        List<ExercisePicture> exercisePictureList = exercisePictureRepository.findAllByExercise_Id(exerciseId);
        Set<Long> exercisePictureSet = new HashSet<>();

        // 내용이 있을때만 해당 기능 동작
        // 저장된 사진이 없으면 추가만 되도록
        for (ExercisePicture picture : exercisePictureList) {
            exercisePictureSet.add(picture.getId());
        }

        for (Long picture : pictureIdDeleteList) {
            if (!exercisePictureSet.contains(picture)) {
                throw new CustomException(ErrorCode.AUTHORIZATION_DENIED);
            }
        }

    }
}
