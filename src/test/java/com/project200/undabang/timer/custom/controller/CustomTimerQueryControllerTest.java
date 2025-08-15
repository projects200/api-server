package com.project200.undabang.timer.custom.controller;

import com.project200.undabang.common.web.exception.CustomException;
import com.project200.undabang.common.web.exception.ErrorCode;
import com.project200.undabang.configuration.AbstractRestDocSupport;
import com.project200.undabang.timer.custom.dto.response.CustomTimerRecord;
import com.project200.undabang.timer.custom.dto.response.GetCustomTimerListResponse;
import com.project200.undabang.timer.custom.service.CustomTimerQueryService;
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
import static org.springframework.restdocs.payload.JsonFieldType.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CustomTimerQueryController.class)
class CustomTimerQueryControllerTest extends AbstractRestDocSupport {

    @MockitoBean
    private CustomTimerQueryService customTimerQueryService;

    @Nested
    @DisplayName("GET /api/v1/custom-timers API는")
    class GetCustomTimerList {

        @Test
        @DisplayName("회원의 커스텀 타이머 목록을 성공적으로 조회한다")
        void getCustomTimerList_Success() throws Exception {
            // given
            UUID memberId = UUID.randomUUID();
            List<CustomTimerRecord> recordList = createCustomTimerRecordList();
            GetCustomTimerListResponse expectedResponse = GetCustomTimerListResponse.builder()
                    .customTimerCount(recordList.size())
                    .customTimers(recordList)
                    .build();

            BDDMockito.given(customTimerQueryService.getCustomTimerList()).willReturn(expectedResponse);

            // when & then
            mockMvc.perform(get("/api/v1/custom-timers")
                            .contentType(MediaType.APPLICATION_JSON)
                            .accept(MediaType.APPLICATION_JSON)
                            .headers(getCommonApiHeaders(memberId)))
                    .andExpectAll(
                            status().isOk(),
                            jsonPath("$.succeed").value(true),
                            jsonPath("$.code").value("SUCCESS"),
                            jsonPath("$.data.customTimerCount").value(expectedResponse.getCustomTimerCount()),
                            jsonPath("$.data.customTimers").isArray(),
                            jsonPath("$.data.customTimers[0].customTimerId").value(recordList.get(0).customTimerId()),
                            jsonPath("$.data.customTimers[0].customTimerName").value(recordList.get(0).customTimerName())
                    )
                    .andDo(document.document(
                            requestHeaders(HEADER_ACCESS_TOKEN),
                            responseFields(commonResponseFields(
                                    fieldWithPath("data.customTimerCount").type(NUMBER).description("커스텀 타이머 리스트의 크기를 담고있는 데이터 입니다."),
                                    fieldWithPath("data.customTimers").type(ARRAY).description("커스텀 타이머 리스트 입니다. 내용에는 커스텀 타이머 식별자, 커스텀 타이머 이름이 포함됩니다."),
                                    fieldWithPath("data.customTimers[].customTimerId").type(NUMBER).description("커스텀 타이머의 식별자 정보입니다."),
                                    fieldWithPath("data.customTimers[].customTimerName").type(STRING).description("커스텀 타이머의 이름 입니다.")
                            ))
                    ));

            BDDMockito.then(customTimerQueryService).should(BDDMockito.times(1)).getCustomTimerList();
        }

        @Test
        @DisplayName("존재하지 않는 회원으로 조회 시 실패한다")
        void getCustomTimerList_Fail_MemberNotFound() throws Exception {
            // given
            UUID memberId = UUID.randomUUID();
            BDDMockito.given(customTimerQueryService.getCustomTimerList())
                    .willThrow(new CustomException(ErrorCode.MEMBER_NOT_FOUND));

            // when & then
            mockMvc.perform(get("/api/v1/custom-timers")
                            .contentType(MediaType.APPLICATION_JSON)
                            .accept(MediaType.APPLICATION_JSON)
                            .headers(getCommonApiHeaders(memberId)))
                    .andExpectAll(
                            status().isNotFound(),
                            jsonPath("$.succeed").value(false),
                            jsonPath("$.code").value(ErrorCode.MEMBER_NOT_FOUND.getCode()),
                            jsonPath("$.message").value(ErrorCode.MEMBER_NOT_FOUND.getMessage())
                    );

            BDDMockito.then(customTimerQueryService).should(BDDMockito.times(1)).getCustomTimerList();
        }

        private List<CustomTimerRecord> createCustomTimerRecordList() {
            return List.of(
                    new CustomTimerRecord(1L, "공부"),
                    new CustomTimerRecord(2L, "운동"),
                    new CustomTimerRecord(3L, "독서")
            );
        }
    }
}