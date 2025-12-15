package com.project200.undabang.notification.controller;

import com.project200.undabang.common.web.exception.CustomException;
import com.project200.undabang.common.web.exception.ErrorCode;
import com.project200.undabang.configuration.AbstractRestDocSupport;
import com.project200.undabang.notification.dto.record.NotificationSettingRecord;
import com.project200.undabang.notification.dto.request.UpdateDeviceNotificationSettingRequest;
import com.project200.undabang.notification.dto.response.UpdateDeviceNotificationSettingResponse;
import com.project200.undabang.notification.service.NotificationSettingCommandService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.UUID;

import static com.project200.undabang.configuration.DocumentFormatGenerator.getTypeFormat;
import static com.project200.undabang.configuration.HeadersGenerator.getCommonApiHeaders;
import static com.project200.undabang.configuration.RestDocsUtils.HEADER_ACCESS_TOKEN;
import static com.project200.undabang.configuration.RestDocsUtils.commonResponseFields;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(NotificationSettingCommandController.class)
class NotificationSettingCommandControllerTest extends AbstractRestDocSupport {

    @MockitoBean
    private NotificationSettingCommandService notificationSettingCommandService;

    @Nested
    @DisplayName("PATCH /api/v1/notification-settings/device API는")
    class UpdateDeviceNotificationSettings {

        @Test
        @DisplayName("정상적으로 디바이스 알림 설정을 업데이트한다")
        void updateDeviceNotificationSettings_Success() throws Exception {
            // given
            UUID memberId = UUID.randomUUID();
            String fcmToken = "test-fcm-token";

            List<UpdateDeviceNotificationSettingRequest> requestList = List.of(
                    new UpdateDeviceNotificationSettingRequest("CHAT_MESSAGE", true),
                    new UpdateDeviceNotificationSettingRequest("WORKOUT_REMINDER", false)
            );

            UpdateDeviceNotificationSettingResponse serviceResponse = UpdateDeviceNotificationSettingResponse.builder()
                    .fcmToken(fcmToken)
                    .settings(List.of(
                            new NotificationSettingRecord("CHAT_MESSAGE", true),
                            new NotificationSettingRecord("WORKOUT_REMINDER", false)
                    ))
                    .build();

            given(notificationSettingCommandService.updateDeviceNotificationSetting(eq(fcmToken), any(List.class)))
                    .willReturn(serviceResponse);

            // when & then
            mockMvc.perform(patch("/api/v1/notification-settings/device")
                            .contentType(MediaType.APPLICATION_JSON)
                            .accept(MediaType.APPLICATION_JSON)
                            .headers(getCommonApiHeaders(memberId)) // AbstractRestDocSupport의 헬퍼 메소드 사용 가정
                            .header("X-Fcm-Token", fcmToken)
                            .content(objectMapper.writeValueAsString(requestList)) // 요청 Body를 JSON 문자열로 변환
                    )
                    .andExpectAll(
                            status().isOk(),
                            jsonPath("$.succeed").value(true),
                            jsonPath("$.data.fcmToken").value(fcmToken),
                            jsonPath("$.data.settings[0].type").value("CHAT_MESSAGE"),
                            jsonPath("$.data.settings[0].enabled").value(true)
                    )
                    .andDo(document.document(
                            requestHeaders(
                                    HEADER_ACCESS_TOKEN, // 공통 헤더
                                    headerWithName("X-Fcm-Token").attributes(getTypeFormat(JsonFieldType.STRING)).description("설정을 변경할 디바이스의 FCM 토큰을 의미합니다.")
                            ),
                            requestFields(
                                    fieldWithPath("[]").type(JsonFieldType.ARRAY).description("알림 설정 on/off 목록을 의미합니다."),
                                    fieldWithPath("[].type").type(JsonFieldType.STRING).description("알림 타입을 의미합니다. 예시로 CHAT_MESSAGE, WORKOUT_REMINDER 등이 있습니다."),
                                    fieldWithPath("[].enabled").type(JsonFieldType.BOOLEAN).description("해당 알림의 활성화 여부를 의미합니다.")
                            ),
                            responseFields(commonResponseFields(
                                    fieldWithPath("data.fcmToken").type(JsonFieldType.STRING).description("업데이트된 FCM 토큰을 의미합니다. 같은 기기여도 토큰이 다를 수 있습니다."),
                                    fieldWithPath("data.settings").type(JsonFieldType.ARRAY).description("업데이트 완료된 전체 설정 목록을 의미합니다."),
                                    fieldWithPath("data.settings[].type").type(JsonFieldType.STRING).description("특정 알림 타입을 의미합니다. (채팅메시지, 운동격려 알림 등)"),
                                    fieldWithPath("data.settings[].enabled").type(JsonFieldType.BOOLEAN).description("변경된 활성화 상태 여부를 나타냅니다.")
                            ))
                    ));

            // 4. 서비스 메소드가 정확히 1번 호출되었는지 검증합니다.
            then(notificationSettingCommandService).should(times(1)).updateDeviceNotificationSetting(eq(fcmToken), any(List.class));
        }

        @Test
        @DisplayName("서비스 로직에서 토큰을 찾지 못해 실패한다")
        void updateDeviceNotificationSettings_Fail_TokenNotFound() throws Exception {
            // given
            UUID memberId = UUID.randomUUID();
            String fcmToken = "invalid-fcm-token";
            List<UpdateDeviceNotificationSettingRequest> requestList = List.of(
                    new UpdateDeviceNotificationSettingRequest("CHAT_MESSAGE", true)
            );

            // 서비스가 CustomException을 던지도록 설정합니다.
            given(notificationSettingCommandService.updateDeviceNotificationSetting(eq(fcmToken), any(List.class)))
                    .willThrow(new CustomException(ErrorCode.FCM_TOKEN_NOT_FOUND));

            // when & then
            mockMvc.perform(patch("/api/v1/notification-settings/device")
                            .contentType(MediaType.APPLICATION_JSON)
                            .headers(getCommonApiHeaders(memberId))
                            .header("X-Fcm-Token", fcmToken)
                            .content(objectMapper.writeValueAsString(requestList))
                    )
                    .andExpectAll(
                            status().isNotFound(), // CustomException에 정의된 HTTP 상태 코드
                            jsonPath("$.succeed").value(false),
                            jsonPath("$.code").value(ErrorCode.FCM_TOKEN_NOT_FOUND.getCode())
                    );
        }

        @Test
        @DisplayName("요청 Body가 비어있어 Validation에 실패한다")
        void updateDeviceNotificationSettings_Fail_Validation_BodyEmpty() throws Exception {
            // given
            UUID memberId = UUID.randomUUID();
            String fcmToken = "test-fcm-token";
            String emptyRequestBody = "[]"; // @NotEmpty 위반

            // when & then
            mockMvc.perform(patch("/api/v1/notification-settings/device")
                            .contentType(MediaType.APPLICATION_JSON)
                            .headers(getCommonApiHeaders(memberId))
                            .header("X-Fcm-Token", fcmToken)
                            .content(emptyRequestBody)
                    )
                    .andExpectAll(
                            status().isBadRequest(), // Validation 실패 시 400 Bad Request
                            jsonPath("$.succeed").value(false)
                    );

            // Validation 실패 시 서비스는 호출되지 않아야 합니다.
            then(notificationSettingCommandService).should(never()).updateDeviceNotificationSetting(anyString(), any(List.class));
        }
    }
}