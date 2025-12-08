package com.project200.undabang.member.controller;

import com.project200.undabang.common.web.response.CommonResponse;
import com.project200.undabang.configuration.AbstractRestDocSupport;
import com.project200.undabang.member.dto.response.AvailableExerciseTypeResponse;
import com.project200.undabang.member.dto.response.MyPreferredExerciseResponse;
import com.project200.undabang.member.enums.ExerciseSkillLevel;
import com.project200.undabang.member.service.PreferredExerciseQueryService;
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

import java.util.List;
import java.util.UUID;

import static com.project200.undabang.configuration.HeadersGenerator.getCommonApiHeaders;
import static com.project200.undabang.configuration.RestDocsUtils.HEADER_ACCESS_TOKEN;
import static com.project200.undabang.configuration.RestDocsUtils.commonResponseFieldsForList;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PreferredExerciseQueryController.class)
@DisplayName("PreferredExerciseQueryController 테스트")
class PreferredExerciseQueryControllerTest extends AbstractRestDocSupport {

    @MockitoBean
    private PreferredExerciseQueryService preferredExerciseQueryService;

    // ============== 테스트 헬퍼 메소드 ==============

    private AvailableExerciseTypeResponse createAvailableExerciseTypeResponse(Long id, String name) {
        return AvailableExerciseTypeResponse.builder()
                .exerciseId(id)
                .exerciseName(name)
                .imageUrl("http://example.com/" + name + ".jpg")
                .build();
    }

    private MyPreferredExerciseResponse createMyPreferredExerciseResponse(Long id, String name) {
        return MyPreferredExerciseResponse.builder()
                .preferredExerciseId(id)
                .exerciseTypeId(10L)
                .exerciseName(name)
                .imageUrl("http://example.com/" + name + ".jpg")
                .skillLevel(ExerciseSkillLevel.INTERMEDIATE)
                .daysOfWeek(new boolean[]{true, false, true, false, true, false, false})
                .build();
    }

    @Nested
    @DisplayName("getAvailableExerciseTypes 메소드는")
    class GetAvailableExerciseTypes {

        @Test
        @DisplayName("선택 가능한 선호 운동 종류 목록을 조회한다")
        void getAvailableExerciseTypes_Success() throws Exception {
            // given
            UUID memberId = UUID.randomUUID();
            AvailableExerciseTypeResponse response1 = createAvailableExerciseTypeResponse(1L, "헬스");
            AvailableExerciseTypeResponse response2 = createAvailableExerciseTypeResponse(2L, "러닝");

            List<AvailableExerciseTypeResponse> responseList = List.of(response1, response2);

            BDDMockito.given(preferredExerciseQueryService.getAvailableExerciseTypes())
                    .willReturn(responseList);

            // when
            String response = mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/exercise-types")
                            .contentType(MediaType.APPLICATION_JSON)
                            .accept(MediaType.APPLICATION_JSON)
                            .headers(getCommonApiHeaders(memberId)))
                    .andExpect(status().isOk())
                    .andDo(document.document(
                            requestHeaders(HEADER_ACCESS_TOKEN),
                            responseFields(commonResponseFieldsForList(
                                    fieldWithPath("data[].exerciseId").type(JsonFieldType.NUMBER).description("선택 가능한 선호 운동 종류 ID"),
                                    fieldWithPath("data[].exerciseName").type(JsonFieldType.STRING).description("선택 가능한 선호 운동 이름"),
                                    fieldWithPath("data[].imageUrl").type(JsonFieldType.STRING).description("선택 가능한 선호 운동 이미지 사진의 정보를 나타냅니다.")))))
                    .andReturn().getResponse().getContentAsString();

            // then
            CommonResponse<List<AvailableExerciseTypeResponse>> expectedData = CommonResponse
                    .success(responseList);
            String expected = objectMapper.writeValueAsString(expectedData);
            Assertions.assertThat(response).as("응답 본문 검증").isEqualTo(expected);
        }
    }

    @Nested
    @DisplayName("getMyPreferredExercises 메소드는")
    class GetMyPreferredExercises {

        @Test
        @DisplayName("현재 사용자가 보유하고 있는 선호 운동 목록을 조회한다")
        void getMyPreferredExercises_Success() throws Exception {
            // given
            UUID memberId = UUID.randomUUID();
            MyPreferredExerciseResponse response1 = createMyPreferredExerciseResponse(1L, "헬스");

            List<MyPreferredExerciseResponse> responseList = List.of(response1);

            BDDMockito.given(preferredExerciseQueryService.getMyPreferredExercises())
                    .willReturn(responseList);

            // when
            String response = mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/preferred-exercises")
                            .contentType(MediaType.APPLICATION_JSON)
                            .accept(MediaType.APPLICATION_JSON)
                            .headers(getCommonApiHeaders(memberId)))
                    .andExpect(status().isOk())
                    .andDo(document.document(
                            requestHeaders(HEADER_ACCESS_TOKEN),
                            responseFields(commonResponseFieldsForList(
                                    fieldWithPath("data[].preferredExerciseId").type(JsonFieldType.NUMBER).description("선호 운동 ID"),
                                    fieldWithPath("data[].exerciseTypeId")
                                            .type(JsonFieldType.NUMBER)
                                            .description("현재 사용자가 보유하고 있는 선호 운동 종류 ID"),
                                    fieldWithPath("data[].exerciseName")
                                            .type(JsonFieldType.STRING)
                                            .description("현재 사용자가 보유하고 있는 선호 운동 이름"),
                                    fieldWithPath("data[].imageUrl")
                                            .type(JsonFieldType.STRING)
                                            .description("현재 사용자가 보유하고 있는 운동 이미지 URL"),
                                    fieldWithPath("data[].skillLevel")
                                            .type(JsonFieldType.STRING)
                                            .description("현재 사용자가 보유하고 있는 운동 숙련도 (NOVICE, BEGINNER, INTERMEDIATE, ADVANCED, EXPERT, PROFESSIONAL)"),
                                    fieldWithPath("data[].daysOfWeek")
                                            .type(JsonFieldType.ARRAY)
                                            .description("현재 사용자가 보유하고 있는 운동 요일 (월~일)")))))
                    .andReturn().getResponse().getContentAsString();

            // then
            CommonResponse<List<MyPreferredExerciseResponse>> expectedData = CommonResponse
                    .success(responseList);
            String expected = objectMapper.writeValueAsString(expectedData);
            Assertions.assertThat(response).as("응답 본문 검증").isEqualTo(expected);
        }
    }
}
