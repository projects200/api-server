package com.project200.undabang.member.service;

import com.project200.undabang.member.dto.response.AvailableExerciseTypeResponse;
import com.project200.undabang.member.dto.response.MyPreferredExerciseResponse;

import java.util.List;

/**
 * 선호 운동 조회 관련 비즈니스 로직을 처리하는 서비스 인터페이스입니다.
 */
public interface PreferredExerciseQueryService {
    /**
     * 선택 가능한 선호 운동 종류 목록을 조회합니다.
     * 
     * @return 선택 가능한 운동 종류 이름 목록
     */
    List<AvailableExerciseTypeResponse> getAvailableExerciseTypes();
    
    /**
     * 현재 사용자가 보유하고 있는 선호 운동 목록을 조회합니다.
     * 
     * @return 사용자의 선호 운동 목록 (운동 종류, 운동 주기, 운동 수준 포함)
     */
    List<MyPreferredExerciseResponse> getMyPreferredExercises();
}


