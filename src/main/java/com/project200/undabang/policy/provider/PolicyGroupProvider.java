package com.project200.undabang.policy.provider;

import com.project200.undabang.policy.dto.response.PolicyResponseDto;

import java.util.Map;

public interface PolicyGroupProvider {
    /**
     * 모든 정책을 Map 형태로 조회하여 "policy-group" 캐시에 저장합니다.
     * 동일한 요청에 대해서는 DB 조회 없이 캐시된 결과를 반환합니다.
     * @return 정책 키와 정책 객체를 매핑한 Map
     */
    Map<String, PolicyResponseDto> getAllPolicyGroupAsMap();

    /**
     * 관리자가 정책을 수정한 후 호출하는 메소드입니다.
     * "policy-group" 캐시의 모든 항목을 제거하여 다음 조회 시 DB에서 최신 데이터를 가져오게 합니다.
     */
    void refreshPolicies();

    // todo : 우선 테스트 코드 작성하기
    // todo : 그 다음 서비스레이어에서 캐싱 되는지 확인하기
    // todo : 로컬에서 실제 개발디비 연결해서 체크해보기
}
