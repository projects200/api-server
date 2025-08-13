package com.project200.undabang.auth.controller;

import com.project200.undabang.auth.service.AuthService;
import com.project200.undabang.common.web.exception.CustomException;
import com.project200.undabang.common.web.exception.ErrorCode;
import com.project200.undabang.configuration.AbstractRestDocSupport;
import com.project200.undabang.configuration.RestDocsUtils;
import com.project200.undabang.member.entity.Member;
import com.project200.undabang.push_message.service.FcmTokenCommandService;
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
        @DisplayName("로그인 성공 - FCM 토큰 포함")
        void loginMember_Success_WithFcmToken() throws Exception {
            // given
            String fcmToken = "test-fcm-token";
            String userAgent = "Test-User-Agent";
            BDDMockito.given(authService.login()).willReturn(member);

            // when & then
            mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .accept(MediaType.APPLICATION_JSON)
                            .header("User-Agent", userAgent)
                            .header("X-Fcm-Token", fcmToken)
                            .headers(getCommonApiHeaders(memberId)))
                    .andExpect(MockMvcResultMatchers.status().isOk())
                    .andDo(document.document(
                            requestHeaders(
                                    RestDocsUtils.HEADER_ACCESS_TOKEN,
                                    headerWithName("User-Agent").attributes(getTypeFormat(JsonFieldType.STRING))
                                            .description("사용자 디바이스 정보 (선택)").optional(),
                                    headerWithName("X-Fcm-Token").attributes(getTypeFormat(JsonFieldType.STRING))
                                            .description("FCM 푸시 알림을 위한 토큰 (선택)").optional()
                            ),
                            responseFields(
                                    RestDocsUtils.commonResponseFieldsOnly()
                            )
                    ));

            BDDMockito.then(authService).should().login();
            BDDMockito.then(fcmTokenCommandService).should().saveFcmToken(member, fcmToken, userAgent);
        }

        @Test
        @DisplayName("로그인 성공 - FCM 토큰 미포함")
        void loginMember_Success_WithoutFcmToken() throws Exception {
            // given
            BDDMockito.given(authService.login()).willReturn(member);

            // when & then
            mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .accept(MediaType.APPLICATION_JSON)
                            .headers(getCommonApiHeaders(memberId)))
                    .andExpect(MockMvcResultMatchers.status().isOk());

            BDDMockito.then(authService).should().login();
            BDDMockito.then(fcmTokenCommandService).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("로그인 실패 - 존재하지 않는 회원")
        void loginMember_Fail_MemberNotFound() throws Exception {
            // given
            BDDMockito.given(authService.login()).willThrow(new CustomException(ErrorCode.LOGIN_FAILED));


            // when & then
            mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/login")
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