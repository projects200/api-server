package com.project200.undabang.exercise.controller;

import com.project200.undabang.common.web.response.CommonResponse;
import com.project200.undabang.configuration.AbstractRestDocSupport;
import com.project200.undabang.configuration.RestDocsUtils;
import com.project200.undabang.exercise.dto.request.CreateExerciseRequestDto;
import com.project200.undabang.exercise.dto.request.UpdateExerciseRequestDto;
import com.project200.undabang.exercise.dto.response.ExerciseIdResponseDto;
import com.project200.undabang.exercise.service.ExerciseCommandService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.BDDMockito;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ExerciseCommandController.class)
class ExerciseCommandControllerTest extends AbstractRestDocSupport {

    @MockitoBean
    private ExerciseCommandService exerciseCommandService;

    @Test
    @DisplayName("운동 생성 - 성공 케이스")
    void createExerciseSuccess() throws Exception {
        // given
        UUID testUserId = UUID.randomUUID();

        CreateExerciseRequestDto requestDto = CreateExerciseRequestDto.builder()
                .exerciseTitle("운동 1일차")
                .exerciseStartedAt(LocalDateTime.now().minusHours(3))
                .exerciseEndedAt(LocalDateTime.now().minusHours(1))
                .exerciseLocation("여의도 한강 공원")
                .exercisePersonalType("러닝")
                .exerciseDetail("10시간 동안 달리기")
                .build();

        ExerciseIdResponseDto responseDto = new ExerciseIdResponseDto(1L);
        given(exerciseCommandService.createExercise(BDDMockito.any(CreateExerciseRequestDto.class)))
                .willReturn(responseDto);

        // when
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-USER-ID", testUserId.toString());
        headers.add("Authorization", "Bearer dummy-access-token-for-docs");

        String actualResponse = this.mockMvc.perform(MockMvcRequestBuilders.post("/v1/exercises")
                        .content(objectMapper.writeValueAsString(requestDto))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .headers(headers))
                .andExpect(status().isCreated())
                .andDo(this.document.document(
                        requestHeaders(RestDocsUtils.HEADER_ACCESS_TOKEN),
                        requestFields(
                                fieldWithPath("exerciseTitle").type(JsonFieldType.STRING)
                                        .description("운동 제목: 최대 255자"),
                                fieldWithPath("exerciseStartedAt").type(JsonFieldType.STRING)
                                        .description("운동 시작 일시: 현재 이전"),
                                fieldWithPath("exerciseEndedAt").type(JsonFieldType.STRING)
                                        .description("운동 종료 일시: 현재 이전, 시작일시 이후"),
                                fieldWithPath("exerciseDetail").type(JsonFieldType.STRING)
                                        .description("운동 상세 설명: 글자 수 제한 없음"),
                                fieldWithPath("exerciseLocation").type(JsonFieldType.STRING)
                                        .description("운동 장소(사용자 직접 입력): 최대 255자"),
                                fieldWithPath("exercisePersonalType").type(JsonFieldType.STRING)
                                        .description("운동 종류: 최대 255자")
                        ),
                        responseFields(RestDocsUtils.commonResponseFields(
                                fieldWithPath("data.exerciseId").type(JsonFieldType.NUMBER).description("생성된 운동의 ID")
                        ))
                ))
                .andReturn().getResponse().getContentAsString();

        // then
        CommonResponse<ExerciseIdResponseDto> expectedResponse = CommonResponse.create(responseDto);
        assertThat(actualResponse)
                .as("운동 생성 성공 응답 검증")
                .isEqualTo(objectMapper.writeValueAsString(expectedResponse));
        BDDMockito.then(exerciseCommandService).should(BDDMockito.times(1))
                .createExercise(BDDMockito.any(CreateExerciseRequestDto.class));
    }

