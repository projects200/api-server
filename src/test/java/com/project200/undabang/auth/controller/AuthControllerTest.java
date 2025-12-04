package com.project200.undabang.auth.controller;

import com.project200.undabang.auth.dto.request.LoginRequestDto;
import com.project200.undabang.auth.service.AuthService;
import com.project200.undabang.common.web.exception.CustomException;
import com.project200.undabang.common.web.exception.ErrorCode;
import com.project200.undabang.configuration.AbstractRestDocSupport;
import com.project200.undabang.configuration.RestDocsUtils;
import com.project200.undabang.member.entity.Member;
import com.project200.undabang.notification.fcm.entity.FcmAccessMode;
import com.project200.undabang.notification.fcm.entity.FcmPlatform;
import com.project200.undabang.notification.fcm.service.FcmTokenCommandService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
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
import static org.springframework.restdocs.payload.PayloadDocumentation.*;

@WebMvcTest(AuthController.class)
@DisplayName("AuthController 테스트")
class AuthControllerTest extends AbstractRestDocSupport {

    private final UUID memberId = UUID.randomUUID();
    private final Member member = Member.builder().memberId(memberId).build();
    @MockitoBean
    private AuthService authService;
    @MockitoBean
    private FcmTokenCommandService fcmTokenCommandService;

    @Nested
    @DisplayName("로그인 API 테스트")
    class Login {

        @Test
        @DisplayName("로그인 성공 - FCM 토큰 및 정보 포함")
        void loginMember_Success_WithFcmToken() throws Exception {
            // given
            String fcmToken = "test-fcm-token";
            String userAgent = "Test-User-Agent";
            LoginRequestDto requestDto = new LoginRequestDto(FcmPlatform.IOS, FcmAccessMode.APP);

            BDDMockito.given(authService.login()).willReturn(member);

            // when & then
            mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .accept(MediaType.APPLICATION_JSON)
                            .header("User-Agent", userAgent)
                            .header("X-Fcm-Token", fcmToken)
                            .headers(getCommonApiHeaders(memberId))
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andExpect(MockMvcResultMatchers.status().isOk())
                    .andDo(document.document(
                            requestHeaders(
                                    RestDocsUtils.HEADER_ACCESS_TOKEN,
                                    headerWithName("User-Agent").attributes(getTypeFormat(JsonFieldType.STRING))
                                            .description("사용자 디바이스 정보 (선택)").optional(),
                                    headerWithName("X-Fcm-Token").attributes(getTypeFormat(JsonFieldType.STRING))
                                            .description("FCM 푸시 알림을 위한 토큰 (선택)").optional()
                            ),
                            requestFields(
                                    fieldWithPath("platform").type(JsonFieldType.STRING)
                                            .description("플랫폼 정보 [IOS, ANDROID, WEB] (FCM 토큰 전송 시 필수)"),
                                    fieldWithPath("accessMode").type(JsonFieldType.STRING)
                                            .description("접속 모드 [APP, PWA, BROWSER] (FCM 토큰 전송 시 필수)")
                            ),
                            responseFields(
                                    RestDocsUtils.commonResponseFieldsOnly()
                            )
                    ));

            BDDMockito.then(authService).should().login();
            BDDMockito.then(fcmTokenCommandService).should()
                    .saveFcmToken(ArgumentMatchers.refEq(member), ArgumentMatchers.eq(fcmToken), ArgumentMatchers.eq(userAgent), ArgumentMatchers.refEq(requestDto));
        }

