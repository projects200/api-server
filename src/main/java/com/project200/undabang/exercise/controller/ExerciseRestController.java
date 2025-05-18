package com.project200.undabang.exercise.controller;

import com.project200.undabang.common.web.response.CommonResponse;
import com.project200.undabang.exercise.dto.response.FindExerciseRecordResponseDto;
import com.project200.undabang.exercise.service.ExerciseRecordService;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
public class ExerciseRestController {
    private final ExerciseRecordService exerciseRecordService;


    @GetMapping("/v1/exerciseRecords/{recordId}")
    public ResponseEntity<CommonResponse<FindExerciseRecordResponseDto>> findMemberExerciseRecord(@PathVariable @Positive(message = "올바른 Record를 다시 입력해주세요") Long recordId){
        FindExerciseRecordResponseDto responseDto = exerciseRecordService.findExerciseRecordByRecordId(recordId);
        return ResponseEntity.ok(CommonResponse.success(responseDto));
    }
}
