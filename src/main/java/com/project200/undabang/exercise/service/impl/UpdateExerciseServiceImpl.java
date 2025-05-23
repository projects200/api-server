package com.project200.undabang.exercise.service.impl;

import com.project200.undabang.common.context.UserContextHolder;
import com.project200.undabang.common.entity.Picture;
import com.project200.undabang.common.repository.PictureRepository;
import com.project200.undabang.common.service.FileType;
import com.project200.undabang.common.service.S3Service;
import com.project200.undabang.common.web.exception.CustomException;
import com.project200.undabang.common.web.exception.ErrorCode;
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

        List<MultipartFile> fileList = requestDto.getExercisePictureList();

        List<Long> prevPictureList = requestDto.getCurrentPictureIdList();
        List<Long> deletePictureList = requestDto.getDeletePictureIdList();

        List<Picture> pictureList = new ArrayList<>();
        List<ExercisePicture> exercisePictureList = new ArrayList<>();


        for (int i = 0; i < fileList.size(); i++) {
            MultipartFile newImage = fileList.get(0);
            String objectKey = s3Service.generateObjectKey(newImage.getOriginalFilename(), FileType.EXERCISE);
            String imageUrl;
            try {
                imageUrl = s3Service.uploadImage(newImage, objectKey);
            } catch (Exception e) { // 이거아님
                throw new CustomException(ErrorCode.AUTHORIZATION_DENIED);
            }

            Picture picture = Picture.of(newImage, imageUrl);
            pictureList.add(picture);

            ExercisePicture exercisePicture = ExercisePicture.builder()
                    .exercise(exercise)
                    .pictures(picture)
                    .build();
            exercisePictureList.add(exercisePicture);
        }

        try {
            exerciseRepository.save(exercise);
            exercisePictureRepository.saveAll(exercisePictureList);
            pictureRepository.saveAll(pictureList);
        }catch (Exception e){
            throw new CustomException(ErrorCode.AUTHORIZATION_DENIED);//이거아님
        }


        return null;
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

    @Getter
    public static class UpdateExerciseContext{
        private final Exercise exercise;

        private final List<Picture> pictureList = new ArrayList<>();

        private final List<ExercisePicture> exercisePictureList = new ArrayList<>();

        private final List<Long> pictureIdListToDelete;

        private final List<Long> currentPictureIdList;

        public UpdateExerciseContext(Exercise exercise, List<Long> pictureIdListToDelete, List<Long> currentPictureIdList){
            this.exercise = exercise;
            this.pictureIdListToDelete = pictureIdListToDelete != null ? pictureIdListToDelete : new ArrayList<>();
            this.currentPictureIdList = currentPictureIdList != null ? currentPictureIdList : new ArrayList<>();
        }

        public UpdateExerciseContext(Exercise exercise) {
            this.exercise = exercise;
            this.pictureIdListToDelete = new ArrayList<>();
            this.currentPictureIdList = new ArrayList<>();
        }

        public void addPicture(Picture picture) {
            pictureList.add(picture);
        }

        public void addExercisePicture(ExercisePicture exercisePicture) {
            exercisePictureList.add(exercisePicture);
        }
    }
}
