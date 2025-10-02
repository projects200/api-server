package com.project200.undabang.openchat.controller;

import com.project200.undabang.common.web.exception.CustomException;
import com.project200.undabang.common.web.exception.ErrorCode;
import com.project200.undabang.configuration.AbstractRestDocSupport;
import com.project200.undabang.openchat.dto.response.GetOpenChatUrlResponse;
import com.project200.undabang.openchat.dto.response.GetOtherMemberOpenChatUrlResponse;
import com.project200.undabang.openchat.service.OpenChatRoomQueryService;
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
import static com.project200.undabang.configuration.RestDocsUtils.HEADER_ACCESS_TOKEN;
import static com.project200.undabang.configuration.RestDocsUtils.commonResponseFields;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OpenChatRoomQueryController.class)
class OpenChatRoomQueryControllerTest extends AbstractRestDocSupport {

    @MockitoBean
    private OpenChatRoomQueryService openChatQueryService;

    @Nested
    @DisplayName("GET /api/v1/members/{memberId}/open-chat API는")
    class GetOtherMemberOpenChatUrl {

        @Test
        @DisplayName("다른 회원의 오픈채팅 URL을 성공적으로 조회한다")
        void getOtherMemberOpenChatUrl_Success() throws Exception {
            // given
            UUID memberId = UUID.randomUUID();
            UUID targetMemberId = UUID.randomUUID();
            String openChatUrl = "https://open.kakao.com/o/testurl123";
            GetOtherMemberOpenChatUrlResponse response = createOtherMemberChatUrlResponse(openChatUrl);

            BDDMockito.given(openChatQueryService.getOtherMemberOpenChatroomUrl(targetMemberId)).willReturn(response);

            // when & then
            mockMvc.perform(get("/api/v1/members/{memberId}/open-chat", targetMemberId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .accept(MediaType.APPLICATION_JSON)
                            .headers(getCommonApiHeaders(memberId))) // 요청자는 memberId
                    .andExpectAll(
                            status().isOk(),
                            jsonPath("$.succeed").value(true),
                            jsonPath("$.code").value("SUCCESS"),
                            jsonPath("$.data.openChatroomUrl").value(openChatUrl)
                    )
                    .andDo(document.document(
                            requestHeaders(HEADER_ACCESS_TOKEN),
                            pathParameters(
                                    parameterWithName("memberId").attributes(getTypeFormat(JsonFieldType.STRING)).description("조회하려는 다른 회원의 식별자(UUID) 입니다.")
                            ),
                            responseFields(commonResponseFields(
                                    fieldWithPath("data.openChatroomUrl").type(JsonFieldType.STRING).description("다른 회원의 카카오톡 오픈채팅방 링크 입니다.")
                            ))
                    ));

            BDDMockito.then(openChatQueryService).should(BDDMockito.times(1)).getOtherMemberOpenChatroomUrl(targetMemberId);
        }

        @Test
        @DisplayName("조회하려는 회원이 존재하지 않으면 실패한다")
        void getOtherMemberOpenChatUrl_Fail_MemberNotFound() throws Exception {
            // given
            UUID memberId = UUID.randomUUID();
            UUID nonExistentMemberId = UUID.randomUUID();
            BDDMockito.given(openChatQueryService.getOtherMemberOpenChatroomUrl(nonExistentMemberId))
                    .willThrow(new CustomException(ErrorCode.MEMBER_NOT_FOUND));

            // when & then
            mockMvc.perform(get("/api/v1/members/{memberId}/open-chat", nonExistentMemberId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .accept(MediaType.APPLICATION_JSON)
                            .headers(getCommonApiHeaders(memberId)))
                    .andExpectAll(
                            status().isNotFound(),
                            jsonPath("$.succeed").value(false),
                            jsonPath("$.code").value(ErrorCode.MEMBER_NOT_FOUND.getCode()),
                            jsonPath("$.message").value(ErrorCode.MEMBER_NOT_FOUND.getMessage())
                    );

            BDDMockito.then(openChatQueryService).should(BDDMockito.times(1)).getOtherMemberOpenChatroomUrl(nonExistentMemberId);
        }
    }

    private GetOtherMemberOpenChatUrlResponse createOtherMemberChatUrlResponse(String url) {
        return GetOtherMemberOpenChatUrlResponse.builder()
                .openChatroomUrl(url)
                .build();
    }

    private GetOpenChatUrlResponse createMyChatUrlResponse(Long id, String url) {
        return GetOpenChatUrlResponse.builder()
                .openChatroomId(id)
                .openChatroomUrl(url)
                .build();
    }

    @Nested
    @DisplayName("GET /api/v1/open-chats API는")
    class GetMyOpenChatUrl {

        @Test
        @DisplayName("자신의 오픈채팅 URL을 성공적으로 조회한다")
        void getMyOpenChatUrl_Success() throws Exception {
            // given
            UUID memberId = UUID.randomUUID();
            Long openChatId = 123456789L;
            String openChatUrl = "https://open.kakao.com/o/myurl456";
            GetOpenChatUrlResponse response = createMyChatUrlResponse(openChatId, openChatUrl);

            BDDMockito.given(openChatQueryService.getOpenChatroomUrl()).willReturn(response);

            // when & then
            mockMvc.perform(get("/api/v1/open-chats")
                            .contentType(MediaType.APPLICATION_JSON)
                            .accept(MediaType.APPLICATION_JSON)
                            .headers(getCommonApiHeaders(memberId)))
                    .andExpectAll(
                            status().isOk(),
                            jsonPath("$.succeed").value(true),
                            jsonPath("$.code").value("SUCCESS"),
                            jsonPath("$.data.openChatroomId").value(openChatId),
                            jsonPath("$.data.openChatroomUrl").value(openChatUrl)
                    )
                    .andDo(document.document(
                            requestHeaders(HEADER_ACCESS_TOKEN),
                            responseFields(commonResponseFields(
                                    fieldWithPath("data.openChatroomId").type(JsonFieldType.NUMBER).description("자신의 카카오톡 오픈채팅방 식별자 정보 입니다."),
                                    fieldWithPath("data.openChatroomUrl").type(JsonFieldType.STRING).description("자신의 카카오톡 오픈채팅방 URL 링크 입니다.")
                            ))
                    ));

            BDDMockito.then(openChatQueryService).should(BDDMockito.times(1)).getOpenChatroomUrl();
        }

        @Test
        @DisplayName("자신의 오픈채팅 URL이 존재하지 않으면 실패한다")
        void getMyOpenChatUrl_Fail_NotFound() throws Exception {
            // given
            UUID memberId = UUID.randomUUID();
            BDDMockito.given(openChatQueryService.getOpenChatroomUrl())
                    .willThrow(new CustomException(ErrorCode.OPEN_CHAT_ROOM_NOT_FOUND));

            // when & then
            mockMvc.perform(get("/api/v1/open-chats")
                            .contentType(MediaType.APPLICATION_JSON)
                            .accept(MediaType.APPLICATION_JSON)
                            .headers(getCommonApiHeaders(memberId)))
                    .andExpectAll(
                            status().isNotFound(), // OPEN_CHAT_ROOM_NOT_FOUND는 404를 반환한다고 가정
                            jsonPath("$.succeed").value(false),
                            jsonPath("$.code").value(ErrorCode.OPEN_CHAT_ROOM_NOT_FOUND.getCode()),
                            jsonPath("$.message").value(ErrorCode.OPEN_CHAT_ROOM_NOT_FOUND.getMessage())
                    );

            BDDMockito.then(openChatQueryService).should(BDDMockito.times(1)).getOpenChatroomUrl();
        }
    }
}