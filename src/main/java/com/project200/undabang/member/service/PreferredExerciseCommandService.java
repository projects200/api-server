package com.project200.undabang.member.service;

import com.project200.undabang.member.dto.request.CreatePreferredExerciseRequest;
import com.project200.undabang.member.dto.request.UpdatePreferredExerciseRequest;
import com.project200.undabang.member.dto.response.MyPreferredExerciseResponse;
import jakarta.validation.Valid;

import java.util.List;

public interface PreferredExerciseCommandService {
    /**
     * 선호 운동 목록을 추가합니다.
     *
     * @param requests 추가할 선호 운동 요청 목록
     * @return 추가된 선호 운동 목록
     */
    List<MyPreferredExerciseResponse> createPreferredExercises(List<CreatePreferredExerciseRequest> requests);

    /**
     * 선호 운동 목록을 삭제합니다.
     *
     * @param preferredExerciseIds 삭제할 선호 운동 ID 목록
     * @return 삭제된 선호 운동 ID 목록
     */
    void deletePreferredExercises(List<Long> preferredExerciseIds);

    /**
     * 선호 운동 목록을 수정합니다.
     *
     * @param requests 수정할 선호 운동 ID 목록
     * @return 수정할 선호 운동 ID 목록
     */
    List<MyPreferredExerciseResponse> updatePreferredExercises(@Valid List<UpdatePreferredExerciseRequest> requests);
}
