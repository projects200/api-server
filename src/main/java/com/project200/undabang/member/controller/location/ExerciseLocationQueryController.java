package com.project200.undabang.member.controller.location;

import com.project200.undabang.common.web.response.CommonResponse;
import com.project200.undabang.member.dto.record.Viewport;
import com.project200.undabang.member.dto.response.GetExerciseLocationsResponse;
import com.project200.undabang.member.dto.response.GetMembersExerciseLocationsResponse;
import com.project200.undabang.member.service.ExerciseLocationQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class ExerciseLocationQueryController {
    private final ExerciseLocationQueryService exerciseLocationQueryService;

    @GetMapping("/v1/members")
    public ResponseEntity<CommonResponse<List<GetMembersExerciseLocationsResponse>>> getMembersExerciseLocations(
            @RequestParam Double leftTopLatitude,
            @RequestParam Double leftTopLongitude,
            @RequestParam Double rightBottomLatitude,
            @RequestParam Double rightBottomLongitude) {

        Viewport viewport = Viewport.of(leftTopLatitude, leftTopLongitude, rightBottomLatitude, rightBottomLongitude);

        return ResponseEntity.ok(CommonResponse.success(exerciseLocationQueryService.getMembersExerciseLocations(viewport)));
    }

    @GetMapping("/v1/exercise-locations")
    public ResponseEntity<CommonResponse<List<GetExerciseLocationsResponse>>> getExerciseLocations() {

        return ResponseEntity.ok(CommonResponse.success(exerciseLocationQueryService.getExerciseLocations()));
    }
}
