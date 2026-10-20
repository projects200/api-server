package com.project200.undabang.timer.custom.controller;

import com.project200.undabang.common.web.exception.CustomException;
import com.project200.undabang.common.web.exception.ErrorCode;
import com.project200.undabang.configuration.AbstractRestDocSupport;
import com.project200.undabang.configuration.RestDocsUtils;
import com.project200.undabang.timer.custom.dto.request.CustomTimerCreateRequest;
import com.project200.undabang.timer.custom.dto.request.CustomTimerNameUpdateRequest;
import com.project200.undabang.timer.custom.dto.request.CustomTimerStepCreateRequest;
import com.project200.undabang.timer.custom.dto.request.CustomTimerUpdateRequest;
import com.project200.undabang.timer.custom.dto.response.CustomTimerCreateResponse;
import com.project200.undabang.timer.custom.service.CustomTimerCommandService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static com.project200.undabang.configuration.DocumentFormatGenerator.getTypeFormat;
import static com.project200.undabang.configuration.HeadersGenerator.getCommonApiHeaders;
import static com.project200.undabang.configuration.RestDocsUtils.HEADER_ACCESS_TOKEN;
import static com.project200.undabang.configuration.RestDocsUtils.commonResponseFields;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.doThrow;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.patch;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CustomTimerCommandController.class)
class CustomTimerCommandControllerTest extends AbstractRestDocSupport {
    private static final LocalDateTime UPDATED_AT = LocalDateTime.of(2026, 10, 19, 12, 0, 0, 123_000_000);


    @MockitoBean
    private CustomTimerCommandService customTimerCommandService;

    @Nested
    @DisplayName("커스텀 타이머 생성 성공")
    class CreateCustomTimer {

        @Test
        @DisplayName("커스텀 타이머를 성공적으로 생성한다")
        void createCustomTimer_Success() throws Exception {
            // given
            UUID memberId = UUID.randomUUID();

            // 요청 본문 DTO 생성
            List<CustomTimerStepCreateRequest> steps = List.of(
                    new CustomTimerStepCreateRequest("데드리프트", (byte) 0, 180),
                    new CustomTimerStepCreateRequest("휴식", (byte) 1, 60),
                    new CustomTimerStepCreateRequest("스쿼트", (byte) 2, 120)
            );
            CustomTimerCreateRequest request = new CustomTimerCreateRequest("3대 운동 루틴", steps, UUID.randomUUID(), 1_792_368_000_123L);

            // 서비스 계층의 응답 객체 생성
            CustomTimerCreateResponse expectedResponse = new CustomTimerCreateResponse(1L, UPDATED_AT);

            // 서비스 메소드가 어떤 요청이든(any) 받으면, 미리 정의된 응답을 반환하도록 설정
            given(customTimerCommandService.createCustomTimer(any(CustomTimerCreateRequest.class)))
                    .willReturn(expectedResponse);

            // when & then
            mockMvc.perform(post("/api/v1/custom-timers") // POST 요청
                            .contentType(MediaType.APPLICATION_JSON)
                            .accept(MediaType.APPLICATION_JSON)
                            .headers(getCommonApiHeaders(memberId))
                            .content(objectMapper.writeValueAsString(request))) // 요청 본문 추가
                    .andExpectAll(
                            status().isCreated(), // 201 Created 상태 코드를 기대
                            jsonPath("$.succeed").value(true),
                            jsonPath("$.code").value("CREATED"),
                            jsonPath("$.data.customTimerId").value(expectedResponse.customTimerId()),
                            jsonPath("$.data.updatedAt").value("2026-10-19T12:00:00.123")
                    )
                    .andDo(document.document(
                            requestHeaders(HEADER_ACCESS_TOKEN),
                            // 요청 본문의 필드를 문서화합니다.
                            requestFields(
                                    fieldWithPath("customTimerName").description("생성할 커스텀 타이머의 이름입니다."),
                                    fieldWithPath("customTimerSteps").description("커스텀 타이머에 포함될 스텝 목록입니다."),
                                    fieldWithPath("customTimerSteps[].customTimerStepName").description("스텝의 이름입니다."),
                                    fieldWithPath("customTimerSteps[].customTimerStepOrder").description("스텝의 순서입니다."),
                                    fieldWithPath("customTimerSteps[].customTimerStepTime").description("스텝의 시간(초)입니다."),
                                    fieldWithPath("createRequestId").type(JsonFieldType.STRING).optional().description("선택 UUID. 같은 회원의 생성 요청 재전송을 식별합니다."),
                                    fieldWithPath("editedAt").type(JsonFieldType.NUMBER).optional().description("선택 UTC epoch 밀리초. createRequestId가 필요하며 생략 시 서버 시각을 사용합니다.")
                            ),
                            // 응답 본문의 필드를 문서화합니다.
                            responseFields(commonResponseFields(
                                    fieldWithPath("data.customTimerId").description("생성 요청에 최초 할당된 커스텀 타이머의 ID입니다."),
                                    fieldWithPath("data.updatedAt").description("최초 생성 응답의 서버 수정 시각입니다. 밀리초 정밀도입니다.")
                            ))
                    ));

            // 서비스 메소드가 정확히 1번 호출되었는지 검증
            then(customTimerCommandService).should().createCustomTimer(any(CustomTimerCreateRequest.class));
            then(customTimerCommandService).shouldHaveNoMoreInteractions();
        }

