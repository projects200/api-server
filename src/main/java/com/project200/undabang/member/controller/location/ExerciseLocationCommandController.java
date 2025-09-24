package com.project200.undabang.member.controller.location;

import com.project200.undabang.common.web.response.CommonResponse;
import com.project200.undabang.member.dto.request.CreateExerciseLocationRequest;
import com.project200.undabang.member.dto.response.CreateExerciseLocationResponse;
import com.project200.undabang.member.service.ExerciseLocationCommandService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class ExerciseLocationCommandController {
    private final ExerciseLocationCommandService exerciseLocationCommandService;

    @PostMapping("/v1/exercise-locations")
    public ResponseEntity<CommonResponse<CreateExerciseLocationResponse>> createExerciseLocation(@Valid @RequestBody CreateExerciseLocationRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED).body(CommonResponse.create(exerciseLocationCommandService.createExerciseLocation(request)));
    }
}
