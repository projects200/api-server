package com.project200.undabang.timer.custom.controller;

import com.project200.undabang.common.web.exception.CustomException;
import com.project200.undabang.common.web.exception.ErrorCode;
import com.project200.undabang.configuration.AbstractRestDocSupport;
import com.project200.undabang.timer.custom.dto.request.CustomTimerNameUpdateRequest;
import com.project200.undabang.timer.custom.dto.request.CustomTimerStepCreateRequest;
import com.project200.undabang.timer.custom.dto.request.CustomTimerUpdateRequest;
import com.project200.undabang.timer.custom.dto.response.CustomTimerDetailResponse;
import com.project200.undabang.timer.custom.dto.response.CustomTimerStepRecord;
import com.project200.undabang.timer.custom.service.CustomTimerCommandService;
import com.project200.undabang.timer.sync.TimerDeleteRequest;
import com.project200.undabang.timer.sync.TimerMutationResponse;
import com.project200.undabang.timer.sync.TimerSyncConflictException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static com.project200.undabang.configuration.HeadersGenerator.getCommonApiHeaders;
import static com.project200.undabang.configuration.RestDocsUtils.commonResponseFields;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CustomTimerCommandController.class)
class CustomTimerSynchronizationControllerTest extends AbstractRestDocSupport {
    private static final LocalDateTime UPDATED_AT = LocalDateTime.of(2026, 10, 19, 12, 0, 0, 123_000_000);

    @MockitoBean
    private CustomTimerCommandService customTimerCommandService;

