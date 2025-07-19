package com.project200.undabang.policy.controller;

import com.project200.undabang.common.web.response.CommonResponse;
import com.project200.undabang.policy.dto.response.PolicyResponseDto;
import com.project200.undabang.policy.service.PolicyQueryService;
import com.project200.undabang.policy.service.PolicyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 정책 관련 API 요청을 처리하는 컨트롤러입니다.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/open")
public class PolicyRestController {
    private final PolicyService policyService;
    private final PolicyQueryService policyQueryService;

    /**
     * 지정된 타입의 정책 정보를 조회합니다.
     * PathVariable 'groupName'을 기준으로 관련 정책 데이터를 반환합니다.
     */
    @GetMapping("/v1/policy-groups/{groupName}/policies")
    public ResponseEntity<CommonResponse<PolicyResponseDto>> findPolicies(@PathVariable String groupName){
        PolicyResponseDto policy = policyQueryService.getPoliciesByGroupName(groupName);

        return ResponseEntity.ok(CommonResponse.success(policy));
    }
}
