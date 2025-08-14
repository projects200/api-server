package com.project200.undabang.timer.simple.controller;

import com.project200.undabang.common.web.exception.CustomException;
import com.project200.undabang.common.web.exception.ErrorCode;
import com.project200.undabang.configuration.AbstractRestDocSupport;
import com.project200.undabang.configuration.RestDocsUtils;
import com.project200.undabang.timer.simple.dto.request.SimpleTimerUpdateRequestDto;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SimpleTimerCommandController.class)
class SimpleTimerCommandControllerTest extends AbstractRestDocSupport {

    @MockitoBean
    private SimpleTimerCommandService simpleTimerCommandService;

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