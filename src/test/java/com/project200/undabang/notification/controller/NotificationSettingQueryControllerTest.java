package com.project200.undabang.notification.controller;

import com.project200.undabang.common.web.exception.CustomException;
import com.project200.undabang.common.web.exception.ErrorCode;
import com.project200.undabang.configuration.AbstractRestDocSupport;
import com.project200.undabang.notification.dto.response.GetAllDeviceNotificationSettingsResponse;
import com.project200.undabang.notification.fcm.entity.NotificationType;
import com.project200.undabang.notification.service.NotificationSettingQueryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.BDDMockito;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.UUID;

import static com.project200.undabang.configuration.DocumentFormatGenerator.getTypeFormat;
import static com.project200.undabang.configuration.HeadersGenerator.getCommonApiHeaders;
import static com.project200.undabang.configuration.RestDocsUtils.HEADER_ACCESS_TOKEN;
import static com.project200.undabang.configuration.RestDocsUtils.commonResponseFieldsForList;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.payload.JsonFieldType.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(NotificationSettingQueryController.class)
class NotificationSettingQueryControllerTest extends AbstractRestDocSupport {

    @MockitoBean
    private NotificationSettingQueryService notificationSettingQueryService;

    @Nested
    @DisplayName("GET /api/v1/notification-settings/device API는")
    class GetAllDeviceNotificationSettings {

        @Test
        @DisplayName("정상적으로 디바이스 알림 설정 목록을 반환한다")
        void getAllDeviceNotificationSettings_Success() throws Exception {
            // given
            UUID memberId = UUID.randomUUID();
            String fcmToken = "test-fcm-token";

            GetAllDeviceNotificationSettingsResponse resp1 =
                    new GetAllDeviceNotificationSettingsResponse(NotificationType.CHAT_MESSAGE, true);
            GetAllDeviceNotificationSettingsResponse resp2 =
                    new GetAllDeviceNotificationSettingsResponse(NotificationType.WORKOUT_REMINDER, false);

            BDDMockito.given(notificationSettingQueryService.getAllDeviceNotificationSettings(fcmToken))
                    .willReturn(List.of(resp1, resp2));

            // when & then
            mockMvc.perform(get("/api/v1/notification-settings/device")
                            .contentType(MediaType.APPLICATION_JSON)
                            .accept(MediaType.APPLICATION_JSON)
                            .headers(getCommonApiHeaders(memberId))
                            .header("X-Fcm-Token", fcmToken))
                    .andExpectAll(
                            status().isOk(),
                            jsonPath("$.succeed").value(true),
                            jsonPath("$.code").value("SUCCESS"),
                            jsonPath("$.data").isArray(),
                            jsonPath("$.data[0].type").value("CHAT_MESSAGE"),
                            jsonPath("$.data[0].enabled").value(true),
                            jsonPath("$.data[1].type").value("WORKOUT_REMINDER"),
                            jsonPath("$.data[1].enabled").value(false)
                    )
                    .andDo(document.document(
                            requestHeaders(
                                    HEADER_ACCESS_TOKEN,
                                    headerWithName("X-Fcm-Token").attributes(getTypeFormat(JsonFieldType.STRING)).description("FCM 토큰 값입니다.")
                            ),
                            responseFields(commonResponseFieldsForList(
                                    fieldWithPath("data").type(ARRAY).description("토큰별 알림 설정 리스트입니다. 같은 기기여도 여러 토큰을 가질 수 있습니다(앱 <-> 웹)"),
                                    fieldWithPath("data[].type").type(STRING).description("알림 타입입니다. CHAT_MESSAGE, WORKOUT_REMINDER 등 어떤 타입의 알림인지 알려줍니다."),
                                    fieldWithPath("data[].enabled").type(BOOLEAN).description("해당 알림 타입의 활성화 여부입니다.")
                            ))
                    ));

            BDDMockito.then(notificationSettingQueryService).should(BDDMockito.times(1)).getAllDeviceNotificationSettings(fcmToken);
        }

        @Test
        @DisplayName("존재하지 않는 회원 등으로 조회 시 실패한다")
        void getAllDeviceNotificationSettings_Fail_MemberNotFound() throws Exception {
            // given
            UUID memberId = UUID.randomUUID();
            String fcmToken = "invalid-token";
            BDDMockito.given(notificationSettingQueryService.getAllDeviceNotificationSettings(fcmToken))
                    .willThrow(new CustomException(ErrorCode.MEMBER_NOT_FOUND));

            // when & then
            mockMvc.perform(get("/api/v1/notification-settings/device")
                            .contentType(MediaType.APPLICATION_JSON)
                            .accept(MediaType.APPLICATION_JSON)
                            .headers(getCommonApiHeaders(memberId))
                            .header("X-Fcm-Token", fcmToken))
                    .andExpectAll(
                            status().isNotFound(),
                            jsonPath("$.succeed").value(false),
                            jsonPath("$.code").value(ErrorCode.MEMBER_NOT_FOUND.getCode()),
                            jsonPath("$.message").value(ErrorCode.MEMBER_NOT_FOUND.getMessage())
                    );

            BDDMockito.then(notificationSettingQueryService).should(BDDMockito.times(1)).getAllDeviceNotificationSettings(fcmToken);
        }
    }
}
