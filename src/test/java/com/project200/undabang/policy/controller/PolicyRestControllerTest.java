package com.project200.undabang.policy.controller;

import com.project200.undabang.common.web.response.CommonResponse;
import com.project200.undabang.configuration.AbstractRestDocSupport;
import com.project200.undabang.policy.dto.response.ExercisePolicyResponseDto;
import com.project200.undabang.policy.service.PolicyService;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.BDDMockito;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static com.project200.undabang.configuration.DocumentFormatGenerator.getTypeFormat;
import static com.project200.undabang.configuration.RestDocsUtils.commonResponseFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


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
            String type = "exercises";
            ExercisePolicyResponseDto responseDto = ExercisePolicyResponseDto.builder().build();

            BDDMockito.given(policyService.getExercisePolicies()).willReturn(responseDto);

            // when
            String response = mockMvc.perform(MockMvcRequestBuilders.get("/open/v1/policies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .queryParam("type", type))
                    .andExpect(status().isOk())
                    .andDo(document.document(
                            queryParameters(
                                parameterWithName("exercises").attributes(getTypeFormat(JsonFieldType.STRING))
                                        .description("정책 종류입니다. 운동 정책을 조회할 경우 exercises를 요청 하시면 됩니다.")
                            ),
                            responseFields(commonResponseFields(
                                fieldWithPath("data.maxPoint").type(getTypeFormat(JsonFieldType.STRING)).description("회원이 가질 수 있는 최대 점수 입니다."),
                                fieldWithPath("data.minPoint").type(getTypeFormat(JsonFieldType.STRING)).description("회원이 가질 수 있는 최소 점수 입니다."),
                                fieldWithPath("data.initialPoint").type(getTypeFormat(JsonFieldType.STRING)).description("회원이 회원 가입시 부여 받는 기본 점수 입니다."),
                                fieldWithPath("data.penaltyPoint").type(getTypeFormat(JsonFieldType.STRING)).description("회원이 일정 기간 운동 기록을 생성 하지 않을시 차감 되는 포인트 입니다."),
                                fieldWithPath("data.pointPerExercise").type(getTypeFormat(JsonFieldType.STRING)).description("회원이 운동 기록을 생성할 시 부여 받는 포인트 입니다." +
                                        "(해당 날짜에 운동 기록이 없을 경우 부여 받습니다)"),
                                fieldWithPath("data.validityPeriod").type(getTypeFormat(JsonFieldType.STRING)).description("회원이 운동 기록을 생성할 시, 점수를 부여 받을 수 있는 기간을 의미 합니다. " +
                                        "이 기간이 지나면 운동 기록을 생성 해도 점수를 부여받을 수 없습니다"),
                                fieldWithPath("data.penaltyThresholdDay").type(getTypeFormat(JsonFieldType.STRING)).description("회원이 특정 기간동안 운동 기록을 생성하지 않을 경우, 운동 점수가 감소합니다." +
                                        "이때, 특정 기간을 설정하는 필드입니다.")
                            ))
                    )).andReturn().getResponse().getContentAsString();

            CommonResponse expectedData = CommonResponse.success(responseDto);
            String expected = objectMapper.writeValueAsString(expectedData);
            Assertions.assertThat(response).isEqualTo(expected);
        }
    }
}
