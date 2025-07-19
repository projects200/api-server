package com.project200.undabang.policy.controller;

import com.project200.undabang.configuration.AbstractRestDocSupport;


//@WebMvcTest(PolicyRestController.class)
class PolicyRestControllerTest extends AbstractRestDocSupport {
//    @MockitoBean
//    private PolicyService policyService;
//
//    @Nested
//    @DisplayName("운동 정책 조회 API 테스트")
//    class ExercisePolicyTest {
//        private final String maxPoint = "100";
//        private final String minPoint = "0";
//        private final String initialPoint = "35";
//        private final String penaltyPoint = "1";
//        private final String penaltyThresholdDay = "7";
//        private final String pointPerExercise = "3";
//        private final String validityPeriod = "2";
//    }
//
//        @BeforeEach
//        void setup(){
//            responseDto = ExercisePolicyResponseDto.builder()
//                    .maxPoint(maxPoint)
//                    .minPoint(minPoint)
//                    .initialPoint(initialPoint)
//                    .penaltyPoint(penaltyPoint)
//                    .penaltyThresholdDay(penaltyThresholdDay)
//                    .pointPerExercise(pointPerExercise)
//                    .validityPeriod(validityPeriod)
//                    .build();
//        }
//
//        @Test
//        @DisplayName("성공케이스 : 운동정책을 조회한다")
//        @Disabled
//        void getExercisePolicy_Success() throws Exception{
//            // given
//            String type = "exercise-score";
//            BDDMockito.given(policyService.getPoliciesByGroupName(type)).willReturn(responseDto);
//
//            // when
//            String response = mockMvc.perform(MockMvcRequestBuilders.get("/open/v1/policies")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .accept(MediaType.APPLICATION_JSON)
//                        .queryParam("type", type))
//                    .andExpect(status().isOk())
//                    .andDo(document.document(
//                            queryParameters(
//                                parameterWithName("type").attributes(getTypeFormat(JsonFieldType.STRING))
//                                        .description("정책 종류입니다. 운동 정책을 조회할 경우 exercises를 요청 하시면 됩니다.")
//                            ),
//                            responseFields(commonResponseFields(
//                                fieldWithPath("data.maxPoint").type(JsonFieldType.STRING).description("회원이 가질 수 있는 최대 점수 입니다."),
//                                fieldWithPath("data.minPoint").type(JsonFieldType.STRING).description("회원이 가질 수 있는 최소 점수 입니다."),
//                                fieldWithPath("data.initialPoint").type(JsonFieldType.STRING).description("회원이 회원 가입시 부여 받는 기본 점수 입니다."),
//                                fieldWithPath("data.penaltyPoint").type(JsonFieldType.STRING).description("회원이 일정 기간 운동 기록을 생성 하지 않을시 차감 되는 포인트 입니다."),
//                                fieldWithPath("data.pointPerExercise").type(JsonFieldType.STRING).description("회원이 운동 기록을 생성할 시 부여 받는 포인트 입니다." +
//                                        "(해당 날짜에 운동 기록이 없을 경우 부여 받습니다)"),
//                                fieldWithPath("data.validityPeriod").type(JsonFieldType.STRING).description("회원이 운동 기록을 생성할 시, 점수를 부여 받을 수 있는 기간을 의미 합니다. " +
//                                        "이 기간이 지나면 운동 기록을 생성 해도 점수를 부여받을 수 없습니다"),
//                                fieldWithPath("data.penaltyThresholdDay").type(JsonFieldType.STRING).description("회원이 특정 기간동안 운동 기록을 생성하지 않을 경우, 운동 점수가 감소합니다." +
//                                        "이때, 특정 기간을 설정하는 필드입니다.")
//                            ))
//                    )).andReturn().getResponse().getContentAsString();
//
//            CommonResponse<?> expectedData = CommonResponse.success(responseDto);
//            String expected = objectMapper.writeValueAsString(expectedData);
//            Assertions.assertThat(response).isEqualTo(expected);
//        }
//
//        @Test
//        @DisplayName("실패케이스 : 쿼리 파라미터가 없는 경우 400에러를 반환한다")
//        @Disabled
//        void getExercisePolicy_Fail_NoTypeQueryParam() throws Exception {
//            // when then
//            mockMvc.perform(MockMvcRequestBuilders.get("/open/v1/policies")
//                            .contentType(MediaType.APPLICATION_JSON)
//                            .accept(MediaType.APPLICATION_JSON))
//                    .andExpect(status().isBadRequest());
//        }
//
//        @Test
//        @DisplayName("실패케이스 : 지원하지 않는 type으로 조회시 500에러를 반환한다")
//        @Disabled
//        void getExercisePolicy_Fail_UnsupportedType() throws Exception {
//            // given
//            String invalidType = "invalid-type";
//            BDDMockito.given(policyService.getPoliciesByGroupName(invalidType))
//                    .willThrow(new CustomException(ErrorCode.POLICY_NOT_FOUND));
//
//            // when then
//            mockMvc.perform(MockMvcRequestBuilders.get("/open/v1/policies")
//                            .contentType(MediaType.APPLICATION_JSON)
//                            .accept(MediaType.APPLICATION_JSON)
//                            .queryParam("type", invalidType))
//                    .andExpect(status().isInternalServerError());
//        }
//    }
}
