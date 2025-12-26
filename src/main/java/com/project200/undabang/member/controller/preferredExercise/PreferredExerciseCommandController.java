package com.project200.undabang.member.controller.preferredExercise;

import com.project200.undabang.common.web.response.CommonResponse;
import com.project200.undabang.member.dto.request.CreatePreferredExerciseRequest;
import com.project200.undabang.member.dto.request.DeletePreferredExerciseRequest;
import com.project200.undabang.member.dto.request.UpdatePreferredExerciseRequest;
import com.project200.undabang.member.dto.response.MyPreferredExerciseResponse;
import com.project200.undabang.member.service.PreferredExerciseCommandService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class PreferredExerciseCommandController {

    private final PreferredExerciseCommandService preferredExerciseCommandService;

    /**
     * 선호 운동 목록을 추가합니다.
     *
     * @param requests 추가할 선호 운동 요청 목록
     * @return 추가된 선호 운동 목록
     */
    @PostMapping("/v1/preferred-exercises")
    public ResponseEntity<CommonResponse<List<MyPreferredExerciseResponse>>> createPreferredExercises(
            @RequestBody @Valid List<CreatePreferredExerciseRequest> requests) {
        return ResponseEntity.ok(CommonResponse.create(
                preferredExerciseCommandService.createPreferredExercises(requests)));
    }

    /**
     * 선호 운동 목록을 삭제합니다.
     *
     * @param request 삭제할 선호 운동 ID 목록이 담긴 요청
     * @return 성공 응답
     */
    @DeleteMapping("/v1/preferred-exercises")
    public ResponseEntity<CommonResponse<Void>> deletePreferredExercises(
            @RequestBody @Valid DeletePreferredExerciseRequest request) {
        preferredExerciseCommandService.deletePreferredExercises(request.getPreferredExerciseIds());
        return ResponseEntity.ok(CommonResponse.delete(null));
    }

    /**
     * 선호 운동 목록을 수정합니다.
     *
     * @param requests 수정할 선호 운동 정보 (ID, 숙련도, 요일)목록이 담긴 요청
     * @return 성공 응답
     */
    @PatchMapping("/v1/preferred-exercises")
    public ResponseEntity<CommonResponse<List<MyPreferredExerciseResponse>>> updatePreferredExercises(
            @RequestBody @Valid List<UpdatePreferredExerciseRequest> requests) {
        return ResponseEntity.ok(CommonResponse.update(
            preferredExerciseCommandService.updatePreferredExercises(requests)));
    }

}
