package com.project200.undabang.policy.controller;

import com.project200.undabang.common.web.exception.CustomException;
import com.project200.undabang.common.web.exception.ErrorCode;
import com.project200.undabang.common.web.response.CommonResponse;
import com.project200.undabang.policy.dto.response.PolicyResponseDto;
import com.project200.undabang.policy.service.PolicyGroupService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 정책 관련 API 요청을 처리하는 컨트롤러입니다.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/open")
public class PolicyRestController {
    private final PolicyGroupService policyGroupService;

    /**
     * 지정된 타입의 정책 정보를 LocalCache에서 조회합니다.
     * PathVariable 'groupName'을 기준으로 관련 정책 데이터를 반환합니다.
     */
    @GetMapping("/v1/policy-groups/{groupName}/policies")
    public ResponseEntity<CommonResponse<PolicyResponseDto>> findPolicies(@PathVariable String groupName){
        PolicyResponseDto policyResponseDto = policyGroupService.getByGroupName(groupName).orElseThrow(() -> new CustomException(ErrorCode.POLICY_NOT_EXIST));

        return ResponseEntity.ok(CommonResponse.success(policyResponseDto));
    }
}
