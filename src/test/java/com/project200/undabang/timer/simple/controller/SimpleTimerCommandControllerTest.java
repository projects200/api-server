package com.project200.undabang.timer.simple.controller;

import com.project200.undabang.common.web.exception.CustomException;
import com.project200.undabang.common.web.exception.ErrorCode;
import com.project200.undabang.configuration.AbstractRestDocSupport;
import com.project200.undabang.configuration.RestDocsUtils;
import com.project200.undabang.timer.simple.dto.request.SimpleTimerCreateRequestDto;
import com.project200.undabang.timer.simple.dto.request.SimpleTimerUpdateRequestDto;
import com.project200.undabang.timer.simple.dto.response.SimpleTimerCreateResponseDto;
import com.project200.undabang.timer.simple.dto.response.SimpleTimerRecord;
import com.project200.undabang.timer.simple.service.SimpleTimerCommandService;
import com.project200.undabang.timer.sync.TimerDeleteRequest;
import com.project200.undabang.timer.sync.TimerMutationResponse;
import com.project200.undabang.timer.sync.TimerSyncConflictException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.BDDMockito;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;
import java.util.UUID;

import static com.project200.undabang.configuration.DocumentFormatGenerator.getTypeFormat;
import static com.project200.undabang.configuration.HeadersGenerator.getCommonApiHeaders;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SimpleTimerCommandController.class)
class SimpleTimerCommandControllerTest extends AbstractRestDocSupport {

    @MockitoBean
    private SimpleTimerCommandService simpleTimerCommandService;

    private static final LocalDateTime UPDATED_AT = LocalDateTime.of(2026, 10, 19, 10, 0, 0, 123_000_000);

    @Nested
    @DisplayName("심플 타이머 생성 API")
    class CreateSimpleTimer {

        @Test
        @DisplayName("성공: 유효한 요청 시 심플 타이머를 성공적으로 생성하고 201 Created를 반환한다")
        void createSimpleTimer_Success() throws Exception {
            // given
            UUID memberId = UUID.randomUUID();
            SimpleTimerCreateResponseDto responseDto = SimpleTimerCreateResponseDto.builder()
                    .simpleTimerId(1L).updatedAt(UPDATED_AT).build();

            BDDMockito.given(simpleTimerCommandService.createSimpleTimer(any(SimpleTimerCreateRequestDto.class)))
                    .willReturn(responseDto);

            // when & then
            mockMvc.perform(post("/api/v1/simple-timers")
                            .contentType(MediaType.APPLICATION_JSON)
                            .accept(MediaType.APPLICATION_JSON)
                            .headers(getCommonApiHeaders(memberId))
                            .content("{\"time\":120}"))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.succeed").value(true))
                    .andExpect(jsonPath("$.code").value("CREATED"))
                    .andExpect(jsonPath("$.message").value("리소스가 성공적으로 생성되었습니다."))
                    .andExpect(jsonPath("$.data.simpleTimerId").value(1L))
                    .andExpect(jsonPath("$.data.updatedAt").value(UPDATED_AT.toString()))
                    .andDo(print())
                    .andDo(document.document(
                            requestHeaders(
                                    RestDocsUtils.HEADER_ACCESS_TOKEN
                            ),
                            requestFields(
                                    fieldWithPath("time").type(JsonFieldType.NUMBER)
                                            .description("생성할 타이머의 시간(초)을 의미합니다. 1 ~ 3599 사이의 값을 입력해야 합니다."),
                                    fieldWithPath("createRequestId").type(JsonFieldType.STRING).optional()
                                            .description("생성 멱등 키(UUID). 같은 회원의 두 타이머 종류에서 공유합니다."),
                                    fieldWithPath("editedAt").type(JsonFieldType.NUMBER).optional()
                                            .description("기기 수정 시각(0 이상 UTC Unix 밀리초). createRequestId가 필요합니다.")
                            ),
                            responseFields(
                                    fieldWithPath("succeed").type(JsonFieldType.BOOLEAN).description("요청 성공 여부"),
                                    fieldWithPath("code").type(JsonFieldType.STRING).description("결과 코드"),
                                    fieldWithPath("message").type(JsonFieldType.STRING).description("결과 메시지"),
                                    fieldWithPath("data.simpleTimerId").type(JsonFieldType.NUMBER)
                                            .description("생성된 심플 타이머의 ID"),
                                    fieldWithPath("data.updatedAt").type(JsonFieldType.STRING)
                                            .description("원래 생성 응답의 서버 수정 시각(ISO 로컬 일시, 밀리초 정밀도)")
                            )
                    ));

            BDDMockito.then(simpleTimerCommandService).should().createSimpleTimer(any(SimpleTimerCreateRequestDto.class));
        }

