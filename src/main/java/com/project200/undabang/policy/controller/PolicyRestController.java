package com.project200.undabang.policy.controller;

import com.project200.undabang.common.web.response.CommonResponse;
import com.project200.undabang.policy.dto.response.ExercisePolicyResponseDto;
import com.project200.undabang.policy.provider.PolicyProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
@RequestMapping("/api")
public class PolicyRestController {
    private final PolicyProvider policyProvider;

    @GetMapping("/v1/policies")
    public CommonResponse<ExercisePolicyResponseDto> findExercisePolicies(@RequestParam(value = "type") String type){
        return null;
    }
}
