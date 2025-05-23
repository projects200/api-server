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
import com.project200.undabang.exercise.dto.request.CreateExerciseRequestDto;
import com.project200.undabang.exercise.dto.response.CreateExerciseResponseDto;
import com.project200.undabang.exercise.entity.Exercise;
import com.project200.undabang.exercise.entity.ExercisePicture;
import com.project200.undabang.exercise.repository.ExercisePictureRepository;
import com.project200.undabang.exercise.repository.ExerciseRepository;
import com.project200.undabang.exercise.service.CreateExerciseService;
import com.project200.undabang.member.entity.Member;
import com.project200.undabang.member.repository.MemberRepository;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@Validated
@Slf4j
@RequiredArgsConstructor
@Transactional
@Service
public class CreateExerciseServiceImpl implements CreateExerciseService {

    private final ExerciseRepository exerciseRepository;
    private final MemberRepository memberRepository;
    private final PictureRepository pictureRepository;
    private final ExercisePictureRepository exercisePictureRepository;
    private final S3Service s3Service;

    /**
     * 주어진 운동 이미지 데이터를 처리하고 저장한 후, 운동 데이터의 식별자를 반환하는 메서드.
     *
     * @param requestDto 운동 이미지와 관련 데이터를 포함하는 요청 DTO
     * @return 처리된 운동 데이터의 식별자를 포함한 응답 DTO
     * @throws CustomException 운동 이미지 업로드 또는 데이터 저장 실패 시 예외를 발생
     */
    @Override
    public CreateExerciseResponseDto uploadExerciseImages(CreateExerciseRequestDto requestDto) {
        Member member = findMember();
        Exercise exercise = requestDto.toEntity(member);

        // 운동 이미지와 관련된 데이터를 처리하기 위한 컨텍스트 객체 생성
        CreateExerciseContext context = new CreateExerciseContext(exercise);

        try {
            // 운동 이미지 파일 리스트를 처리
            processImages(requestDto.exercisePictureList(), context);

            // DB에 운동 기록과 이미지 정보를 저장
            saveToDatabase(context);

            return new CreateExerciseResponseDto(exercise.getId());
        } catch (FileProcessingException | S3UploadFailedException ex) {
            // 이미지 처리 또는 S3 업로드 중 예외 발생 시 S3 업로드 롤백
            handleS3AndFileException(context);
        } catch (Exception ex) {
            // DB 저장 중 예외 발생 시 S3 업로드 롤백
            handleDBSaveException(ex, context);
        }
        return null; // 이 줄은 실제로 도달하지 않음
    }

    // 회원 정보를 조회하고 검증하는 메서드
    private Member findMember() {
        return memberRepository.findById(UserContextHolder.getUserId())
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));
    }

    // 이미지 파일 리스트를 처리하는 메서드
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
                    .pictures(picture)
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

    // DB에 운동 기록과 이미지 정보를 저장하는 메서드
    private void saveToDatabase(CreateExerciseContext context) {
        exerciseRepository.save(context.getExercise());
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
}
