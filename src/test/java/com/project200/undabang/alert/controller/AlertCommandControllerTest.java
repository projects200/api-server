package com.project200.undabang.alert.controller;


import com.project200.undabang.alert.dto.response.UpdateExerciseEncouragementResponse;
import com.project200.undabang.alert.service.AlertCommandService;
import com.project200.undabang.configuration.AbstractRestDocSupport;
import com.project200.undabang.configuration.RestDocsUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.BDDMockito;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.UUID;

import static com.project200.undabang.configuration.HeadersGenerator.getCommonApiHeaders;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.patch;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AlertCommandController.class)
@DisplayName("AlertController 테스트")
class AlertCommandControllerTest extends AbstractRestDocSupport {

    private final UUID memberId = UUID.randomUUID();

    @MockitoBean
    private AlertCommandService alertCommandService;

    @Nested
    @DisplayName("알림 활성화 API 테스트")
    class ActivateAlert {

        private final String ACTIVATE_URL = "/api/v1/alerts/exercise-encouragement/activate";

        @Test
        @DisplayName("알림 활성화 성공")
        void activateAlert_Success() throws Exception {
            // given
            long updatedCount = 5L;
            UpdateExerciseEncouragementResponse resp = UpdateExerciseEncouragementResponse.of(updatedCount);
            BDDMockito.given(alertCommandService.activateAllExerciseEncouragementToken()).willReturn(resp);

            // when & then
            mockMvc.perform(patch(ACTIVATE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .headers(getCommonApiHeaders(memberId)))
                    .andExpectAll(
                            status().isOk(),
                            jsonPath("$.succeed").value(true),
                            jsonPath("$.code").value("SUCCESS"),
                            jsonPath("$.message").value("요청이 성공적으로 처리되었습니다."),
                            jsonPath("$.data.updatedTokenCount").value(updatedCount)
                    )
                    .andDo(print())
                    .andDo(document.document(
                            requestHeaders(
                                    RestDocsUtils.HEADER_ACCESS_TOKEN
                            ),
                            responseFields(
                                    RestDocsUtils.commonResponseFields(
                                            fieldWithPath("data.updatedTokenCount").type(JsonFieldType.NUMBER).description("활성화 처리된 FCM 토큰의 수")
                                    )
                            )
                    ));

            BDDMockito.then(alertCommandService).should().activateAllExerciseEncouragementToken();
        }
    }

    @Nested
    @DisplayName("알림 비활성화 API 테스트")
    class DeactivateAlert {

        private final String DEACTIVATE_URL = "/api/v1/alerts/exercise-encouragement/deactivate";

        @Test
        @DisplayName("알림 비활성화 성공")
        void deactivateAlert_Success() throws Exception {
            // given
            long updatedCount = 3L;
            UpdateExerciseEncouragementResponse resp = UpdateExerciseEncouragementResponse.of(updatedCount);
            BDDMockito.given(alertCommandService.deactivateAllExerciseEncouragementToken()).willReturn(resp);

            // when & then
            mockMvc.perform(patch(DEACTIVATE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .headers(getCommonApiHeaders(memberId)))
                    .andExpectAll(
                            status().isOk(),
                            jsonPath("$.succeed").value(true),
                            jsonPath("$.code").value("SUCCESS"),
                            jsonPath("$.message").value("요청이 성공적으로 처리되었습니다."),
                            jsonPath("$.data.updatedTokenCount").value(updatedCount)
                    )
                    .andDo(print())
                    .andDo(document.document(
                            requestHeaders(
                                    RestDocsUtils.HEADER_ACCESS_TOKEN
                            ),
                            responseFields(
                                    RestDocsUtils.commonResponseFields(
                                            fieldWithPath("data.updatedTokenCount").type(JsonFieldType.NUMBER).description("비활성화 처리된 FCM 토큰의 수")
                                    )
                            )
                    ));

            BDDMockito.then(alertCommandService).should().deactivateAllExerciseEncouragementToken();
        }
    }
}