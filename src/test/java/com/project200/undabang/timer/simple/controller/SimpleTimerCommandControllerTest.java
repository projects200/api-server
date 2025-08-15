package com.project200.undabang.timer.simple.controller;

import com.project200.undabang.common.web.exception.CustomException;
import com.project200.undabang.common.web.exception.ErrorCode;
import com.project200.undabang.configuration.AbstractRestDocSupport;
import com.project200.undabang.configuration.RestDocsUtils;
import com.project200.undabang.timer.simple.dto.request.SimpleTimerCreateRequestDto;
import com.project200.undabang.timer.simple.dto.request.SimpleTimerUpdateRequestDto;
import com.project200.undabang.timer.simple.dto.response.SimpleTimerCreateResponseDto;
import com.project200.undabang.timer.simple.service.SimpleTimerCommandService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.BDDMockito;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.UUID;

import static com.project200.undabang.configuration.DocumentFormatGenerator.getTypeFormat;
import static com.project200.undabang.configuration.HeadersGenerator.getCommonApiHeaders;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.willDoNothing;
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

    @Nested
    @DisplayName("심플 타이머 생성 API")
    class CreateSimpleTimer {

        @Test
        @DisplayName("성공: 유효한 요청 시 심플 타이머를 성공적으로 생성하고 200 OK를 반환한다")
        void createSimpleTimer_Success() throws Exception {
            // given
            UUID memberId = UUID.randomUUID();
            SimpleTimerCreateRequestDto requestDto = SimpleTimerCreateRequestDto.builder().time(120).build();
            SimpleTimerCreateResponseDto responseDto = SimpleTimerCreateResponseDto.builder().simpleTimerId(1L).build();

            BDDMockito.given(simpleTimerCommandService.createSimpleTimer(any(SimpleTimerCreateRequestDto.class)))
                    .willReturn(responseDto);

            // when & then
            mockMvc.perform(post("/api/v1/simple-timers")
                            .contentType(MediaType.APPLICATION_JSON)
                            .accept(MediaType.APPLICATION_JSON)
                            .headers(getCommonApiHeaders(memberId))
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.succeed").value(true))
                    .andExpect(jsonPath("$.code").value("CREATED"))
                    .andExpect(jsonPath("$.message").value("리소스가 성공적으로 생성되었습니다."))
                    .andExpect(jsonPath("$.data.simpleTimerId").value(1L))
                    .andDo(print())
                    .andDo(document.document(
                            requestHeaders(
                                    RestDocsUtils.HEADER_ACCESS_TOKEN
                            ),
                            requestFields(
                                    fieldWithPath("time").type(JsonFieldType.NUMBER)
                                            .description("생성할 타이머의 시간(초)을 의미합니다. 1 ~ 3599 사이의 값을 입력해야 합니다.")
                            ),
                            responseFields(
                                    fieldWithPath("succeed").type(JsonFieldType.BOOLEAN).description("요청 성공 여부"),
                                    fieldWithPath("code").type(JsonFieldType.STRING).description("결과 코드"),
                                    fieldWithPath("message").type(JsonFieldType.STRING).description("결과 메시지"),
                                    fieldWithPath("data.simpleTimerId").type(JsonFieldType.NUMBER)
                                            .description("생성된 심플 타이머의 ID")
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

            // 실제로 삭제하지 않기위해 willDoNothing() 사용
            willDoNothing().given(simpleTimerCommandService).deleteSimpleTimer(simpleTimerId);

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

            BDDMockito.then(simpleTimerCommandService).should().deleteSimpleTimer(simpleTimerId);
        }


        @Test
        @DisplayName("실패: 존재하지 않는 타이머 ID로 요청하면 404 Not Found를 반환한다")
        void deleteSimpleTimer_Fail_NotFound() throws Exception {
            // given
            Long nonExistentTimerId = 999L;
            UUID memberId = UUID.randomUUID();

            willThrow(new CustomException(ErrorCode.SIMPLE_TIMER_NOT_EXIST))
                    .given(simpleTimerCommandService).deleteSimpleTimer(nonExistentTimerId);

            // when & then
            mockMvc.perform(delete("/api/v1/simple-timers/{simpleTimerId}", nonExistentTimerId)
                            .headers(getCommonApiHeaders(memberId)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value(ErrorCode.SIMPLE_TIMER_NOT_EXIST.getCode()))
                    .andExpect(jsonPath("$.message").value(ErrorCode.SIMPLE_TIMER_NOT_EXIST.getMessage()));

            BDDMockito.then(simpleTimerCommandService).should().deleteSimpleTimer(nonExistentTimerId);
        }

        @Test
        @DisplayName("실패: 존재하지 않는 회원 ID로 요청하면 404 Not Found를 반환한다")
        void deleteSimpleTimer_Fail_MemberNotFound() throws Exception {
            // given
            Long simpleTimerId = 1L;
            UUID nonExistentMemberId = UUID.randomUUID();

            willThrow(new CustomException(ErrorCode.MEMBER_NOT_FOUND))
                    .given(simpleTimerCommandService).deleteSimpleTimer(simpleTimerId);

            // when & then
            mockMvc.perform(delete("/api/v1/simple-timers/{simpleTimerId}", simpleTimerId)
                            .headers(getCommonApiHeaders(nonExistentMemberId)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value(ErrorCode.MEMBER_NOT_FOUND.getCode()))
                    .andExpect(jsonPath("$.message").value(ErrorCode.MEMBER_NOT_FOUND.getMessage()));

            BDDMockito.then(simpleTimerCommandService).should().deleteSimpleTimer(simpleTimerId);
        }

        @Test
        @DisplayName("실패: 남은 타이머가 1개일 때 삭제를 시도하면 409 Conflict를 반환한다")
        void deleteSimpleTimer_Fail_LastTimer() throws Exception {
            // given
            Long simpleTimerId = 1L;
            UUID memberId = UUID.randomUUID();

            // 서비스 레이어에서 CANNOT_DELETE_LAST_SIMPLE_TIMER 예외를 발생시킨다고 가정
            willThrow(new CustomException(ErrorCode.SIMPLE_TIMER_MIN_COUNT_VIOLATION))
                    .given(simpleTimerCommandService).deleteSimpleTimer(simpleTimerId);

            // when & then
            mockMvc.perform(delete("/api/v1/simple-timers/{simpleTimerId}", simpleTimerId)
                            .headers(getCommonApiHeaders(memberId)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value(ErrorCode.SIMPLE_TIMER_MIN_COUNT_VIOLATION.getCode()))
                    .andExpect(jsonPath("$.message").value(ErrorCode.SIMPLE_TIMER_MIN_COUNT_VIOLATION.getMessage()));

            BDDMockito.then(simpleTimerCommandService).should().deleteSimpleTimer(simpleTimerId);
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
            SimpleTimerUpdateRequestDto requestDto = new SimpleTimerUpdateRequestDto(180);

            BDDMockito.willDoNothing().given(simpleTimerCommandService).updateSimpleTimer(eq(simpleTimerId), any(SimpleTimerUpdateRequestDto.class));

            // when & then
            mockMvc.perform(patch("/api/v1/simple-timers/{simpleTimerId}", simpleTimerId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .headers(getCommonApiHeaders(memberId))
                            .content(objectMapper.writeValueAsString(requestDto)))
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
                                            .description("수정할 시간을 의미합니다. 1 ~ 3599 사이의 값을 입력하셔야 합니다.")
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
            SimpleTimerUpdateRequestDto requestDto = new SimpleTimerUpdateRequestDto(0);

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
            SimpleTimerUpdateRequestDto requestDto = new SimpleTimerUpdateRequestDto(180);

            BDDMockito.willThrow(new CustomException(ErrorCode.SIMPLE_TIMER_NOT_EXIST))
                    .given(simpleTimerCommandService).updateSimpleTimer(eq(nonExistentTimerId), any(SimpleTimerUpdateRequestDto.class));

            // when & then
            mockMvc.perform(patch("/api/v1/simple-timers/{simpleTimerId}", nonExistentTimerId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .headers(getCommonApiHeaders(memberId))
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value(ErrorCode.SIMPLE_TIMER_NOT_EXIST.getCode()))
                    .andExpect(jsonPath("$.message").value(ErrorCode.SIMPLE_TIMER_NOT_EXIST.getMessage()));

            BDDMockito.then(simpleTimerCommandService).should().updateSimpleTimer(eq(nonExistentTimerId), any(SimpleTimerUpdateRequestDto.class));
        }
    }
}