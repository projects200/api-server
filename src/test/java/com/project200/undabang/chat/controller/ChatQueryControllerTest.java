package com.project200.undabang.chat.controller;


import com.project200.undabang.chat.dto.response.GetMemberChatroomResponse;
import com.project200.undabang.chat.service.ChatQueryService;
import com.project200.undabang.common.web.exception.CustomException;
import com.project200.undabang.common.web.exception.ErrorCode;
import com.project200.undabang.common.web.response.CommonResponse;
import com.project200.undabang.configuration.AbstractRestDocSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.BDDMockito;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static com.project200.undabang.configuration.HeadersGenerator.getCommonApiHeaders;
import static com.project200.undabang.configuration.RestDocsUtils.HEADER_ACCESS_TOKEN;
import static com.project200.undabang.configuration.RestDocsUtils.commonResponseFieldsForList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ChatQueryController.class)
class ChatQueryControllerTest extends AbstractRestDocSupport {

    @MockitoBean
    private ChatQueryService chatQueryService;

    private GetMemberChatroomResponse createChatroomResponse(Long chatRoomId, String otherNickname, String lastChat, Long unreadCount, LocalDateTime receivedAt) {
        return GetMemberChatroomResponse.builder()
                .chatRoomId(chatRoomId)
                .otherMemberNickname(otherNickname)
                .otherMemberProfileImageUrl("https://example.com/profile.jpg")
                .otherMemberThumbnailImageUrl("https://example.com/thumbnail.jpg")
                .lastChatContent(lastChat)
                .lastChatReceivedAt(receivedAt)
                .unreadCount(unreadCount)
                .build();
    }

    @Nested
    @DisplayName("GET /api/v1/chat-rooms API는")
    class GetChatRoomList {

        @Test
        @DisplayName("채팅방 목록을 성공적으로 조회한다")
        void getChatRoomList_Success() throws Exception {
            // given
            UUID memberId = UUID.randomUUID();
            LocalDateTime fixedTime = LocalDateTime.of(2024, 1, 1, 10, 0, 0);
            List<GetMemberChatroomResponse> responseList = List.of(
                    createChatroomResponse(1L, "상대방1", "안녕하세요", 3L, fixedTime),
                    createChatroomResponse(2L, "상대방2", "네, 반갑습니다.", 0L, fixedTime.plusHours(1))
            );

            BDDMockito.given(chatQueryService.getMemberChatroomList()).willReturn(responseList);

            // when
            String response = mockMvc.perform(get("/api/v1/chat-rooms")
                            .contentType(MediaType.APPLICATION_JSON)
                            .accept(MediaType.APPLICATION_JSON)
                            .headers(getCommonApiHeaders(memberId)))
                    .andExpect(status().isOk())
                    .andDo(document.document(
                            requestHeaders(HEADER_ACCESS_TOKEN),
                            responseFields(commonResponseFieldsForList(
                                    fieldWithPath("data[].chatRoomId").type(JsonFieldType.NUMBER).description("채팅방 식별자 정보 입니다."),
                                    fieldWithPath("data[].otherMemberNickname").type(JsonFieldType.STRING).description("상대방 닉네임 정보 입니다."),
                                    fieldWithPath("data[].otherMemberProfileImageUrl").type(JsonFieldType.STRING).description("상대방 프로필 이미지 URL 입니다."),
                                    fieldWithPath("data[].otherMemberThumbnailImageUrl").type(JsonFieldType.STRING).description("상대방 썸네일 이미지 URL 입니다."),
                                    fieldWithPath("data[].lastChatContent").type(JsonFieldType.STRING).description("특정 채팅방의 마지막 대화 내용 입니다."),
                                    fieldWithPath("data[].lastChatReceivedAt").type(JsonFieldType.STRING).description("특정 채팅방의 마지막 대화 수신 시간을 나타냅니다."),
                                    fieldWithPath("data[].unreadCount").type(JsonFieldType.NUMBER).description("특정 채팅방의 읽지 않은 메시지 수를 의미합니다.")
                            ))
                    ))
                    .andReturn().getResponse().getContentAsString();

            // then
            String expected = objectMapper.writeValueAsString(CommonResponse.success(responseList));
            assertThat(response).isEqualTo(expected);
            BDDMockito.then(chatQueryService).should(BDDMockito.times(1)).getMemberChatroomList();
        }

        @Test
        @DisplayName("참여중인 채팅방이 없을 경우 빈 목록을 반환한다")
        void getChatRoomList_Success_Empty() throws Exception {
            // given
            UUID memberId = UUID.randomUUID();
            BDDMockito.given(chatQueryService.getMemberChatroomList()).willReturn(Collections.emptyList());

            // when
            String response = mockMvc.perform(get("/api/v1/chat-rooms")
                            .contentType(MediaType.APPLICATION_JSON)
                            .accept(MediaType.APPLICATION_JSON)
                            .headers(getCommonApiHeaders(memberId)))
                    .andExpect(status().isOk())
                    .andDo(document.document(
                            requestHeaders(HEADER_ACCESS_TOKEN),
                            responseFields(commonResponseFieldsForList(
                                    // data가 빈 배열일 경우, 내부 필드를 기술하지 않습니다.
                            ))
                    ))
                    .andReturn().getResponse().getContentAsString();

            // then
            String expected = objectMapper.writeValueAsString(CommonResponse.success(Collections.emptyList()));
            assertThat(response).isEqualTo(expected);
            BDDMockito.then(chatQueryService).should(BDDMockito.times(1)).getMemberChatroomList();
        }

        @Test
        @DisplayName("사용자를 찾을 수 없을 경우 404 Not Found를 반환한다")
        void getChatRoomList_Fail_MemberNotFound() throws Exception {
            // given
            UUID memberId = UUID.randomUUID();
            BDDMockito.given(chatQueryService.getMemberChatroomList()).willThrow(new CustomException(ErrorCode.MEMBER_NOT_FOUND));

            // when & then
            mockMvc.perform(get("/api/v1/chat-rooms")
                            .contentType(MediaType.APPLICATION_JSON)
                            .accept(MediaType.APPLICATION_JSON)
                            .headers(getCommonApiHeaders(memberId)))
                    .andExpect(status().isNotFound());

            BDDMockito.then(chatQueryService).should(BDDMockito.times(1)).getMemberChatroomList();
        }
    }
}