        @Test
        @DisplayName("로그인 성공 - FCM 토큰 미포함 (Body 포함)")
        void loginMember_Success_WithoutFcmToken() throws Exception {
            // given
            LoginRequestDto requestDto = new LoginRequestDto(FcmPlatform.PC, FcmAccessMode.BROWSER);
            BDDMockito.given(authService.login()).willReturn(member);

            // when & then
            mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .accept(MediaType.APPLICATION_JSON)
                            .headers(getCommonApiHeaders(memberId))
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andExpect(MockMvcResultMatchers.status().isOk());

            BDDMockito.then(authService).should().login();
            // 토큰이 없으므로 saveFcmToken은 호출되지 않아야 함
            BDDMockito.then(fcmTokenCommandService).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("로그인 성공 - FCM 토큰 및 Body 미포함 (순수 로그인)")
        void loginMember_Success_PureLogin() throws Exception {
            // given
            BDDMockito.given(authService.login()).willReturn(member);

            // when & then
            mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .accept(MediaType.APPLICATION_JSON)
                            .headers(getCommonApiHeaders(memberId))) // Content 없이 요청
                    .andExpect(MockMvcResultMatchers.status().isOk())
                    .andDo(document.document(
                            requestHeaders(
                                    RestDocsUtils.HEADER_ACCESS_TOKEN
                            ),
                            responseFields(
                                    RestDocsUtils.commonResponseFieldsOnly()
                            )
                    ));

            BDDMockito.then(authService).should().login();
            BDDMockito.then(fcmTokenCommandService).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("로그인 실패 - FCM 토큰은 있으나 플랫폼 정보(Body)가 없음")
        void loginMember_Fail_TokenProvided_But_BodyMissing() throws Exception {
            // given
            String fcmToken = "test-token";
            BDDMockito.given(authService.login()).willReturn(member);

            // when & then
            mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .accept(MediaType.APPLICATION_JSON)
                            .header("X-Fcm-Token", fcmToken)
                            .headers(getCommonApiHeaders(memberId))) // Body 없음
                    // Controller에서 throw new CustomException(...)을 던지므로 에러 응답 예상
                    // ErrorCode.FCM_TOKEN_NOT_FOUND의 HttpStatus에 맞춰 수정 (여기선 4xx로 가정)
                    .andExpect(MockMvcResultMatchers.status().is4xxClientError());

            BDDMockito.then(authService).should().login();
            // 예외가 발생하여 저장 로직까지 도달하지 않아야 함
            BDDMockito.then(fcmTokenCommandService).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("로그인 실패 - 존재하지 않는 회원")
        void loginMember_Fail_MemberNotFound() throws Exception {
            // given
            LoginRequestDto requestDto = new LoginRequestDto(FcmPlatform.ANDROID, FcmAccessMode.APP);
            BDDMockito.given(authService.login()).willThrow(new CustomException(ErrorCode.LOGIN_FAILED));

            // when & then
            mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .accept(MediaType.APPLICATION_JSON)
                            .headers(getCommonApiHeaders(UUID.randomUUID()))
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andExpect(MockMvcResultMatchers.status().isUnauthorized())
                    .andDo(document.document(
                            requestHeaders(
                                    RestDocsUtils.HEADER_ACCESS_TOKEN
                            ),
                            responseFields(
                                    RestDocsUtils.commonResponseFieldsOnly()
                            )
                    ));

            BDDMockito.then(authService).should().login();
            BDDMockito.then(fcmTokenCommandService).shouldHaveNoInteractions();
        }
    }


    @Nested
    @DisplayName("로그아웃 API 테스트")
    class Logout {

        @Test
        @DisplayName("로그아웃 성공 - FCM 토큰 포함")
        void logoutMember_Success_WithFcmToken() throws Exception {
            // given
            String fcmToken = "test-fcm-token";
            BDDMockito.given(authService.logout()).willReturn(member);

            // when & then
            mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/logout")
                            .contentType(MediaType.APPLICATION_JSON)
                            .accept(MediaType.APPLICATION_JSON)
                            .header("X-Fcm-Token", fcmToken)
                            .headers(getCommonApiHeaders(memberId)))
                    .andExpect(MockMvcResultMatchers.status().isOk())
                    .andDo(document.document(
                            requestHeaders(
                                    RestDocsUtils.HEADER_ACCESS_TOKEN,
                                    headerWithName("X-Fcm-Token").attributes(getTypeFormat(JsonFieldType.STRING))
                                            .description("비활성화할 FCM 토큰 (선택)").optional()
                            ),
                            responseFields(
                                    RestDocsUtils.commonResponseFieldsOnly()
                            )
                    ));

            BDDMockito.then(authService).should().logout();
            BDDMockito.then(fcmTokenCommandService).should().deactivateFcmToken(member, fcmToken);
        }

        @Test
        @DisplayName("로그아웃 성공 - FCM 토큰 미포함")
        void logoutMember_Success_WithoutFcmToken() throws Exception {
            // given
            BDDMockito.given(authService.logout()).willReturn(member);

            // when & then
            mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/logout")
                            .contentType(MediaType.APPLICATION_JSON)
                            .accept(MediaType.APPLICATION_JSON)
                            .headers(getCommonApiHeaders(memberId)))
                    .andExpect(MockMvcResultMatchers.status().isOk());

            BDDMockito.then(authService).should().logout();
            BDDMockito.then(fcmTokenCommandService).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("로그아웃 실패 - 존재하지 않는 회원")
        void logoutMember_Fail_MemberNotFound() throws Exception {
            // given
            BDDMockito.given(authService.logout()).willThrow(new CustomException(ErrorCode.LOGOUT_FAILED));

            // when & then
            mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/logout")
                            .contentType(MediaType.APPLICATION_JSON)
                            .accept(MediaType.APPLICATION_JSON)
                            .headers(getCommonApiHeaders(UUID.randomUUID()))) // 존재하지 않는 UUID
                    .andExpect(MockMvcResultMatchers.status().isUnauthorized())
                    .andDo(document.document(
                            requestHeaders(
                                    RestDocsUtils.HEADER_ACCESS_TOKEN
                            ),
                            responseFields(
                                    RestDocsUtils.commonResponseFieldsOnly()
                            )
                    ));

            BDDMockito.then(authService).should().logout();
            BDDMockito.then(fcmTokenCommandService).shouldHaveNoInteractions();
        }
    }
}