    @Test
    @DisplayName("운동기록 변경 테스트 _ 성공")
    void exerciseUpdate_Success() throws Exception {
        // given
        UUID memberId = UUID.randomUUID();
        Long exerciseId = 1L;
        UpdateExerciseRequestDto requestDto = UpdateExerciseRequestDto.builder()
                .exerciseTitle("운동제목")
                .exerciseDetail("운동내용")
                .exerciseLocation("운동위치")
                .exercisePersonalType("운동종류")
                .exerciseStartedAt(LocalDateTime.of(2025, 5, 1, 0, 0, 0))
                .exerciseEndedAt(LocalDateTime.of(2025, 5, 1, 1, 0, 0))
                .build();

        ExerciseIdResponseDto responseDto = new ExerciseIdResponseDto(1L);

        BDDMockito.given(exerciseCommandService.updateExercise(exerciseId, requestDto)).willReturn(responseDto);

        // when
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-USER-ID", memberId.toString());
        headers.add("Authorization", "Bearer {AccessToken}");

        this.mockMvc.perform(MockMvcRequestBuilders.patch("/v1/exercises/{exerciseId}", exerciseId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .headers(headers)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk())
                .andDo(this.document.document(
                        requestHeaders(RestDocsUtils.HEADER_ACCESS_TOKEN),
                        requestFields(
                                fieldWithPath("exerciseTitle").type(JsonFieldType.STRING)
                                        .description("운동 제목입니다. 최대 255자까지 입력 가능합니다."),
                                fieldWithPath("exercisePersonalType").type(JsonFieldType.STRING).optional()
                                        .description("운동 종류입니다. 사용자가 입력하는 값이며 최대 255자까지 입력 가능합니다."),
                                fieldWithPath("exerciseLocation").type(JsonFieldType.STRING).optional()
                                        .description("운동 장소 입니다. 최대 255자까지 입력 가능합니다."),
                                fieldWithPath("exerciseDetail").type(JsonFieldType.STRING).optional()
                                        .description("운동에 대한 상세 내용입니다. 최대 65,535byte 까지 입력 가능합니다. 글자 수 기준이 아닙니다."),
                                fieldWithPath("exerciseStartedAt").type("Datetime")
                                        .description("시작일시 입니다. ISO 8601 형식으로 입력 받으며 오늘 이전 일시여야 합니다."),
                                fieldWithPath("exerciseEndedAt").type("Datetime")
                                        .description("종료일시 입니다.ISO 8601 형식으로 입력 받습니다. 오늘 이전 일시여야 하며 시작 일시 이후여야 합니다.")
                        ),
                        responseFields(RestDocsUtils.commonResponseFields(
                                    fieldWithPath("data.exerciseId").type(JsonFieldType.NUMBER)
                                            .description("운동 ID 입니다. 이미지 업로드 시 사용 가능합니다.")
                                )
                        )
                ))
                .andReturn().getResponse().getContentAsString();

        // then
        BDDMockito.then(exerciseCommandService).should(BDDMockito.times(1))
                .updateExercise(BDDMockito.eq(exerciseId), BDDMockito.any(UpdateExerciseRequestDto.class));
    }

    @Test
    @DisplayName("운동 기록 수정 - 실패 (exerciseId 음수)")
    void updateExercise_Fail_NegativeExerciseId() throws Exception {
        // given
        long invalidExerciseId = -1L;
        UUID testUserId = UUID.randomUUID();
        UpdateExerciseRequestDto requestDto = UpdateExerciseRequestDto.builder()
                .exerciseTitle("수정된 운동 제목")
                .exerciseStartedAt(LocalDateTime.now().minusHours(2))
                .exerciseEndedAt(LocalDateTime.now().minusHours(1))
                .build();

        HttpHeaders headers = new HttpHeaders();
        headers.add("X-USER-ID", testUserId.toString());
        headers.add("Authorization", "Bearer dummy-access-token-for-docs");

        // when & then
        this.mockMvc.perform(MockMvcRequestBuilders.patch("/v1/exercises/{exerciseId}", invalidExerciseId)
                        .content(objectMapper.writeValueAsString(requestDto))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .headers(headers))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("succeed").value(false))
                .andExpect(jsonPath("message").value("입력값 검증중 실패했습니다."));

        BDDMockito.then(exerciseCommandService).should(BDDMockito.never())
                .updateExercise(BDDMockito.anyLong(), BDDMockito.any(UpdateExerciseRequestDto.class));
    }

    @Test
    @DisplayName("운동 기록 수정 - 실패 (요청 DTO 유효성 위반 - 제목 공백)")
    void exerciseUpdate_validateFailed_BlankTitle() throws Exception{
        // given
        long exerciseId = 1L;
        UUID testUserId = UUID.randomUUID();
        UpdateExerciseRequestDto requestDto = UpdateExerciseRequestDto.builder()
                .exerciseTitle(" ") // 유효성 위반: 제목 공백
                .exerciseStartedAt(LocalDateTime.now().minusHours(2))
                .exerciseEndedAt(LocalDateTime.now().minusHours(1))
                .exerciseLocation("운동 장소")
                .exercisePersonalType("운동 종류")
                .exerciseDetail("운동 상세")
                .build();

        HttpHeaders headers = new HttpHeaders();
        headers.add("X-USER-ID", testUserId.toString());
        headers.add("Authorization", "Bearer dummy-access-token-for-docs");

        // when & then
        this.mockMvc.perform(MockMvcRequestBuilders.patch("/v1/exercises/{exerciseId}", exerciseId)
                        .content(objectMapper.writeValueAsString(requestDto))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .headers(headers))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("succeed").value(false))
                .andExpect(jsonPath("message").isNotEmpty());

        BDDMockito.then(exerciseCommandService).should(BDDMockito.never())
                .updateExercise(BDDMockito.anyLong(), BDDMockito.any(UpdateExerciseRequestDto.class));
    }

    @Test
    @DisplayName("운동 기록 수정 - 실패 (요청 DTO 유효성 위반 - 시작 시간이 종료 시간 이후)")
    void exerciseUpdate_validateFailed_InvalidStartEndTime() throws Exception{
        // given
        long exerciseId = 1L;
        UUID testUserId = UUID.randomUUID();
        UpdateExerciseRequestDto requestDto = UpdateExerciseRequestDto.builder()
                .exerciseTitle("유효한 제목")
                .exerciseStartedAt(LocalDateTime.now().minusHours(1)) // 시작 시간이 종료 시간보다 이후
                .exerciseEndedAt(LocalDateTime.now().minusHours(2))
                .exerciseLocation("운동 장소")
                .exercisePersonalType("운동 종류")
                .exerciseDetail("운동 상세")
                .build();

        HttpHeaders headers = new HttpHeaders();
        headers.add("X-USER-ID", testUserId.toString());
        headers.add("Authorization", "Bearer dummy-access-token-for-docs");

        // when & then
        this.mockMvc.perform(MockMvcRequestBuilders.patch("/v1/exercises/{exerciseId}", exerciseId)
                        .content(objectMapper.writeValueAsString(requestDto))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .headers(headers))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("succeed").value(false))
                .andExpect(jsonPath("message").isNotEmpty());

        BDDMockito.then(exerciseCommandService).should(BDDMockito.never())
                .updateExercise(BDDMockito.anyLong(), BDDMockito.any(UpdateExerciseRequestDto.class));
    }
}