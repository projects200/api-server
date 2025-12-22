package com.project200.undabang.member.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project200.undabang.common.web.exception.CustomException;
import com.project200.undabang.common.web.exception.ErrorCode;
import com.project200.undabang.configuration.AbstractRestDocSupport;
import com.project200.undabang.configuration.RestDocsUtils;
import com.project200.undabang.member.controller.preferredExercise.PreferredExerciseCommandController;
import com.project200.undabang.member.dto.request.CreatePreferredExerciseRequest;
import com.project200.undabang.member.dto.response.MyPreferredExerciseResponse;
import com.project200.undabang.member.enums.ExerciseSkillLevel;
import com.project200.undabang.member.service.PreferredExerciseCommandService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.UUID;

import static com.project200.undabang.configuration.HeadersGenerator.getCommonApiHeaders;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PreferredExerciseCommandController.class)
class PreferredExerciseCommandControllerTest extends AbstractRestDocSupport {

    @MockitoBean
    private PreferredExerciseCommandService preferredExerciseCommandService;

    @Autowired
    private ObjectMapper objectMapper;

    @Nested
    @DisplayName("createPreferredExercises 메서드는")
    class Describe_createPreferredExercises {

        @Test
        @DisplayName("유효한 선호 운동 목록을 받아 생성하고 201 Created를 반환한다")
        void it_creates_preferred_exercises_and_returns_201() throws Exception {
            // given
            UUID memberId = UUID.randomUUID();

            CreatePreferredExerciseRequest request1 = createRequest(1L, ExerciseSkillLevel.BEGINNER);
            CreatePreferredExerciseRequest request2 = createRequest(2L, ExerciseSkillLevel.INTERMEDIATE);
            List<CreatePreferredExerciseRequest> requests = List.of(request1, request2);

            MyPreferredExerciseResponse response1 = MyPreferredExerciseResponse.builder()
                    .preferredExerciseId(100L)
                    .exerciseTypeId(1L)
                    .exerciseName("축구")
                    .skillLevel(ExerciseSkillLevel.BEGINNER)
                    .daysOfWeek(new boolean[7])
                    .imageUrl("url1")
                    .build();
            MyPreferredExerciseResponse response2 = MyPreferredExerciseResponse.builder()
                    .preferredExerciseId(101L)
                    .exerciseTypeId(2L)
                    .exerciseName("농구")
                    .skillLevel(ExerciseSkillLevel.INTERMEDIATE)
                    .daysOfWeek(new boolean[7])
                    .imageUrl("url2")
                    .build();

                        given(preferredExerciseCommandService.createPreferredExercises(anyList()))
                                        .willReturn(List.of(response1, response2));
            // when
            mockMvc.perform(post("/api/v1/preferred-exercises")
                            .contentType(MediaType.APPLICATION_JSON)
                            .accept(MediaType.APPLICATION_JSON)
                            .headers(getCommonApiHeaders(memberId))
                            .content(objectMapper.writeValueAsString(requests)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("CREATED"))
                    .andDo(org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document(
                            "post-my-preferred-exercises/post-my-preferred-exercises_-success",
                            requestHeaders(
                                    RestDocsUtils.HEADER_ACCESS_TOKEN),
                            requestFields(
                                    fieldWithPath("[].exerciseTypeId")
                                            .type(JsonFieldType.NUMBER)
                                            .description("현재 사용자가 추가할 선호 운동 종류 ID입니다."),
                                    fieldWithPath("[].skillLevel")
                                            .type(JsonFieldType.STRING)
                                            .description(
                                                    "현재 사용자가 추가할 선호 운동 실력 (BEGINNER, ROOKIE, INTERMEDIATE, ADVANCED, SKILLED, PRO)"),
                                    fieldWithPath("[].daysOfWeek")
                                            .type(JsonFieldType.ARRAY)
                                            .description("현재 사용자가 추가할 선호 운동 요일 (월~일, boolean array)")),
                            responseFields(RestDocsUtils.commonResponseFieldsForList(
                                    fieldWithPath("data[].preferredExerciseId")
                                            .type(JsonFieldType.NUMBER)
                                            .description("생성된 선호 운동 ID"),
                                    fieldWithPath("data[].exerciseTypeId")
                                            .type(JsonFieldType.NUMBER)
                                            .description("생성된 선호 운동 종류 ID"),
                                    fieldWithPath("data[].exerciseName")
                                            .type(JsonFieldType.STRING)
                                            .description("생성된 선호 운동 이름"),
                                    fieldWithPath("data[].skillLevel")
                                            .type(JsonFieldType.STRING)
                                            .description("생성된 선호 운동 실력"),
                                    fieldWithPath("data[].daysOfWeek")
                                            .type(JsonFieldType.ARRAY)
                                            .description("생성된 선호 운동 요일별 선호 여부"),
                                    fieldWithPath("data[].imageUrl")
                                            .type(JsonFieldType.STRING)
                                            .description("생성된 선호 운동 이미지 URL")))));
        }

        @Test
        @DisplayName("최대 갯수를 초과하면 예외를 반환한다")
        void it_returns_error_when_limit_exceeded() throws Exception {
            // given
            UUID memberId = UUID.randomUUID();
            List<CreatePreferredExerciseRequest> requests = List
                    .of(createRequest(1L, ExerciseSkillLevel.BEGINNER));

            given(preferredExerciseCommandService.createPreferredExercises(anyList()))
                    .willThrow(new CustomException(
                            ErrorCode.PREFERRED_EXERCISE_MAX_COUNT_VIOLATION));

            // when
            mockMvc.perform(post("/api/v1/preferred-exercises")
                            .contentType(MediaType.APPLICATION_JSON)
                            .headers(getCommonApiHeaders(memberId))
                            .content(objectMapper.writeValueAsString(requests)))
                    .andExpect(status().isConflict()) // 409
                    .andExpect(jsonPath("$.code").value("PREFERRED_EXERCISE_MAX_COUNT_VIOLATION"))
                    .andDo(org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document(
                            "post-my-preferred-exercises/limit-exceeded"));
        }
    }

    private CreatePreferredExerciseRequest createRequest(Long exerciseTypeId, ExerciseSkillLevel level) {
        CreatePreferredExerciseRequest request = new CreatePreferredExerciseRequest();
        ReflectionTestUtils.setField(request, "exerciseTypeId", exerciseTypeId);
        ReflectionTestUtils.setField(request, "skillLevel", level);
        ReflectionTestUtils.setField(request, "daysOfWeek",
                new boolean[]{true, false, true, false, true, false, false});
        return request;
    }
}
