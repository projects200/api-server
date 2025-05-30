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


/**
 * 운동 기록 생성, 수정, 삭제 관련 API를 제공하는 컨트롤러입니다.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/")
public class ExerciseCommandController {
    private final ExerciseCommandService exerciseCommandService;

    /**
     * 새로운 운동 기록을 생성합니다.
     *
     * @param requestDto 운동 기록 생성 요청 DTO
     * @return 생성된 운동 기록 ID를 포함하는 응답
     */
    @PostMapping(path = "exercises")
    public ResponseEntity<CommonResponse<ExerciseIdResponseDto>> createExercise(@Valid @RequestBody CreateExerciseRequestDto requestDto) {
        ExerciseIdResponseDto responseData = exerciseCommandService.createExercise(requestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(CommonResponse.create(responseData));
    }

    /**
     * 특정 운동 기록에 이미지를 업로드합니다.
     *
     * @param exerciseId        운동 기록 ID
     * @param exercisePictureList 업로드할 이미지 파일 목록
     * @return 이미지가 업로드된 운동 기록 ID를 포함하는 응답
     */
    @PostMapping(path = "exercises/{exerciseId}/pictures", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CommonResponse<ExerciseIdResponseDto>> uploadExerciseImages(
            @PathVariable @Positive(message = "올바른 Exercise ID를 입력해주세요") Long exerciseId,
            @Size(max = 5, message = "최대 5개의 파일만 업로드할 수 있습니다.")
            @AllowedExtensions(extensions = {".jpg", ".jpeg", ".png"})
            @RequestPart("pictures") List<MultipartFile> exercisePictureList) {
        ExerciseIdResponseDto responseData = exerciseCommandService.uploadExerciseImages(exerciseId, exercisePictureList);
        return ResponseEntity.status(HttpStatus.CREATED).body(CommonResponse.create(responseData));
    }

    /**
     * 기존 운동 기록을 수정합니다.
     *
     * @param exerciseId 운동 기록 ID
     * @param requestDto 운동 기록 수정 요청 DTO
     * @return 수정된 운동 기록 ID를 포함하는 응답
     */
    @PatchMapping(path = "exercises/{exerciseId}")
    public ResponseEntity<CommonResponse<ExerciseIdResponseDto>> updateExercise(
            @PathVariable @Positive(message = "올바른 Exercise ID를 입력해주세요") Long exerciseId,
            @Valid @RequestBody UpdateExerciseRequestDto requestDto) {
        ExerciseIdResponseDto responseData = exerciseCommandService.updateExercise(exerciseId, requestDto);
        return ResponseEntity.ok(CommonResponse.update(responseData));
    }

    /**
     * 특정 운동 기록에서 지정된 이미지들을 삭제합니다.
     *
     * @param exerciseId 운동 기록 ID
     * @param pictureIds 삭제할 이미지 ID 목록
     * @return 성공 응답
     */
    @DeleteMapping(path = "exercises/{exerciseId}/pictures")
    public ResponseEntity<CommonResponse<Void>> deleteExerciseImages(
            @PathVariable @Positive(message = "올바른 Exercise ID를 입력해주세요") Long exerciseId,
            @RequestParam("pictureIds") List<Long> pictureIds) {
        exerciseCommandService.deleteImages(exerciseId, pictureIds);
        return ResponseEntity.ok(CommonResponse.success());
    }

    /**
     * 특정 운동 기록을 삭제합니다. (관련된 이미지 포함)
     *
     * @param exerciseId 삭제할 운동 기록 ID
     * @return 성공 응답
     */
    @DeleteMapping(path = "exercises/{exerciseId}")
    public ResponseEntity<CommonResponse<Void>> deleteExercise(
            @PathVariable @Positive(message = "올바른 Exercise ID를 입력해주세요") Long exerciseId) {
        exerciseCommandService.deleteExercise(exerciseId);
        return ResponseEntity.ok(CommonResponse.success());
    }
}
