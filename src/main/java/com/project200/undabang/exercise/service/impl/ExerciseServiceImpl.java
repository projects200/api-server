package com.project200.undabang.exercise.service.impl;

import com.project200.undabang.common.context.UserContextHolder;
import com.project200.undabang.common.entity.Picture;
import com.project200.undabang.common.repository.PictureRepository;
import com.project200.undabang.common.service.FileType;
import com.project200.undabang.common.service.S3Service;
import com.project200.undabang.common.web.exception.CustomException;
import com.project200.undabang.common.web.exception.ErrorCode;
import com.project200.undabang.exercise.dto.request.CreateExerciseRequestDto;
import com.project200.undabang.exercise.dto.request.UpdateExerciseRequestDto;
import com.project200.undabang.exercise.dto.response.CreateExerciseResponseDto;
import com.project200.undabang.exercise.entity.Exercise;
import com.project200.undabang.exercise.entity.ExercisePicture;
import com.project200.undabang.exercise.repository.ExercisePictureRepository;
import com.project200.undabang.exercise.repository.ExerciseRepository;
import com.project200.undabang.exercise.service.ExerciseService;
import com.project200.undabang.member.entity.Member;
import com.project200.undabang.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@RequiredArgsConstructor
@Service
@Transactional
public class ExerciseServiceImpl implements ExerciseService {

    private final ExerciseRepository exerciseRepository;
    private final MemberRepository memberRepository;
    private final PictureRepository pictureRepository;
    private final ExercisePictureRepository exercisePictureRepository;
    private final S3Service s3Service;

    @Override
    public CreateExerciseResponseDto uploadExerciseImages(CreateExerciseRequestDto requestDto) {

        // memberId를 통해 회원 정보 조회 및 검증
        Member member = memberRepository.findById(UserContextHolder.getUserId())
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        Exercise exercise = requestDto.toEntity(member);

        List<MultipartFile> fileList = requestDto.getExercisePictureList();

        List<Picture> pictureList = new ArrayList<>();
        List<ExercisePicture> exercisePictureList = new ArrayList<>();
        for (MultipartFile file : fileList) {
            // S3에 이미지 업로드
            String objectKey = s3Service.generateObjectKey(file.getOriginalFilename(), FileType.EXERCISE);
            String imageUrl;
            try {
                imageUrl = s3Service.uploadImage(file, objectKey);
            } catch (IOException e) {
                throw new CustomException(ErrorCode.EXERCISE_PICTURE_UPLOAD_FAILED);
            }

            // DB에 이미지 정보 저장
            // Picture 엔티티 생성
            Picture picture = Picture.of(file, imageUrl);
            pictureList.add(picture);

            // ExercisePicture 엔티티 생성
            ExercisePicture exercisePicture = ExercisePicture.builder()
                    .exercise(exercise)
                    .pictures(picture)
                    .build();
            exercisePictureList.add(exercisePicture);
        }

        try {
            // DB에 운동 기록과 이미지 정보 저장
            exerciseRepository.save(exercise);
            pictureRepository.saveAll(pictureList);
            exercisePictureRepository.saveAll(exercisePictureList);
        } catch (Exception e) {
            // 예외 발생 시 S3에서 이미지 삭제
            for (Picture picture : pictureList) {
                s3Service.deleteImage(picture.getPictureUrl());
            }
            throw new CustomException(ErrorCode.EXERCISE_PICTURE_UPLOAD_FAILED);
        }

        return new CreateExerciseResponseDto(exercise.getId());
    }

    @Override
    public CreateExerciseResponseDto updateExerciseImages(UpdateExerciseRequestDto requestDto) throws IOException {
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
            } catch (IOException e) { // 이거아님
                throw new IOException();
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
}
