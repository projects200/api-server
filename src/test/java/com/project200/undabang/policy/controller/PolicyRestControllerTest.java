package com.project200.undabang.policy.controller;

import com.project200.undabang.configuration.AbstractRestDocSupport;
import com.project200.undabang.policy.dto.response.ExercisePolicyResponseDto;
import com.project200.undabang.policy.service.PolicyService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@WebMvcTest(PolicyRestController.class)
class PolicyRestControllerTest extends AbstractRestDocSupport {
    @MockitoBean
    private PolicyService policyService;

    @Nested
    @DisplayName("운동 정책 조회 API 테스트")
    class ExercisePolicyTest{

        @Test
        @DisplayName("성공케이스 : 운동정책을 조회한다")
        void getExercisePolicy_Success() throws Exception{
            // given
            ExercisePolicyResponseDto responseDto = ExercisePolicyResponseDto.builder().build();

//            BDDMockito.given(policyService.getExercisePolicy()).willReturn(responseDto);

            // when


        }
    }
}
