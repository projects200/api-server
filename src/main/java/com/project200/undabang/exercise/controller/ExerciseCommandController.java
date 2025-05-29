package com.project200.undabang.exercise.controller;

import com.project200.undabang.common.validation.AllowedExtensions;
import com.project200.undabang.common.web.response.CommonResponse;
import com.project200.undabang.exercise.dto.request.CreateExerciseRequestDto;
import com.project200.undabang.exercise.dto.request.UpdateExerciseRequestDto;
import com.project200.undabang.exercise.dto.response.ExerciseIdResponseDto;
import com.project200.undabang.exercise.service.ExerciseCommandService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class ExerciseCommandController {
    private final ExerciseCommandService exerciseCommandService;

    @PostMapping(path = "/v1/exercises")
    public ResponseEntity<CommonResponse<ExerciseIdResponseDto>> createExercise(@Valid @RequestBody CreateExerciseRequestDto requestDto) {
        ExerciseIdResponseDto responseData = exerciseCommandService.createExercise(requestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(CommonResponse.create(responseData));
    }

    // TODO: 운동기록 수정 api
    @PatchMapping(path = "/v1/exercises/{exerciseId}")
    public ResponseEntity<CommonResponse<ExerciseIdResponseDto>> updateExercise(
            @PathVariable @Positive(message = "올바른 Exercise ID를 입력해주세요") Long exerciseId,
            @Valid @RequestBody UpdateExerciseRequestDto requestDto) {
        ExerciseIdResponseDto responseData = exerciseCommandService.updateExercise(exerciseId, requestDto);
        return ResponseEntity.ok(CommonResponse.update(responseData));
    }

    // TODO: 운동기록 삭제 api
    @DeleteMapping(path = "/v1/exercises/{exerciseId}")
    public ResponseEntity<CommonResponse<Void>> deleteExercise(
            @PathVariable @Positive(message = "올바른 Exercise ID를 입력해주세요") Long exerciseId) {
        exerciseCommandService.deleteExercise(exerciseId);
        return ResponseEntity.ok(CommonResponse.success());
    }


    @PostMapping(path = "/v1/exercises/{exerciseId}/pictures", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CommonResponse<ExerciseIdResponseDto>> uploadExerciseImages(
            @PathVariable @Positive(message = "올바른 Exercise ID를 입력해주세요") Long exerciseId,
            @Size(max = 5, message = "최대 5개의 파일만 업로드할 수 있습니다.")
            @AllowedExtensions(extensions = {".jpg", ".jpeg", ".png"})
            @RequestPart("pictures") List<MultipartFile> exercisePictureList) {
        ExerciseIdResponseDto responseData = exerciseCommandService.uploadExerciseImages(exerciseId, exercisePictureList);
        return ResponseEntity.status(HttpStatus.CREATED).body(CommonResponse.create(responseData));
    }

    // TODO: 업로드한 파일 리스트로 삭제 api
    @DeleteMapping(path = "/v1/exercises/{exerciseId}/pictures")
    public ResponseEntity<CommonResponse<Void>> deleteExerciseImages(
            @PathVariable @Positive(message = "올바른 Exercise ID를 입력해주세요") Long exerciseId,
            @RequestParam("pictureIds") List<Long> pictureIds) {
        exerciseCommandService.deleteExerciseImages(exerciseId, pictureIds);
        return ResponseEntity.ok(CommonResponse.success());
    }
}
