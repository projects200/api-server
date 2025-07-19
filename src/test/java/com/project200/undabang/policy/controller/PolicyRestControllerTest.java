package com.project200.undabang.policy.controller;

import com.project200.undabang.common.web.response.CommonResponse;
import com.project200.undabang.configuration.AbstractRestDocSupport;
import com.project200.undabang.policy.dto.record.PolicyItemRecord;
import com.project200.undabang.policy.dto.response.PolicyResponseDto;
import com.project200.undabang.policy.service.PolicyQueryService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.BDDMockito;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.List;

import static com.project200.undabang.configuration.DocumentFormatGenerator.getTypeFormat;
import static com.project200.undabang.configuration.RestDocsUtils.commonResponseFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@WebMvcTest(PolicyRestController.class)
class PolicyRestControllerTest extends AbstractRestDocSupport {
    @MockitoBean
    private PolicyQueryService policyQueryService;

    @Test
    @DisplayName("정책 그룹 이름으로 조회시 성공하면 정책 그룹 이름, 크기, 정책들을 반환한다")
    void findPoliciesByGroupName() throws Exception {
        // given
        String groupName = "exercise-score";
        List<PolicyItemRecord> policyItemRecordList = List.of(
                new PolicyItemRecord("EXERCISE_SCORE_MAX_POINTS", "100", "POINTS", "회원이 가질 수 있는 최대 운동 점수"),
                new PolicyItemRecord("EXERCISE_SCORE_MIN_POINTS", "0", "POINTS", "회원이 가질 수 있는 최소 운동 점수"),
                new PolicyItemRecord("SIGNUP_INITIAL_POINTS", "35", "POINTS", "회원 가입 시 기본으로 부여되는 점수"),
                new PolicyItemRecord("POINTS_PER_EXERCISE", "3", "POINTS", "운동 기록 1회당 부여되는 점수 (일 1회)"),
                new PolicyItemRecord("EXERCISE_RECORD_VALIDITY_PERIOD", "2", "DAYS", "점수 획득이 가능한 운동 기록의 유효 기간. (단위: DAYS, HOURS, MINUTES)"),
                new PolicyItemRecord("EXERCISE_RECORD_MAX_PER_DAY", "1", "COUNT", "하루에 기록할 수 있는 최대 운동 횟수"),
                new PolicyItemRecord("PENALTY_INACTIVITY_THRESHOLD_DAYS", "7", "DAYS", "페널티가 시작되는 비활성 기준일 (이 기간 이상 운동 기록이 없을 경우)"),
                new PolicyItemRecord("PENALTY_SCORE_DECREMENT_POINTS", "1", "POINTS", "비활성 상태일 때 매일 차감되는 점수")
        );

        PolicyResponseDto mockedResponse = PolicyResponseDto.builder()
                .groupName(groupName)
                .size(8)
                .policies(policyItemRecordList)
                .build();

        // when
        BDDMockito.given(policyQueryService.getPoliciesByGroupName(groupName)).willReturn(mockedResponse);

        // then
        String response = mockMvc.perform(MockMvcRequestBuilders.get("/open/v1/policy-groups/{groupName}/policies", groupName)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andDo(this.document.document(
                        pathParameters(
                                parameterWithName("groupName").attributes(getTypeFormat(JsonFieldType.STRING))
                                        .description("정책 그룹 이름명 입니다. 조회할 정책 그룹의 이름을 입력 하시면 됩니다.")
                        ),
                        responseFields(commonResponseFields(
                                fieldWithPath("data.groupName").type(JsonFieldType.STRING).description("정책 그룹 이름입니다."),
                                fieldWithPath("data.size").type(JsonFieldType.NUMBER).description("정책 그룹 이름으로 조회한 정책들의 크기를 반환합니다."),
                                fieldWithPath("data.policies[]").type(JsonFieldType.ARRAY).description("정책들을 반환하는 리스트 입니다."),
                                fieldWithPath("data.policies[].policyKey").type(JsonFieldType.STRING).description("정책을 식별하는 고유 키입니다."),
                                fieldWithPath("data.policies[].policyValue").type(JsonFieldType.STRING).description("정책에 해당하는 값입니다."),
                                fieldWithPath("data.policies[].policyUnit").type(JsonFieldType.STRING).description("정책 값의 단위입니다."),
                                fieldWithPath("data.policies[].policyDescription").type(JsonFieldType.STRING).description("정책에 대한 설명입니다.")
                        ))
                )).andReturn().getResponse().getContentAsString();

        //then
        CommonResponse<PolicyResponseDto> expectedData = CommonResponse.success(mockedResponse);
        String expected = objectMapper.writeValueAsString(expectedData);
        Assertions.assertEquals(response, expected);
    }
}
