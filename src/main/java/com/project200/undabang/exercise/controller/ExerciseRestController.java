package com.project200.undabang.exercise.controller;

import com.project200.undabang.common.web.response.CommonResponse;
import com.project200.undabang.exercise.dto.response.FindExerciseRecordByPeriodResponseDto;
import com.project200.undabang.exercise.dto.response.FindExerciseRecordDateResponseDto;
import com.project200.undabang.exercise.dto.response.FindExerciseRecordResponseDto;
import com.project200.undabang.exercise.service.ExerciseRecordService;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * 운동 기록 관련 API 엔드포인트를 제공하는 컨트롤러입니다.
 * 사용자의 운동 기록 조회 및 날짜별 운동 기록 조회 기능을 제공합니다.
 */

@Validated
@RestController
@RequiredArgsConstructor
public class ExerciseRestController {
    private final ExerciseRecordService exerciseRecordService;

    /**
     * 특정 운동 기록 ID로 해당 운동 기록을 상세 조회합니다.
     */

    @GetMapping("/v1/exercises/{recordId}")
    public ResponseEntity<CommonResponse<FindExerciseRecordResponseDto>> findMemberExerciseRecord(@PathVariable @Positive(message = "올바른 Record를 다시 입력해주세요") Long recordId){
        FindExerciseRecordResponseDto responseDto = exerciseRecordService.findExerciseRecordByRecordId(recordId);
        return ResponseEntity.ok(CommonResponse.success(responseDto));
    }

    /**
     * 특정 날짜에 해당하는 운동 기록 목록을 조회합니다.
     * 요청 파라미터로 전달된 날짜(YYYY-MM-DD 형식)에 해당하는 운동 기록을 반환합니다.
     * 날짜에 해당하는 운동이 없다면 null을 반환합니다.
     */

    @GetMapping("/v1/exercises/dates")
    public ResponseEntity<CommonResponse<List<FindExerciseRecordDateResponseDto>>> findExerciseRecordByDate(@RequestParam(value = "date") LocalDate inputDate){
        List<FindExerciseRecordDateResponseDto> responseDto = exerciseRecordService.findExerciseRecordByDate(inputDate).orElse(null);
        return ResponseEntity.ok(CommonResponse.success(responseDto));
    }

    /**
     * 특정 기간 동안의 날짜별 운동 기록 개수를 조회합니다.
     */

    @GetMapping("/v1/exercises")
    public ResponseEntity<CommonResponse<List<FindExerciseRecordByPeriodResponseDto>>> findExerciseRecordByPeriod(@RequestParam(value = "start-date") LocalDate startDate,
                                                                                                                  @RequestParam(value = "end-date") LocalDate endDate){
        List<FindExerciseRecordByPeriodResponseDto> responseDto = exerciseRecordService.findExerciseRecordsByPeriod(startDate, endDate);
        return ResponseEntity.ok(CommonResponse.success(responseDto));
    }
}