    @Test
    @DisplayName("전체 교체는 편집 메타데이터와 서버 수정 시각을 전달한다")
    void replace() throws Exception {
        CustomTimerUpdateRequest request = new CustomTimerUpdateRequest("교체 루틴", steps(), UUID.randomUUID(), 200L);
        given(customTimerCommandService.updateCustomTimer(eq(1L), any(CustomTimerUpdateRequest.class)))
                .willReturn(new TimerMutationResponse(UPDATED_AT));

        mockMvc.perform(put("/api/v1/custom-timers/{customTimerId}", 1L)
                        .headers(getCommonApiHeaders(UUID.randomUUID()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("UPDATED"))
                .andExpect(jsonPath("$.data.updatedAt").value("2026-10-19T12:00:00.123"))
                .andDo(org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document("sync-custom-timer/replace",
                        requestFields(
                                fieldWithPath("customTimerName").description("전체 교체할 이름입니다."),
                                fieldWithPath("customTimerSteps").description("전체 교체할 활성 스텝 목록입니다."),
                                fieldWithPath("customTimerSteps[].customTimerStepName").description("스텝 이름입니다."),
                                fieldWithPath("customTimerSteps[].customTimerStepOrder").description("0부터 시작하는 연속 순서입니다."),
                                fieldWithPath("customTimerSteps[].customTimerStepTime").description("스텝 시간(초)입니다."),
                                fieldWithPath("editId").description("이 전체 교체 요청의 UUID입니다."),
                                fieldWithPath("editedAt").description("기기 편집 시각의 UTC epoch 밀리초입니다.")
                        ),
                        responseFields(commonResponseFields(
                                fieldWithPath("data.updatedAt").description("이 요청이 최초 성공했을 때의 서버 수정 시각입니다.")
                        ))));
    }

    @Test
    @DisplayName("이름 수정은 편집 메타데이터와 서버 수정 시각을 전달한다")
    void rename() throws Exception {
        CustomTimerNameUpdateRequest request = new CustomTimerNameUpdateRequest("새 이름", UUID.randomUUID(), 200L);
        given(customTimerCommandService.updateCustomTimerName(eq(1L), any(CustomTimerNameUpdateRequest.class)))
                .willReturn(new TimerMutationResponse(UPDATED_AT));

        mockMvc.perform(patch("/api/v1/custom-timers/{customTimerId}", 1L)
                        .headers(getCommonApiHeaders(UUID.randomUUID()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("UPDATED"))
                .andExpect(jsonPath("$.data.updatedAt").value("2026-10-19T12:00:00.123"))
                .andDo(org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document("sync-custom-timer/rename",
                        requestFields(
                                fieldWithPath("customTimerName").description("새 이름입니다. 스텝은 유지합니다."),
                                fieldWithPath("editId").description("이 이름 수정 요청의 UUID입니다."),
                                fieldWithPath("editedAt").description("기기 편집 시각의 UTC epoch 밀리초입니다.")
                        ),
                        responseFields(commonResponseFields(
                                fieldWithPath("data.updatedAt").description("이 요청이 최초 성공했을 때의 서버 수정 시각입니다.")
                        ))));
    }

    @Test
    @DisplayName("선택 DELETE 본문을 전달하면 최초 삭제 시각을 반환한다")
    void deleteWithMetadata() throws Exception {
        TimerDeleteRequest request = new TimerDeleteRequest(UUID.randomUUID(), 100L);
        given(customTimerCommandService.deleteCustomTimer(1L, request)).willReturn(new TimerMutationResponse(UPDATED_AT));

        mockMvc.perform(delete("/api/v1/custom-timers/{customTimerId}", 1L)
                        .headers(getCommonApiHeaders(UUID.randomUUID()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("DELETED"))
                .andExpect(jsonPath("$.data.updatedAt").value("2026-10-19T12:00:00.123"))
                .andDo(org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document("sync-custom-timer/delete",
                        requestFields(
                                fieldWithPath("editId").description("이 삭제 요청의 UUID입니다."),
                                fieldWithPath("editedAt").description("기기 삭제 시각의 UTC epoch 밀리초입니다. 삭제는 더 최신인 편집보다 우선합니다.")
                        ),
                        responseFields(commonResponseFields(
                                fieldWithPath("data.updatedAt").description("삭제 응답의 서버 수정 시각입니다.")
                        ))));
        then(customTimerCommandService).should().deleteCustomTimer(1L, request);
    }

    @Test
    @DisplayName("오래된 변경의 409 응답은 현재 이름과 활성 스텝 전체 및 수정 시각을 포함한다")
    void conflict() throws Exception {
        CustomTimerDetailResponse current = CustomTimerDetailResponse.builder().customTimerId(1L)
                .customTimerName("최신 이름").updatedAt(UPDATED_AT).customTimerStepCount(1)
                .customTimerSteps(List.of(new CustomTimerStepRecord(9L, "최신 스텝", (byte) 0, 90))).build();
        given(customTimerCommandService.updateCustomTimer(eq(1L), any(CustomTimerUpdateRequest.class)))
                .willThrow(new TimerSyncConflictException(current));
        CustomTimerUpdateRequest request = new CustomTimerUpdateRequest("오래된 이름", steps(), UUID.randomUUID(), 100L);

        mockMvc.perform(put("/api/v1/custom-timers/{customTimerId}", 1L)
                        .headers(getCommonApiHeaders(UUID.randomUUID()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.succeed").value(false))
                .andExpect(jsonPath("$.code").value("TIMER_EDIT_CONFLICT"))
                .andExpect(jsonPath("$.data.customTimerId").value(1L))
                .andExpect(jsonPath("$.data.customTimerName").value("최신 이름"))
                .andExpect(jsonPath("$.data.updatedAt").value("2026-10-19T12:00:00.123"))
                .andExpect(jsonPath("$.data.customTimerStepCount").value(1))
                .andExpect(jsonPath("$.data.customTimerSteps[0].customTimerStepId").value(9L))
                .andExpect(jsonPath("$.data.customTimerSteps[0].customTimerStepName").value("최신 스텝"))
                .andExpect(jsonPath("$.data.customTimerSteps[0].customTimerStepTime").value(90))
                .andExpect(jsonPath("$.data.editedAt").doesNotExist())
                .andExpect(jsonPath("$.data.customTimerDeletedAt").doesNotExist())
                .andDo(org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document("sync-custom-timer/conflict",
                        responseFields(commonResponseFields(
                                fieldWithPath("data.customTimerId").description("현재 타이머 ID입니다."),
                                fieldWithPath("data.customTimerName").description("현재 이름입니다."),
                                fieldWithPath("data.updatedAt").description("현재 서버 수정 시각입니다."),
                                fieldWithPath("data.customTimerStepCount").description("현재 활성 스텝 수입니다."),
                                fieldWithPath("data.customTimerSteps").description("현재 활성 스텝 전체입니다."),
                                fieldWithPath("data.customTimerSteps[].customTimerStepId").description("현재 스텝 ID입니다."),
                                fieldWithPath("data.customTimerSteps[].customTimerStepName").description("현재 스텝 이름입니다."),
                                fieldWithPath("data.customTimerSteps[].customTimerStepOrder").description("현재 스텝 순서입니다."),
                                fieldWithPath("data.customTimerSteps[].customTimerStepTime").description("현재 스텝 시간(초)입니다.")
                        ))));
    }

    @Test
    @DisplayName("삭제된 대상의 전체 교체와 이름 수정은 410을 전달한다")
    void deletedEdits() throws Exception {
        given(customTimerCommandService.updateCustomTimer(eq(1L), any(CustomTimerUpdateRequest.class)))
                .willThrow(new CustomException(ErrorCode.TIMER_DELETED));
        given(customTimerCommandService.updateCustomTimerName(eq(1L), any(CustomTimerNameUpdateRequest.class)))
                .willThrow(new CustomException(ErrorCode.TIMER_DELETED));

        mockMvc.perform(put("/api/v1/custom-timers/{customTimerId}", 1L)
                        .headers(getCommonApiHeaders(UUID.randomUUID())).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CustomTimerUpdateRequest("이름", steps(), UUID.randomUUID(), 100L))))
                .andExpect(status().isGone()).andExpect(jsonPath("$.code").value("TIMER_DELETED"));
        mockMvc.perform(patch("/api/v1/custom-timers/{customTimerId}", 1L)
                        .headers(getCommonApiHeaders(UUID.randomUUID())).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CustomTimerNameUpdateRequest("이름", UUID.randomUUID(), 100L))))
                .andExpect(status().isGone()).andExpect(jsonPath("$.code").value("TIMER_DELETED"));
    }

    @Test
    @DisplayName("빈 DELETE 메타데이터 본문은 기존 null 데이터 응답을 유지한다")
    void emptyDeleteBodyRemainsLegacy() throws Exception {
        given(customTimerCommandService.deleteCustomTimer(1L, new TimerDeleteRequest(null, null))).willReturn(null);

        mockMvc.perform(delete("/api/v1/custom-timers/{customTimerId}", 1L)
                        .headers(getCommonApiHeaders(UUID.randomUUID())).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value("DELETED"))
                .andExpect(jsonPath("$.data").value(org.hamcrest.Matchers.nullValue()));
    }

    private List<CustomTimerStepCreateRequest> steps() {
        return List.of(new CustomTimerStepCreateRequest("운동", (byte) 0, 60));
    }
}
