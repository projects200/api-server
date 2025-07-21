package com.project200.undabang.score.controller;

import com.project200.undabang.score.dto.response.EarnablePointsInfoResponseDto;
import com.project200.undabang.score.service.ExerciseScoreQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/scores")
@RequiredArgsConstructor
public class ExerciseScoreQueryController {

    private final ExerciseScoreQueryService exerciseScoreQueryService;

    @GetMapping("/expected-points-info")
    public ResponseEntity<EarnablePointsInfoResponseDto> getEarnablePointsInfo() {
        EarnablePointsInfoResponseDto responseDto = exerciseScoreQueryService.getEarnablePointsInfo();
        return ResponseEntity.ok(responseDto);
    }
}
