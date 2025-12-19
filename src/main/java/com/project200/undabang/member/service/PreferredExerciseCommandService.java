package com.project200.undabang.member.service;

import com.project200.undabang.member.dto.request.CreatePreferredExerciseRequest;
import com.project200.undabang.member.dto.response.MyPreferredExerciseResponse;

import java.util.List;

public interface PreferredExerciseCommandService {
    /**
     * 선호 운동 목록을 추가합니다.
     *
     * @param requests 추가할 선호 운동 요청 목록
     * @return 추가된 선호 운동 목록
     */
    List<MyPreferredExerciseResponse> createPreferredExercises(List<CreatePreferredExerciseRequest> requests);
}
