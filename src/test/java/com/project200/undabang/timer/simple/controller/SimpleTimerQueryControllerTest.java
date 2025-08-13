package com.project200.undabang.timer.simple.controller;

import com.project200.undabang.common.web.exception.ErrorCode;
import com.project200.undabang.configuration.AbstractRestDocSupport;
import com.project200.undabang.timer.simple.dto.GetSimpleTimerResponseDto;
import com.project200.undabang.timer.simple.dto.SimpleTimerRecord;
import com.project200.undabang.timer.simple.service.SimpleTimerQueryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.BDDMockito;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.UUID;

import static com.project200.undabang.configuration.HeadersGenerator.getCommonApiHeaders;
import static com.project200.undabang.configuration.RestDocsUtils.HEADER_ACCESS_TOKEN;
import static com.project200.undabang.configuration.RestDocsUtils.commonResponseFields;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.payload.JsonFieldType.ARRAY;
import static org.springframework.restdocs.payload.JsonFieldType.NUMBER;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SimpleTimerQueryController.class)
class SimpleTimerQueryControllerTest extends AbstractRestDocSupport {

    @MockitoBean
    private SimpleTimerQueryService simpleTimerQueryService;

    @Nested
    class GetSimpleTimer {
        @Test
        @DisplayName("회원의 심플 타이머 목록을 성공적으로 조회한다")
        public void getSimpleTimers() throws Exception {
            // given
            UUID memberId = UUID.randomUUID();
            List<SimpleTimerRecord> recordList =createSimpleTimerRecordList();
            GetSimpleTimerResponseDto expectedResponse = GetSimpleTimerResponseDto.builder()
                    .simpleTimerCount(recordList.size())
                    .simpleTimers(recordList)
                    .build();

            BDDMockito.given(simpleTimerQueryService.getSimpleTimers()).willReturn(expectedResponse);

            // when & then
            mockMvc.perform(get("/api/v1/simple-timers")
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .headers(getCommonApiHeaders(memberId)))
                    .andExpectAll(
                            status().isOk(),
                            jsonPath("$.succeed").value(true),
                            jsonPath("$.code").value("SUCCESS"),
                            jsonPath("$.message").value("요청이 성공적으로 처리되었습니다."),
                            jsonPath("$.data.simpleTimerCount").value(expectedResponse.getSimpleTimerCount()),
                            jsonPath("$.data.simpleTimers").isArray(),
                            jsonPath("$.data.simpleTimers[0].simpleTimerId").value(recordList.get(0).simpleTimerId()),
                            jsonPath("$.data.simpleTimers[0].time").value(recordList.get(0).time())
                    )
                    .andDo(document.document(
                            requestHeaders(HEADER_ACCESS_TOKEN),
                            responseFields(commonResponseFields(
                                    fieldWithPath("data.simpleTimerCount").type(NUMBER)
                                            .description("심플 타이머 리스트의 크기를 담고있는 데이터 입니다."),
                                    fieldWithPath("data.simpleTimers").type(ARRAY)
                                            .description("심플 타이머 리스트 입니다. 내용에는 심플 타이머 식별자, 타이머 시간이 포함됩니다."),
                                    fieldWithPath("data.simpleTimers[].simpleTimerId").type(NUMBER)
                                            .description("심플 타이머의 식별자 정보 입니다."),
                                    fieldWithPath("data.simpleTimers[].time").type(NUMBER)
                                            .description("심플 타이머의 시간 정보 입니다. 단위는 초 입니다.")
                            ))
                    ));

            // then
            BDDMockito.then(simpleTimerQueryService).should(BDDMockito.times(1)).getSimpleTimers();
        }

        @Test
        @DisplayName("회원의 심플 타이머 조회에 실패했다")
        public void getSimpleTimers_Fail() throws Exception {
            // given
            UUID memberId = UUID.randomUUID();
            BDDMockito.given(simpleTimerQueryService.getSimpleTimers())
                    .willThrow(new com.project200.undabang.common.web.exception.CustomException(com.project200.undabang.common.web.exception.ErrorCode.MEMBER_NOT_FOUND));

            // when & then
            mockMvc.perform(get("/api/v1/simple-timers")
                            .contentType(MediaType.APPLICATION_JSON)
                            .accept(MediaType.APPLICATION_JSON)
                            .headers(getCommonApiHeaders(memberId)))
                    .andExpectAll(
                            status().isNotFound(),
                            jsonPath("$.succeed").value(false),
                            jsonPath("$.code").value(ErrorCode.MEMBER_NOT_FOUND.getCode()),
                            jsonPath("$.message").value(ErrorCode.MEMBER_NOT_FOUND.getMessage())
                    );

            // then
            BDDMockito.then(simpleTimerQueryService).should(BDDMockito.times(1)).getSimpleTimers();
        }

        private List<SimpleTimerRecord> createSimpleTimerRecordList() {
            return List.of(
                    new SimpleTimerRecord(1L, 30),
                    new SimpleTimerRecord(2L, 40),
                    new SimpleTimerRecord(3L, 50),
                    new SimpleTimerRecord(4L, 60),
                    new SimpleTimerRecord(5L, 75),
                    new SimpleTimerRecord(6L, 90)
            );
        }
    }
}