        @Test
        @DisplayName("실패: 시간 값이 3599보다 크면 유효성 검증에 실패하고 400 Bad Request를 반환한다")
        void createSimpleTimer_Fail_ValidationMax() throws Exception {
            // given
            UUID memberId = UUID.randomUUID();
            // SimpleTimerCreateRequestDto의 time 필드에 @Max(3599)가 있다고 가정
            SimpleTimerCreateRequestDto requestDto = SimpleTimerCreateRequestDto.builder().time(3600).build();

            // when & then
            mockMvc.perform(post("/api/v1/simple-timers")
                            .contentType(MediaType.APPLICATION_JSON)
                            .headers(getCommonApiHeaders(memberId))
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andExpect(status().isBadRequest());

            BDDMockito.then(simpleTimerCommandService).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("실패: 타이머 개수가 최대치(6개)일 경우 409 Conflict를 반환한다")
        void createSimpleTimer_Fail_MaxCountViolation() throws Exception {
            // given
            UUID memberId = UUID.randomUUID();
            SimpleTimerCreateRequestDto requestDto = SimpleTimerCreateRequestDto.builder().time(120).build();

            BDDMockito.given(simpleTimerCommandService.createSimpleTimer(any(SimpleTimerCreateRequestDto.class)))
                    .willThrow(new CustomException(ErrorCode.SIMPLE_TIMER_MAX_COUNT_VIOLATION));

            // when & then
            mockMvc.perform(post("/api/v1/simple-timers")
                            .contentType(MediaType.APPLICATION_JSON)
                            .headers(getCommonApiHeaders(memberId))
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value(ErrorCode.SIMPLE_TIMER_MAX_COUNT_VIOLATION.getCode()))
                    .andExpect(jsonPath("$.message").value(ErrorCode.SIMPLE_TIMER_MAX_COUNT_VIOLATION.getMessage()));

            BDDMockito.then(simpleTimerCommandService).should().createSimpleTimer(any(SimpleTimerCreateRequestDto.class));
        }
    }

    @Nested
    @DisplayName("심플 타이머 삭제 API")
    class DeleteSimpleTimer {

        @Test
        @DisplayName("성공: 유효한 요청 시 심플 타이머를 성공적으로 삭제하고 200 Ok를 반환한다")
        void deleteSimpleTimer_Success() throws Exception {
            // given
            Long simpleTimerId = 1L;
            UUID memberId = UUID.randomUUID();


            // when & then
            mockMvc.perform(delete("/api/v1/simple-timers/{simpleTimerId}", simpleTimerId)
                            .headers(getCommonApiHeaders(memberId)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.succeed").value(true))
                    .andExpect(jsonPath("$.code").value("DELETED"))
                    .andExpect(jsonPath("$.message").value("리소스가 성공적으로 삭제되었습니다."))
                    .andExpect(jsonPath("$.data").doesNotExist())
                    .andDo(print())
                    .andDo(document.document(
                            requestHeaders(
                                    RestDocsUtils.HEADER_ACCESS_TOKEN
                            ),
                            pathParameters(
                                    parameterWithName("simpleTimerId").attributes(getTypeFormat(JsonFieldType.NUMBER))
                                            .description("삭제할 심플 타이머의 ID를 의미합니다.")
                            ),
                            responseFields(
                                    RestDocsUtils.commonResponseFieldsOnly()
                            )
                    ));

            BDDMockito.then(simpleTimerCommandService).should().deleteSimpleTimer(simpleTimerId, null);
        }


        @Test
        @DisplayName("실패: 존재하지 않는 타이머 ID로 요청하면 404 Not Found를 반환한다")
        void deleteSimpleTimer_Fail_NotFound() throws Exception {
            // given
            Long nonExistentTimerId = 999L;
            UUID memberId = UUID.randomUUID();

            willThrow(new CustomException(ErrorCode.SIMPLE_TIMER_NOT_FOUND))
                    .given(simpleTimerCommandService).deleteSimpleTimer(nonExistentTimerId, null);

            // when & then
            mockMvc.perform(delete("/api/v1/simple-timers/{simpleTimerId}", nonExistentTimerId)
                            .headers(getCommonApiHeaders(memberId)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value(ErrorCode.SIMPLE_TIMER_NOT_FOUND.getCode()))
                    .andExpect(jsonPath("$.message").value(ErrorCode.SIMPLE_TIMER_NOT_FOUND.getMessage()));

            BDDMockito.then(simpleTimerCommandService).should().deleteSimpleTimer(nonExistentTimerId, null);
        }

        @Test
        @DisplayName("실패: 존재하지 않는 회원 ID로 요청하면 404 Not Found를 반환한다")
        void deleteSimpleTimer_Fail_MemberNotFound() throws Exception {
            // given
            Long simpleTimerId = 1L;
            UUID nonExistentMemberId = UUID.randomUUID();

            willThrow(new CustomException(ErrorCode.MEMBER_NOT_FOUND))
                    .given(simpleTimerCommandService).deleteSimpleTimer(simpleTimerId, null);

            // when & then
            mockMvc.perform(delete("/api/v1/simple-timers/{simpleTimerId}", simpleTimerId)
                            .headers(getCommonApiHeaders(nonExistentMemberId)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value(ErrorCode.MEMBER_NOT_FOUND.getCode()))
                    .andExpect(jsonPath("$.message").value(ErrorCode.MEMBER_NOT_FOUND.getMessage()));

            BDDMockito.then(simpleTimerCommandService).should().deleteSimpleTimer(simpleTimerId, null);
        }

    }

    @Nested
    @DisplayName("심플 타이머 수정 API")
    class UpdateSimpleTimer {

        @Test
        @DisplayName("성공: 유효한 요청 시 심플 타이머를 성공적으로 수정하고 200 OK를 반환한다")
        void updateSimpleTimer_Success() throws Exception {
            // given
            Long simpleTimerId = 1L;
            UUID memberId = UUID.randomUUID();
            // when & then
            mockMvc.perform(patch("/api/v1/simple-timers/{simpleTimerId}", simpleTimerId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .headers(getCommonApiHeaders(memberId))
                            .content("{\"time\":180}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.succeed").value(true))
                    .andExpect(jsonPath("$.code").value("UPDATED"))
                    .andExpect(jsonPath("$.message").value("리소스가 성공적으로 수정되었습니다."))
                    .andExpect(jsonPath("$.data").doesNotExist())
                    .andDo(print())
                    .andDo(document.document(
                            requestHeaders(
                                    RestDocsUtils.HEADER_ACCESS_TOKEN
                            ),
                            pathParameters(
                                    parameterWithName("simpleTimerId").attributes(getTypeFormat(JsonFieldType.NUMBER))
                                            .description("수정할 심플 타이머의 ID를 의미합니다.")
                            ),
                            requestFields(
                                    fieldWithPath("time").type(JsonFieldType.NUMBER)
                                            .description("수정할 시간을 의미합니다. 1 ~ 3599 사이의 값을 입력하셔야 합니다."),
                                    fieldWithPath("editId").type(JsonFieldType.STRING).optional()
                                            .description("수정 멱등 키(UUID). editedAt과 함께 보내야 합니다."),
                                    fieldWithPath("editedAt").type(JsonFieldType.NUMBER).optional()
                                            .description("기기 수정 시각(0 이상 UTC Unix 밀리초). editId와 함께 보내야 합니다.")
                            ),
                            responseFields(
                                    RestDocsUtils.commonResponseFieldsOnly()
                            )
                    ));

            BDDMockito.then(simpleTimerCommandService).should().updateSimpleTimer(eq(simpleTimerId), any(SimpleTimerUpdateRequestDto.class));
        }

        @Test
        @DisplayName("실패: 시간 값이 1보다 작으면 유효성 검증에 실패하고 400 Bad Request를 반환한다")
        void updateSimpleTimer_Fail_ValidationMin() throws Exception {
            // given
            Long simpleTimerId = 1L;
            UUID memberId = UUID.randomUUID();
            SimpleTimerUpdateRequestDto requestDto = new SimpleTimerUpdateRequestDto(0, null, null);

            // when & then
            mockMvc.perform(patch("/api/v1/simple-timers/{simpleTimerId}", simpleTimerId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .headers(getCommonApiHeaders(memberId))
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andExpect(status().isBadRequest());

            BDDMockito.then(simpleTimerCommandService).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("실패: 존재하지 않는 타이머 ID로 요청하면 404 Not Found를 반환한다")
        void updateSimpleTimer_Fail_NotFound() throws Exception {
            // given
            Long nonExistentTimerId = 999L;
            UUID memberId = UUID.randomUUID();
            SimpleTimerUpdateRequestDto requestDto = new SimpleTimerUpdateRequestDto(180, null, null);

            BDDMockito.willThrow(new CustomException(ErrorCode.SIMPLE_TIMER_NOT_FOUND))
                    .given(simpleTimerCommandService).updateSimpleTimer(eq(nonExistentTimerId), any(SimpleTimerUpdateRequestDto.class));

            // when & then
            mockMvc.perform(patch("/api/v1/simple-timers/{simpleTimerId}", nonExistentTimerId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .headers(getCommonApiHeaders(memberId))
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value(ErrorCode.SIMPLE_TIMER_NOT_FOUND.getCode()))
                    .andExpect(jsonPath("$.message").value(ErrorCode.SIMPLE_TIMER_NOT_FOUND.getMessage()));

            BDDMockito.then(simpleTimerCommandService).should().updateSimpleTimer(eq(nonExistentTimerId), any(SimpleTimerUpdateRequestDto.class));
        }
    }

    @Nested
    @DisplayName("심플 타이머 동기화 API")
    class SimpleTimerSynchronization {
        @Test
        @DisplayName("생성 메타데이터를 서비스에 전달하고 ID와 원래 생성 시각을 반환한다")
        void createSyncedSimpleTimer() throws Exception {
            UUID memberId = UUID.randomUUID();
            UUID requestId = UUID.randomUUID();
            SimpleTimerCreateRequestDto request = new SimpleTimerCreateRequestDto(120, requestId, 1792404000123L);
            BDDMockito.given(simpleTimerCommandService.createSimpleTimer(any()))
                    .willReturn(new SimpleTimerCreateResponseDto(1L, UPDATED_AT));

            mockMvc.perform(post("/api/v1/simple-timers").headers(getCommonApiHeaders(memberId))
                            .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.data.simpleTimerId").value(1L))
                    .andExpect(jsonPath("$.data.updatedAt").value(UPDATED_AT.toString()))
                    .andDo(document.document(
                            requestHeaders(RestDocsUtils.HEADER_ACCESS_TOKEN),
                            requestFields(
                                    fieldWithPath("time").type(JsonFieldType.NUMBER).description("타이머 시간(1~3599초)"),
                                    fieldWithPath("createRequestId").type(JsonFieldType.STRING).optional()
                                            .description("생성 멱등 키(UUID). 재시도 시 같은 값을 사용합니다."),
                                    fieldWithPath("editedAt").type(JsonFieldType.NUMBER).optional()
                                            .description("기기 수정 시각(0 이상 UTC Unix 밀리초). 생략 시 서버 현재 시각을 사용합니다.")
                            ),
                            responseFields(RestDocsUtils.commonResponseFields(
                                    fieldWithPath("data.simpleTimerId").type(JsonFieldType.NUMBER).description("최초 할당한 타이머 ID"),
                                    fieldWithPath("data.updatedAt").type(JsonFieldType.STRING).description("최초 생성 응답의 서버 수정 시각")
                            ))
                    ));
            BDDMockito.then(simpleTimerCommandService).should().createSimpleTimer(
                    org.mockito.ArgumentMatchers.argThat(dto -> dto.getCreateRequestId().equals(requestId)
                            && dto.getEditedAt().equals(request.getEditedAt()) && dto.getTime().equals(120)));
        }

        @Test
        @DisplayName("동기화 수정은 서버 수정 시각을 반환한다")
        void updateSyncedSimpleTimer() throws Exception {
            UUID editId = UUID.randomUUID();
            SimpleTimerUpdateRequestDto request = new SimpleTimerUpdateRequestDto(180, editId, 1792404000123L);
            BDDMockito.given(simpleTimerCommandService.updateSimpleTimer(eq(1L), any()))
                    .willReturn(new TimerMutationResponse(UPDATED_AT));

            mockMvc.perform(patch("/api/v1/simple-timers/{simpleTimerId}", 1L)
                            .headers(getCommonApiHeaders(UUID.randomUUID())).contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("UPDATED"))
                    .andExpect(jsonPath("$.data.updatedAt").value(UPDATED_AT.toString()))
                    .andDo(document.document(
                            requestHeaders(RestDocsUtils.HEADER_ACCESS_TOKEN),
                            pathParameters(
                                    parameterWithName("simpleTimerId").attributes(getTypeFormat(JsonFieldType.NUMBER))
                                            .description("수정할 타이머 ID")
                            ),
                            requestFields(
                                    fieldWithPath("time").type(JsonFieldType.NUMBER).description("수정할 시간(1~3599초)"),
                                    fieldWithPath("editId").type(JsonFieldType.STRING).optional().description("수정 멱등 키(UUID)"),
                                    fieldWithPath("editedAt").type(JsonFieldType.NUMBER).optional()
                                            .description("기기 수정 시각(0 이상 UTC Unix 밀리초). editId와 함께 보내야 합니다.")
                            ),
                            responseFields(RestDocsUtils.commonResponseFields(
                                    fieldWithPath("data.updatedAt").type(JsonFieldType.STRING).description("해당 요청의 서버 수정 시각")
                            ))
                    ));
            BDDMockito.then(simpleTimerCommandService).should().updateSimpleTimer(eq(1L),
                    org.mockito.ArgumentMatchers.argThat(dto -> dto.getEditId().equals(editId)
                            && dto.getEditedAt().equals(request.getEditedAt()) && dto.getTime() == 180));
        }

        @Test
        @DisplayName("삭제의 선택적 JSON 본문을 전달하고 서버 수정 시각을 반환한다")
        void deleteSyncedSimpleTimer() throws Exception {
            TimerDeleteRequest request = new TimerDeleteRequest(UUID.randomUUID(), 1792404000123L);
            BDDMockito.given(simpleTimerCommandService.deleteSimpleTimer(1L, request))
                    .willReturn(new TimerMutationResponse(UPDATED_AT));

            mockMvc.perform(delete("/api/v1/simple-timers/{simpleTimerId}", 1L)
                            .headers(getCommonApiHeaders(UUID.randomUUID())).contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("DELETED"))
                    .andExpect(jsonPath("$.data.updatedAt").value(UPDATED_AT.toString()))
                    .andDo(document.document(
                            requestHeaders(RestDocsUtils.HEADER_ACCESS_TOKEN),
                            pathParameters(
                                    parameterWithName("simpleTimerId").attributes(getTypeFormat(JsonFieldType.NUMBER))
                                            .description("삭제할 타이머 ID")
                            ),
                            requestFields(
                                    fieldWithPath("editId").type(JsonFieldType.STRING).optional().description("삭제 멱등 키(UUID)"),
                                    fieldWithPath("editedAt").type(JsonFieldType.NUMBER).optional()
                                            .description("기기 수정 시각(0 이상 UTC Unix 밀리초). editId와 함께 보내야 합니다.")
                            ),
                            responseFields(RestDocsUtils.commonResponseFields(
                                    fieldWithPath("data.updatedAt").type(JsonFieldType.STRING)
                                            .description("해당 삭제 요청의 서버 수정 시각. 이미 삭제되었다면 기존 삭제 시각")
                            ))
                    ));
            BDDMockito.then(simpleTimerCommandService).should().deleteSimpleTimer(1L, request);
        }

        @Test
        @DisplayName("오래된 수정의 409 응답은 현재 타이머 값을 포함한다")
        void staleSimpleTimerEdit() throws Exception {
            BDDMockito.given(simpleTimerCommandService.updateSimpleTimer(eq(1L), any()))
                    .willThrow(new TimerSyncConflictException(new SimpleTimerRecord(1L, 300, UPDATED_AT)));
            SimpleTimerUpdateRequestDto request = new SimpleTimerUpdateRequestDto(180, UUID.randomUUID(), 1L);

            mockMvc.perform(patch("/api/v1/simple-timers/{simpleTimerId}", 1L)
                            .headers(getCommonApiHeaders(UUID.randomUUID())).contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.succeed").value(false))
                    .andExpect(jsonPath("$.code").value(ErrorCode.TIMER_EDIT_CONFLICT.getCode()))
                    .andExpect(jsonPath("$.data.simpleTimerId").value(1L))
                    .andExpect(jsonPath("$.data.time").value(300))
                    .andExpect(jsonPath("$.data.updatedAt").value(UPDATED_AT.toString()))
                    .andExpect(jsonPath("$.data.editedAt").doesNotExist())
                    .andDo(document.document(
                            responseFields(RestDocsUtils.commonResponseFields(
                                    fieldWithPath("data.simpleTimerId").type(JsonFieldType.NUMBER).description("현재 타이머 ID"),
                                    fieldWithPath("data.time").type(JsonFieldType.NUMBER).description("현재 타이머 시간(초)"),
                                    fieldWithPath("data.updatedAt").type(JsonFieldType.STRING).description("현재 서버 수정 시각")
                            ))
                    ));
        }

        @Test
        @DisplayName("삭제된 타이머의 새 수정은 410이다")
        void deletedSimpleTimerEdit() throws Exception {
            BDDMockito.given(simpleTimerCommandService.updateSimpleTimer(eq(1L), any()))
                    .willThrow(new CustomException(ErrorCode.TIMER_DELETED));

            mockMvc.perform(patch("/api/v1/simple-timers/{simpleTimerId}", 1L)
                            .headers(getCommonApiHeaders(UUID.randomUUID())).contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new SimpleTimerUpdateRequestDto(180, UUID.randomUUID(), 1L))))
                    .andExpect(status().isGone())
                    .andExpect(jsonPath("$.code").value(ErrorCode.TIMER_DELETED.getCode()));
        }

        @Test
        @DisplayName("빈 삭제 JSON 본문도 null 데이터의 기존 응답을 유지한다")
        void emptyDeleteBodyKeepsLegacyResponse() throws Exception {
            mockMvc.perform(delete("/api/v1/simple-timers/{simpleTimerId}", 1L)
                            .headers(getCommonApiHeaders(UUID.randomUUID())).contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("DELETED"))
                    .andExpect(jsonPath("$.data").doesNotExist());
            BDDMockito.then(simpleTimerCommandService).should().deleteSimpleTimer(1L, new TimerDeleteRequest(null, null));
        }

        @Test
        @DisplayName("UUID가 아닌 메타데이터는 400이고 서비스를 호출하지 않는다")
        void malformedMetadataIsBadRequest() throws Exception {
            mockMvc.perform(delete("/api/v1/simple-timers/{simpleTimerId}", 1L)
                            .headers(getCommonApiHeaders(UUID.randomUUID())).contentType(MediaType.APPLICATION_JSON)
                            .content("{\"editId\":\"not-a-uuid\",\"editedAt\":1}"))
                    .andExpect(status().isBadRequest());
            BDDMockito.then(simpleTimerCommandService).shouldHaveNoInteractions();
        }
    }
}