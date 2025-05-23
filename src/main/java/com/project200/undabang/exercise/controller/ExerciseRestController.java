package com.project200.undabang.exercise.controller;

import com.project200.undabang.common.web.response.CommonResponse;
import com.project200.undabang.exercise.dto.request.CreateExerciseRequestDto;
import com.project200.undabang.exercise.dto.request.UpdateExerciseRequestDto;
import com.project200.undabang.exercise.dto.response.CreateExerciseResponseDto;
import com.project200.undabang.exercise.dto.response.FindExerciseRecordByPeriodResponseDto;
import com.project200.undabang.exercise.dto.response.FindExerciseRecordDateResponseDto;
import com.project200.undabang.exercise.dto.response.FindExerciseRecordResponseDto;
import com.project200.undabang.exercise.service.ExerciseQueryService;
import com.project200.undabang.exercise.service.ExerciseService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

/**
 * 운동 기록 관련 API 엔드포인트를 제공하는 컨트롤러입니다.
 * 사용자의 운동 기록 조회 및 날짜별 운동 기록 조회 기능을 제공합니다.
 */
@RestController
@RequiredArgsConstructor
public class ExerciseRestController {
    private final ExerciseQueryService exerciseQueryService;
    private final ExerciseService exerciseService;

    /**
     * 새로운 운동 기록을 생성하기 위한 메서드입니다. 전달받은 요청 데이터를 바탕으로 운동 기록 이미지를 업로드하고, 업로드 결과를 반환합니다.
     *
     * @param requestDto 운동 기록 생성 요청 데이터를 포함한 객체입니다.
     *                   이 객체는 유효성 검증(Validation)을 거쳐야 하며, 운동 관련 정보와 업로드할 이미지 파일 목록을 포함하고 있습니다.
     * @return 생성된 운동 기록과 관련된 응답 데이터를 포함한 객체입니다.
     * 업로드된 이미지 URL 목록을 포함하여 반환됩니다.
     * @throws IOException 이미지 업로드 중 파일 처리 과정에서 발생할 수 있는 IO 예외를 나타냅니다.
     */
    @PostMapping(path = "/v1/exercises", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CommonResponse<CreateExerciseResponseDto>> createExercise(@Valid @ModelAttribute CreateExerciseRequestDto requestDto) throws IOException {
        CreateExerciseResponseDto createExerciseResponseDto = exerciseService.uploadExerciseImages(requestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(CommonResponse.create(createExerciseResponseDto));
    }

    /**
     * 특정 운동 기록 ID로 해당 운동 기록을 상세 조회합니다.
     */
    @GetMapping("/v1/exercises/{recordId}")
    public ResponseEntity<CommonResponse<FindExerciseRecordResponseDto>> findMemberExerciseRecord(@PathVariable @Positive(message = "올바른 Record를 다시 입력해주세요") Long recordId){
        FindExerciseRecordResponseDto responseDto = exerciseQueryService.findExerciseRecordByRecordId(recordId);
        return ResponseEntity.ok(CommonResponse.success(responseDto));
    }

    /**
     * 특정 날짜에 해당하는 운동 기록 목록을 조회합니다.
     * 요청 파라미터로 전달된 날짜(YYYY-MM-DD 형식)에 해당하는 운동 기록을 반환합니다.
     * 날짜에 해당하는 운동이 없다면 null을 반환합니다.
     */
    @GetMapping("/v1/exercises")
    public ResponseEntity<CommonResponse<List<FindExerciseRecordDateResponseDto>>> findExerciseRecordByDate(@RequestParam(value = "date") LocalDate inputDate){
        List<FindExerciseRecordDateResponseDto> responseDto = exerciseQueryService.findExerciseRecordByDate(inputDate).orElse(null);
        return responseDto != null ? ResponseEntity.ok(CommonResponse.success(responseDto)) : ResponseEntity.ok(CommonResponse.success());
    }

    /**
     * 특정 기간 동안의 날짜별 운동 기록 개수를 조회합니다.
     */
    @GetMapping("/v1/exercises/count")
    public ResponseEntity<CommonResponse<List<FindExerciseRecordByPeriodResponseDto>>> findExerciseRecordByPeriod(@RequestParam(value = "start") LocalDate startDate,
                                                                                                                  @RequestParam(value = "end") LocalDate endDate){
        List<FindExerciseRecordByPeriodResponseDto> responseDto = exerciseQueryService.findExerciseRecordsByPeriod(startDate, endDate);
        return ResponseEntity.ok(CommonResponse.success(responseDto));
    }

    @PatchMapping(path = "/v1/exercises/{exerciseId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CommonResponse<CreateExerciseResponseDto>> updateExerciseRecord(@PathVariable Long exerciseId,
                                                                                          @Valid @ModelAttribute UpdateExerciseRequestDto requestDto) throws IOException{
        CreateExerciseResponseDto responseDto = exerciseService.updateExerciseImages(exerciseId, requestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(CommonResponse.success(responseDto));
    }
}
