package com.project200.undabang.exercise.service.impl;

import com.project200.undabang.common.context.UserContextHolder;
import com.project200.undabang.common.entity.Picture;
import com.project200.undabang.common.repository.PictureRepository;
import com.project200.undabang.common.service.FileType;
import com.project200.undabang.common.service.S3Service;
import com.project200.undabang.common.web.exception.CustomException;
import com.project200.undabang.common.web.exception.ErrorCode;
import com.project200.undabang.exercise.dto.request.CreateExerciseRequestDto;
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
import java.util.ArrayList;
import java.util.List;

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
}
