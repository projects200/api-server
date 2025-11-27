package com.project200.undabang.member.controller;

import com.project200.undabang.common.web.response.CommonResponse;
import com.project200.undabang.member.dto.response.AvailableExerciseTypeResponse;
import com.project200.undabang.member.dto.response.MyPreferredExerciseResponse;
import com.project200.undabang.member.service.PreferredExerciseQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 선호 운동 조회 관련 REST API를 처리하는 컨트롤러입니다.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class PreferredExerciseQueryController {
    
    private final PreferredExerciseQueryService preferredExerciseQueryService;
    
    /**
     * 선택 가능한 선호 운동 종류 목록을 조회합니다.
     */
    @GetMapping("/v1/exercise-types")
    public ResponseEntity<CommonResponse<List<AvailableExerciseTypeResponse>>> getAvailableExerciseTypes() {
        return ResponseEntity.ok(CommonResponse.success(preferredExerciseQueryService.getAvailableExerciseTypes()));
    }
    
    /**
     * 현재 사용자가 보유하고 있는 선호 운동 목록을 조회합니다.
     */
    @GetMapping("/v1/preferred-exercises")
    public ResponseEntity<CommonResponse<List<MyPreferredExerciseResponse>>> getMyPreferredExercises() {
        return ResponseEntity.ok(CommonResponse.success(preferredExerciseQueryService.getMyPreferredExercises()));
    }
}


