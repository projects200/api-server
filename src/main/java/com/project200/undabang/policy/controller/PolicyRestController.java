package com.project200.undabang.policy.controller;

import com.project200.undabang.common.web.response.CommonResponse;
import com.project200.undabang.policy.service.PolicyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 정책 관련 API 요청을 처리하는 컨트롤러입니다.
 */
@Controller
@RequiredArgsConstructor
@RequestMapping("/open")
public class PolicyRestController {
    private final PolicyService policyService;

    /**
     * 지정된 타입의 정책 정보를 조회합니다.
     * 쿼리 파라미터 'type'을 기준으로 관련 정책 데이터를 반환합니다.
     */
    @GetMapping("/v1/policies")
    public ResponseEntity<CommonResponse<?>> findPolicies(@RequestParam(value = "type") String policyType){
        Object policy = policyService.getPoliciesByType(policyType);
        return ResponseEntity.ok(CommonResponse.success(policy));
    }
}
