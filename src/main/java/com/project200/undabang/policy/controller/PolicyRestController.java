package com.project200.undabang.policy.controller;

import com.project200.undabang.common.web.response.CommonResponse;
import com.project200.undabang.policy.service.PolicyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
@RequestMapping("/open")
public class PolicyRestController {
    private final PolicyService policyService;

    @GetMapping("/v1/policies")
    public ResponseEntity<CommonResponse<?>> findPolicies(@RequestParam(value = "type") String policyType){
        Object policy = policyService.findPoliciesByType(policyType);
        return ResponseEntity.ok(CommonResponse.success(policy));
    }
}
