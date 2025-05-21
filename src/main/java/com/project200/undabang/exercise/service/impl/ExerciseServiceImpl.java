package com.project200.undabang.exercise.service.impl;

import com.project200.undabang.common.service.S3Service;
import com.project200.undabang.exercise.dto.request.CreateExerciseRequestDto;
import com.project200.undabang.exercise.dto.response.CreateExerciseResponseDto;
import com.project200.undabang.exercise.repository.ExerciseRepository;
import com.project200.undabang.exercise.service.ExerciseService;
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

    private final S3Service s3Service;

    @Override
    public CreateExerciseResponseDto uploadExerciseImages(CreateExerciseRequestDto requestDto) throws IOException {
        List<MultipartFile> exercisePictureList = requestDto.getExercisePictureList();
        List<String> imageUrlList = new ArrayList<>();
        for (MultipartFile exercisePicture : exercisePictureList) {
            // S3에 이미지 업로드
            String objectKey = s3Service.generateObjectKey(exercisePicture.getOriginalFilename());
            String imageUrl = s3Service.uploadImage(exercisePicture, objectKey);
            imageUrlList.add(imageUrl);
        }
        return new CreateExerciseResponseDto(imageUrlList);
    }
}
