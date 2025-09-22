package com.project200.undabang.member.controller.location;

import com.project200.undabang.common.web.response.CommonResponse;
import com.project200.undabang.member.dto.request.GetMembersExerciseLocationsRequest;
import com.project200.undabang.member.dto.response.GetMembersExerciseLocationsResponse;
import com.project200.undabang.member.service.ExerciseLocationQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class ExerciseLocationQueryController {
    private final ExerciseLocationQueryService exerciseLocationQueryService;

    @GetMapping("/v1/members")
    public ResponseEntity<CommonResponse<GetMembersExerciseLocationsResponse>> getMembersExerciseLocations(@RequestParam(value = "latitude") Double latitude,
                                                                                                           @RequestParam(value = "longitude") Double longitude) {

        GetMembersExerciseLocationsRequest request = new GetMembersExerciseLocationsRequest(latitude, longitude);
        return ResponseEntity.ok(CommonResponse.success(exerciseLocationQueryService.getMembersExerciseLocations(request)));
    }
}
