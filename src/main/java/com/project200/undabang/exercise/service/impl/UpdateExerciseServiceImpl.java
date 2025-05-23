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
import com.project200.undabang.exercise.dto.request.UpdateExerciseRequestDto;
import com.project200.undabang.exercise.dto.response.CreateExerciseResponseDto;
import com.project200.undabang.exercise.entity.Exercise;
import com.project200.undabang.exercise.entity.ExercisePicture;
import com.project200.undabang.exercise.repository.ExercisePictureRepository;
import com.project200.undabang.exercise.repository.ExerciseRepository;
import com.project200.undabang.exercise.service.UpdateExerciseService;
import com.project200.undabang.member.entity.Member;
import com.project200.undabang.member.repository.MemberRepository;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class UpdateExerciseServiceImpl implements UpdateExerciseService {

    private final MemberRepository memberRepository;
    private final S3Service s3Service;
    private final ExerciseRepository exerciseRepository;
    private final ExercisePictureRepository exercisePictureRepository;
    private final PictureRepository pictureRepository;

    @Override
    public CreateExerciseResponseDto updateExerciseImages(UpdateExerciseRequestDto requestDto) {
        Member member = memberRepository.findById(UserContextHolder.getUserId()).orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        checkMemberExerciseId(member.getMemberId(), requestDto.getExerciseId(), requestDto.getDeletePictureIdList());
        checkStartEndDate(requestDto.getExerciseStartedAt(), requestDto.getExerciseEndedAt());

        Exercise exercise = requestDto.toExerciseEntity(member);
        UpdateExerciseContext context =  new UpdateExerciseContext(exercise, requestDto.getDeletePictureIdList());

        // 사진은 수정 안하고 문자열 데이터 수정, 사진삭제하는 경우
        if(requestDto.getExercisePictureList().isEmpty()){
            exerciseRepository.save(context.getExercise());

            // 사진을 삭제만 하는 경우
            if (!context.getPictureIdListToDelete().isEmpty()) {
                try {
                    deletePictures(context.getPictureIdListToDelete());

                } catch (FileProcessingException | S3UploadFailedException e) {
                    handleS3AndFileException(context);
                } catch (Exception e) {
                    handleDBDeleteException(e, context);
                }
            }
            return new CreateExerciseResponseDto(context.getExercise().getId());
        }

        for (MultipartFile file : requestDto.getExercisePictureList()) {
            String objectKey = s3Service.generateObjectKey(file.getOriginalFilename(), FileType.EXERCISE);
            String imageUrl;

            try {
                //새로 저장된 사진 S3에 업로드
                imageUrl = s3Service.uploadImage(file, objectKey);

                Picture picture = Picture.of(file, imageUrl);
                context.addPicture(picture);

                ExercisePicture exercisePicture = ExercisePicture.builder()
                        .exercise(exercise)
                        .pictures(picture)
                        .build();
                context.addExercisePicture(exercisePicture);

                // 그 후 DB에 저장
                exerciseRepository.save(context.getExercise());
                exercisePictureRepository.saveAll(context.getExercisePictureList());
                pictureRepository.saveAll(context.getPictureList());

            } catch (FileProcessingException | S3UploadFailedException ex) {
                // 이미지 처리 또는 S3 업로드 중 예외 발생 시 S3 업로드 롤백
                handleS3AndFileException(context);
            } catch (Exception ex) {
                // DB 저장 중 예외 발생 시 S3 업로드 롤백
                handleDBSaveException(ex, context);
            }
        }

        // 삭제해야할 사진 처리
        if(!context.getPictureIdListToDelete().isEmpty()){
            try{
                deletePictures(context.getPictureIdListToDelete());

            }catch (FileProcessingException | S3UploadFailedException e){
                // s3에 파일 삭제시 실패하면 롤백 (S3에 올라간 사진 삭제)
                handleS3AndFileException(context);
            }catch (Exception e){
                // 디비 삭제시 에러 나면 처리 (S3에 올라간 사진 삭제)
                handleDBDeleteException(e, context);
            }
        }
        return new CreateExerciseResponseDto(context.getExercise().getId());
    }

    @Override
    public void deletePictures(List<Long> pictureIdList) throws FileProcessingException, S3UploadFailedException{
        if (pictureIdList == null || pictureIdList.isEmpty()) {
            return;
        }

        //  사진 테이블에서 soft delete 처리
        List<Picture> softDeletedPictures = pictureRepository.findAllById(pictureIdList);
        softDeletedPictures.forEach(Picture::softDelete);
        pictureRepository.saveAll(softDeletedPictures);

        //  S3에 존재하는 이미지 삭제
        for (Picture picture : softDeletedPictures) {
            s3Service.deleteImage(picture.getPictureUrl());
        }
    }

    /**
     * 수정된 운동 시작/끝 시간이 올바른지 확인
     */
    @Override
    public void checkStartEndDate(LocalDateTime startDate, LocalDateTime endDate) {
        if(startDate.isAfter(endDate) || startDate.isBefore(LocalDate.of(1945,8,15).atStartOfDay()) ||
                endDate.isAfter(LocalDate.now().plusDays(1).atStartOfDay())){
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }

    /**
     * 타인의 운동기록 혹은 사진 접근시 ACCESS DENIED 반환하도록 체크
     */
    @Override
    public void checkMemberExerciseId(UUID memberId, Long exerciseId, List<Long> pictureList) {
        if(!exerciseRepository.existsByRecordIdAndMemberId(memberId, exerciseId)){
            throw new CustomException(ErrorCode.AUTHORIZATION_DENIED);
        }
        List<ExercisePicture> exercisePictureList = exercisePictureRepository.findByExercise_Id(exerciseId);

        Set<Long> exercisePictureSet = new HashSet<>();
        for (ExercisePicture picture : exercisePictureList) {
            exercisePictureSet.add(picture.getId());
        }

        for (Long picture : pictureList) {
            if(!exercisePictureSet.contains(picture)){
                throw new CustomException(ErrorCode.AUTHORIZATION_DENIED);
            }
        }
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

    // 예외 처리 메서드들
    private void handleDBSaveException(Exception ex, UpdateExerciseServiceImpl.UpdateExerciseContext context) {
        log.error("DB 저장 중 예외 발생: {}", ex.getMessage(), ex);
        rollbackS3Upload(context.getPictureList());
        throw new CustomException(ErrorCode.EXERCISE_PICTURE_UPLOAD_FAILED);
    }

    private void handleS3AndFileException(UpdateExerciseServiceImpl.UpdateExerciseContext context) {
        rollbackS3Upload(context.getPictureList());
        throw new CustomException(ErrorCode.EXERCISE_PICTURE_UPLOAD_FAILED);
    }

    private void handleDBDeleteException(Exception ex, UpdateExerciseServiceImpl.UpdateExerciseContext context){
        log.error("DB 삭제 중 예외 발생: {}", ex.getMessage(), ex);
        // 기존에 저장된 사진들 삭제
        rollbackS3Upload(context.getPictureList());
        throw new CustomException(ErrorCode.EXERCISE_PICTURE_DELETE_FAILED);
    }

    @Getter
    public static class UpdateExerciseContext{
        private final Exercise exercise;

        private final List<Picture> pictureList = new ArrayList<>();

        private final List<ExercisePicture> exercisePictureList = new ArrayList<>();

        private final List<Long> pictureIdListToDelete;


        public UpdateExerciseContext(Exercise exercise, List<Long> pictureIdListToDelete){
            this.exercise = exercise;
            this.pictureIdListToDelete = pictureIdListToDelete != null ? pictureIdListToDelete : new ArrayList<>();
        }

        public void addPicture(Picture picture) {
            pictureList.add(picture);
        }

        public void addExercisePicture(ExercisePicture exercisePicture) {
            exercisePictureList.add(exercisePicture);
        }
    }
}
