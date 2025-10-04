package com.project200.undabang.alert.controller;

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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.util.UUID;

import static com.project200.undabang.configuration.DocumentFormatGenerator.getTypeFormat;
import static com.project200.undabang.configuration.HeadersGenerator.getCommonApiHeaders;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;

@WebMvcTest(AlertCommandController.class)
@DisplayName("AlertController 테스트")
class AlertCommandControllerTest extends AbstractRestDocSupport {

    private final String fcmToken = "test-fcm-token";
    private final UUID memberId = UUID.randomUUID();
    @MockitoBean
    private AlertCommandService alertCommandService;

    @Nested
    @DisplayName("알림 활성화 API 테스트")
    class ActivateAlert {

        private final String ACTIVATE_URL = "/api/v1/alerts/activate";

        @Test
        @DisplayName("알림 활성화 성공")
        void activateAlert_Success() throws Exception {
            // given
            BDDMockito.doNothing().when(alertCommandService).activateAlert(fcmToken);

            // when & then
            mockMvc.perform(MockMvcRequestBuilders.patch(ACTIVATE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("X-Fcm-Token", fcmToken)
                            .headers(getCommonApiHeaders(memberId)))
                    .andExpect(MockMvcResultMatchers.status().isOk())
                    .andExpect(MockMvcResultMatchers.jsonPath("$.succeed").value(true))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("ACTIVATE_ALERTS"))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("알림 기능이 활성화 되었습니다."))
                    .andDo(document.document(
                            requestHeaders(
                                    RestDocsUtils.HEADER_ACCESS_TOKEN,
                                    headerWithName("X-Fcm-Token").attributes(getTypeFormat(JsonFieldType.STRING))
                                            .description("활성화할 FCM 토큰")
                            ),
                            responseFields(
                                    RestDocsUtils.commonResponseFieldsOnly()
                            )
                    ));

            BDDMockito.then(alertCommandService).should().activateAlert(fcmToken);
        }

        @Test
        @DisplayName("알림 활성화 실패 - FCM 토큰 누락")
        void activateAlert_Fail_MissingFcmToken() throws Exception {
            // given & when & then
            mockMvc.perform(MockMvcRequestBuilders.patch(ACTIVATE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .headers(getCommonApiHeaders(memberId)))
                    .andExpect(MockMvcResultMatchers.status().isBadRequest());

            BDDMockito.then(alertCommandService).shouldHaveNoInteractions();
        }
    }

    @Nested
    @DisplayName("알림 비활성화 API 테스트")
    class DeactivateAlert {

        private final String DEACTIVATE_URL = "/api/v1/alerts/deactivate";

        @Test
        @DisplayName("알림 비활성화 성공")
        void deactivateAlert_Success() throws Exception {
            // given
            BDDMockito.doNothing().when(alertCommandService).deactivateAlert(fcmToken);

            // when & then
            mockMvc.perform(MockMvcRequestBuilders.patch(DEACTIVATE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("X-Fcm-Token", fcmToken)
                            .headers(getCommonApiHeaders(memberId)))
                    .andExpect(MockMvcResultMatchers.status().isOk())
                    .andExpect(MockMvcResultMatchers.jsonPath("$.succeed").value(true))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("DEACTIVATE_ALERTS"))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("알림 기능이 비활성화 되었습니다."))
                    .andDo(document.document(
                            requestHeaders(
                                    RestDocsUtils.HEADER_ACCESS_TOKEN,
                                    headerWithName("X-Fcm-Token").attributes(getTypeFormat(JsonFieldType.STRING))
                                            .description("비활성화할 FCM 토큰")
                            ),
                            responseFields(
                                    RestDocsUtils.commonResponseFieldsOnly()
                            )
                    ));

            BDDMockito.then(alertCommandService).should().deactivateAlert(fcmToken);
        }

        @Test
        @DisplayName("알림 비활성화 실패 - FCM 토큰 누락")
        void deactivateAlert_Fail_MissingFcmToken() throws Exception {
            // given & when & then
            mockMvc.perform(MockMvcRequestBuilders.patch(DEACTIVATE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .headers(getCommonApiHeaders(memberId)))
                    .andExpect(MockMvcResultMatchers.status().isBadRequest());

            BDDMockito.then(alertCommandService).shouldHaveNoInteractions();
        }
    }
}