        @Test
        @DisplayName("타이머 이름이 비어있으면 400 에러를 반환한다")
        void shouldFail_whenNameIsBlank() throws Exception {
            // given
            UUID memberId = UUID.randomUUID();
            List<CustomTimerStepCreateRequest> steps = List.of(new CustomTimerStepCreateRequest("스텝 1", (byte) 1, 30));
            CustomTimerCreateRequest request = new CustomTimerCreateRequest("", steps, null, null); // Invalid: blank name

            // when & then
            mockMvc.perform(post("/api/v1/custom-timers")
                            .contentType(MediaType.APPLICATION_JSON)
                            .headers(getCommonApiHeaders(memberId))
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());

            then(customTimerCommandService).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("타이머 스텝 리스트가 null이면 400 에러를 반환한다")
        void shouldFail_whenStepsListIsNull() throws Exception {
            // given
            UUID memberId = UUID.randomUUID();
            CustomTimerCreateRequest request = new CustomTimerCreateRequest("유효한 이름", null, null, null); // Invalid: null steps

            // when & then
            mockMvc.perform(post("/api/v1/custom-timers")
                            .contentType(MediaType.APPLICATION_JSON)
                            .headers(getCommonApiHeaders(memberId))
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());

            then(customTimerCommandService).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("타이머 스텝의 이름이 비어있으면(@Valid) 400 에러를 반환한다")
        void shouldFail_whenNestedStepNameIsBlank() throws Exception {
            // given
            UUID memberId = UUID.randomUUID();
            List<CustomTimerStepCreateRequest> steps = List.of(
                    new CustomTimerStepCreateRequest("", (byte) 1, 30) // Invalid: blank step name
            );
            CustomTimerCreateRequest request = new CustomTimerCreateRequest("유효한 이름", steps, null, null);

            // when & then
            mockMvc.perform(post("/api/v1/custom-timers")
                            .contentType(MediaType.APPLICATION_JSON)
                            .headers(getCommonApiHeaders(memberId))
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());

            then(customTimerCommandService).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("타이머 스텝의 시간이 1보다 작으면(@Min) 400 에러를 반환한다")
        void shouldFail_whenNestedStepTimeIsInvalid() throws Exception {
            // given
            UUID memberId = UUID.randomUUID();
            List<CustomTimerStepCreateRequest> steps = List.of(
                    new CustomTimerStepCreateRequest("유효한 스텝 이름", (byte) 1, 0) // Invalid: time is 0
            );
            CustomTimerCreateRequest request = new CustomTimerCreateRequest("유효한 이름", steps, null, null);

            // when & then
            mockMvc.perform(post("/api/v1/custom-timers")
                            .contentType(MediaType.APPLICATION_JSON)
                            .headers(getCommonApiHeaders(memberId))
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());

            then(customTimerCommandService).shouldHaveNoInteractions();
        }
    }

    @Nested
    @DisplayName("커스텀 타이머 삭제 API")
    class DeleteCustomTimer {

        @Test
        @DisplayName("정상적으로 커스텀 타이머를 삭제한다")
        void deleteCustomTimer_Success() throws Exception {
            // given
            UUID memberId = UUID.randomUUID();
            Long timerId = 1L;

            given(customTimerCommandService.deleteCustomTimer(timerId, null)).willReturn(null);

            // when & then
            mockMvc.perform(delete("/api/v1/custom-timers/{customTimerId}", timerId)
                            .headers(getCommonApiHeaders(memberId)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.succeed").value(true))
                    .andExpect(jsonPath("$.code").value("DELETED"))
                    .andExpect(jsonPath("$.message").value("리소스가 성공적으로 삭제되었습니다."))
                    .andExpect(jsonPath("$.data").value(org.hamcrest.Matchers.nullValue()))
                    .andDo(print())
                    .andDo(document.document(
                            requestHeaders(
                                    RestDocsUtils.HEADER_ACCESS_TOKEN
                            ),
                            pathParameters(
                                    parameterWithName("customTimerId").attributes(getTypeFormat(JsonFieldType.NUMBER))
                                            .description("삭제할 커스텀 타이머의 ID를 의미합니다.")
                            ),
                            responseFields(
                                    RestDocsUtils.commonResponseFieldsOnly()
                            )
                    ));

            then(customTimerCommandService).should().deleteCustomTimer(timerId, null);
        }

        @Test
        @DisplayName("존재하지 않는 타이머 삭제 시 404 에러를 반환한다")
        void deleteCustomTimer_NotFound() throws Exception {
            // given
            UUID memberId = UUID.randomUUID();
            Long timerId = 999L;

            // 서비스 계층에서 예외 발생하도록 설정
            doThrow(new CustomException(ErrorCode.CUSTOM_TIMER_NOT_FOUND))
                    .when(customTimerCommandService).deleteCustomTimer(timerId, null);

            // when & then
            mockMvc.perform(delete("/api/v1/custom-timers/{customTimerId}", timerId)
                            .headers(getCommonApiHeaders(memberId)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.succeed").value(false))
                    .andExpect(jsonPath("$.code").value(ErrorCode.CUSTOM_TIMER_NOT_FOUND.name()));

            then(customTimerCommandService).should().deleteCustomTimer(timerId, null);
        }
    }

    @Nested
    @DisplayName("커스텀 타이머 전체 수정 API")
    class UpdateCustomTimer {

        @Test
        @DisplayName("정상적으로 커스텀 타이머를 수정한다 (이름 변경 + 스텝 교체)")
        void updateCustomTimer_Success() throws Exception {
            // given
            UUID memberId = UUID.randomUUID();
            Long timerId = 1L;

            // 요청 본문 DTO 생성
            List<CustomTimerStepCreateRequest> newSteps = List.of(
                    new CustomTimerStepCreateRequest("새로운 스텝 1", (byte) 0, 120),
                    new CustomTimerStepCreateRequest("새로운 휴식", (byte) 1, 30)
            );
            CustomTimerUpdateRequest request = new CustomTimerUpdateRequest("새로운 타이머 이름", newSteps, null, null);

            given(customTimerCommandService.updateCustomTimer(eq(timerId), any(CustomTimerUpdateRequest.class))).willReturn(null);

            // when & then
            mockMvc.perform(put("/api/v1/custom-timers/{customTimerId}", timerId) // PUT 요청
                            .contentType(MediaType.APPLICATION_JSON)
                            .headers(getCommonApiHeaders(memberId))
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.succeed").value(true))
                    .andExpect(jsonPath("$.code").value("UPDATED"))
                    .andExpect(jsonPath("$.message").value("리소스가 성공적으로 수정되었습니다."))
                    .andExpect(jsonPath("$.data").value(org.hamcrest.Matchers.nullValue()))
                    .andDo(print())
                    .andDo(document.document(
                            requestHeaders(
                                    RestDocsUtils.HEADER_ACCESS_TOKEN
                            ),
                            pathParameters(
                                    parameterWithName("customTimerId").attributes(getTypeFormat(JsonFieldType.NUMBER))
                                            .description("수정할 커스텀 타이머의 ID입니다.")
                            ),
                            requestFields(
                                    fieldWithPath("customTimerName").description("생성할 커스텀 타이머의 이름입니다. 커스텀 타이머 이름은 최소 1글자, 최대 100글자 이내여야 합니다."),
                                    fieldWithPath("customTimerSteps").description("커스텀 타이머에 포함될 스텝 목록입니다."),
                                    fieldWithPath("customTimerSteps[].customTimerStepName").description("스텝의 이름입니다. 커스텀 타이머 스텝 이름은 최소 1글자, 최대 50글자 이내여야 합니다."),
                                    fieldWithPath("customTimerSteps[].customTimerStepOrder").description("스텝의 순서입니다. 커스텀 타이머의 순서는 0~49 이내의 연속적인 정수여야 합니다."),
                                    fieldWithPath("customTimerSteps[].customTimerStepTime").description("스텝의 시간(초)입니다. 커스텀 타이머의 시간은 1 ~ 3599 이내의 값을 입력해야 합니다."),
                                    fieldWithPath("editId").type(JsonFieldType.STRING).optional().description("선택 UUID. editedAt과 함께 전송합니다."),
                                    fieldWithPath("editedAt").type(JsonFieldType.NUMBER).optional().description("선택 UTC epoch 밀리초. editId와 함께 전송합니다.")
                            ),
                            responseFields(
                                    RestDocsUtils.commonResponseFieldsOnly()
                            )
                    ));

            // 서비스 메소드가 정확한 인자와 함께 1번 호출되었는지 검증
            then(customTimerCommandService).should()
                    .updateCustomTimer(eq(timerId), any(CustomTimerUpdateRequest.class));
        }

        @Test
        @DisplayName("타이머 이름이 비어있으면(@NotBlank) 400 에러를 반환한다")
        void updateCustomTimer_ValidationFail_BlankName() throws Exception {
            // given
            UUID memberId = UUID.randomUUID();
            Long timerId = 1L;
            List<CustomTimerStepCreateRequest> steps = List.of(new CustomTimerStepCreateRequest("유효한 스텝", (byte) 0, 30));
            // 유효성 검사에 실패할 요청 DTO (이름이 비어있음)
            CustomTimerUpdateRequest request = new CustomTimerUpdateRequest("", steps, null, null);

            // when & then
            mockMvc.perform(put("/api/v1/custom-timers/{customTimerId}", timerId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .headers(getCommonApiHeaders(memberId))
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());

            // 유효성 검사 단계에서 실패했으므로 서비스 계층은 호출되지 않아야 함
            then(customTimerCommandService).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("존재하지 않는 타이머 수정 시 404 에러를 반환한다")
        void updateCustomTimer_NotFound() throws Exception {
            // given
            UUID memberId = UUID.randomUUID();
            Long nonExistentTimerId = 999L;
            CustomTimerUpdateRequest request = new CustomTimerUpdateRequest("유효한 이름", List.of(new CustomTimerStepCreateRequest("유효한 스텝", (byte) 0, 30)), null, null);

            // 서비스 계층에서 예외가 발생하도록 설정
            doThrow(new CustomException(ErrorCode.CUSTOM_TIMER_NOT_FOUND))
                    .when(customTimerCommandService)
                    .updateCustomTimer(eq(nonExistentTimerId), any(CustomTimerUpdateRequest.class));

            // when & then
            mockMvc.perform(put("/api/v1/custom-timers/{customTimerId}", nonExistentTimerId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .headers(getCommonApiHeaders(memberId))
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.succeed").value(false))
                    .andExpect(jsonPath("$.code").value(ErrorCode.CUSTOM_TIMER_NOT_FOUND.name()));

            // 예외는 발생했지만 서비스 메소드 자체는 호출되었음을 검증
            then(customTimerCommandService).should()
                    .updateCustomTimer(eq(nonExistentTimerId), any(CustomTimerUpdateRequest.class));
        }

        @Test
        @DisplayName("스텝 개수가 정책상 최대치를 초과하면 409 Conflict 에러를 반환한다")
        void updateCustomTimer_Conflict_MaxStepsExceeded() throws Exception {
            // given
            UUID memberId = UUID.randomUUID();
            Long timerId = 1L;

            // 정책 위반을 유발할 요청 DTO (스텝이 너무 많음)
            List<CustomTimerStepCreateRequest> tooManySteps = List.of(
                    new CustomTimerStepCreateRequest("스텝 1", (byte) 0, 30),
                    new CustomTimerStepCreateRequest("스텝 2", (byte) 1, 30),
                    new CustomTimerStepCreateRequest("스텝 3", (byte) 2, 30)
            );
            CustomTimerUpdateRequest request = new CustomTimerUpdateRequest("스텝이 너무 많은 타이머", tooManySteps, null, null);

            // 서비스 계층에서 '정책 위반' 예외가 발생하도록 설정
            doThrow(new CustomException(ErrorCode.CUSTOM_TIMER_STEP_MAX_COUNT_VIOLATION))
                    .when(customTimerCommandService)
                    .updateCustomTimer(eq(timerId), any(CustomTimerUpdateRequest.class));

            // when & then
            mockMvc.perform(put("/api/v1/custom-timers/{customTimerId}", timerId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .headers(getCommonApiHeaders(memberId))
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict()) // 409 Conflict 상태 코드를 기대
                    .andExpect(jsonPath("$.succeed").value(false))
                    .andExpect(jsonPath("$.code").value(ErrorCode.CUSTOM_TIMER_STEP_MAX_COUNT_VIOLATION.name()));

            // 서비스 메소드 자체는 호출되었음을 검증
            then(customTimerCommandService).should()
                    .updateCustomTimer(eq(timerId), any(CustomTimerUpdateRequest.class));
        }
    }

    @Nested
    @DisplayName("커스텀 타이머 이름 수정 API")
    class UpdateCustomTimerName {

        @Test
        @DisplayName("정상적으로 커스텀 타이머의 이름을 수정한다")
        void updateCustomTimerName_Success() throws Exception {
            // given
            UUID memberId = UUID.randomUUID();
            Long timerId = 1L;
            CustomTimerNameUpdateRequest request = new CustomTimerNameUpdateRequest("새로운 타이머 이름.", null, null);

            given(customTimerCommandService.updateCustomTimerName(eq(timerId), any(CustomTimerNameUpdateRequest.class))).willReturn(null);

            // when & then
            mockMvc.perform(patch("/api/v1/custom-timers/{customTimerId}", timerId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .headers(getCommonApiHeaders(memberId))
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.succeed").value(true))
                    .andExpect(jsonPath("$.code").value("UPDATED"))
                    .andExpect(jsonPath("$.message").value("리소스가 성공적으로 수정되었습니다."))
                    .andExpect(jsonPath("$.data").value(org.hamcrest.Matchers.nullValue()))
                    .andDo(print())
                    .andDo(document.document(
                            requestHeaders(
                                    RestDocsUtils.HEADER_ACCESS_TOKEN
                            ),
                            pathParameters(
                                    parameterWithName("customTimerId").attributes(getTypeFormat(JsonFieldType.NUMBER))
                                            .description("수정할 커스텀 타이머의 ID를 의미합니다.")
                            ),
                            requestFields(
                                    fieldWithPath("customTimerName").description("새롭게 변경할 커스텀 타이머의 이름입니다."),
                                    fieldWithPath("editId").type(JsonFieldType.STRING).optional().description("선택 UUID. editedAt과 함께 전송합니다."),
                                    fieldWithPath("editedAt").type(JsonFieldType.NUMBER).optional().description("선택 UTC epoch 밀리초. editId와 함께 전송합니다.")
                            ),
                            responseFields(
                                    RestDocsUtils.commonResponseFieldsOnly()
                            )
                    ));

            // 서비스 메소드가 정확한 인자와 함께 1번 호출되었는지 검증
            then(customTimerCommandService).should().updateCustomTimerName(eq(timerId), any(CustomTimerNameUpdateRequest.class));
        }

        @Test
        @DisplayName("타이머 이름이 비어있으면(@NotBlank) 400 에러를 반환한다")
        void updateCustomTimerName_ValidationFail_BlankName() throws Exception {
            // given
            UUID memberId = UUID.randomUUID();
            Long timerId = 1L;
            CustomTimerNameUpdateRequest request = new CustomTimerNameUpdateRequest("", null, null);

            // when & then
            mockMvc.perform(patch("/api/v1/custom-timers/{customTimerId}", timerId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .headers(getCommonApiHeaders(memberId))
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest()); // 400 Bad Request 기대

            // 유효성 검사 단계에서 실패했으므로 서비스 계층은 호출되지 않아야 함
            then(customTimerCommandService).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("존재하지 않는 타이머의 이름 수정 시 404 에러를 반환한다")
        void updateCustomTimerName_NotFound() throws Exception {
            // given
            UUID memberId = UUID.randomUUID();
            Long nonExistentTimerId = 999L;
            CustomTimerNameUpdateRequest request = new CustomTimerNameUpdateRequest("수정될 리 없는 이름", null, null);

            // 서비스 계층에서 예외가 발생하도록 설정
            doThrow(new CustomException(ErrorCode.CUSTOM_TIMER_NOT_FOUND))
                    .when(customTimerCommandService)
                    .updateCustomTimerName(eq(nonExistentTimerId), any(CustomTimerNameUpdateRequest.class));

            // when & then
            mockMvc.perform(patch("/api/v1/custom-timers/{customTimerId}", nonExistentTimerId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .headers(getCommonApiHeaders(memberId))
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.succeed").value(false))
                    .andExpect(jsonPath("$.code").value(ErrorCode.CUSTOM_TIMER_NOT_FOUND.name()));

            then(customTimerCommandService).should().updateCustomTimerName(eq(nonExistentTimerId), any(CustomTimerNameUpdateRequest.class));
        }
    }
}