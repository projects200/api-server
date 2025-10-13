package com.project200.undabang.chat.controller;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.project200.undabang.chat.dto.request.CreateChatroomRequest;
import com.project200.undabang.chat.dto.response.CreateChatroomResponse;
import com.project200.undabang.chat.service.ChatCommandService;
import com.project200.undabang.common.web.exception.CustomException;
import com.project200.undabang.common.web.exception.ErrorCode;
import com.project200.undabang.configuration.AbstractRestDocSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.BDDMockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.UUID;

import static com.project200.undabang.configuration.HeadersGenerator.getCommonApiHeaders;
import static com.project200.undabang.configuration.RestDocsUtils.HEADER_ACCESS_TOKEN;
import static com.project200.undabang.configuration.RestDocsUtils.commonResponseFields;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ChatCommandController.class)
class ChatCommandControllerTest extends AbstractRestDocSupport {

    @MockitoBean
    private ChatCommandService chatCommandService;

    @Autowired
    private ObjectMapper objectMapper;

    private CreateChatroomRequest createRequest(UUID targetMemberId) {
        return new CreateChatroomRequest(targetMemberId);
    }

    private CreateChatroomResponse createResponse(long chatroomId) {
        return new CreateChatroomResponse(chatroomId);
    }

    @Nested
    @DisplayName("POST /api/v1/chat-rooms API는")
    class CreateChatRoom {

        @Test
        @DisplayName("채팅방을 성공적으로 생성한다")
        void createChatRoom_Success() throws Exception {
            // given
            UUID memberId = UUID.randomUUID();
            UUID targetMemberId = UUID.randomUUID();
            CreateChatroomRequest request = createRequest(targetMemberId);
            CreateChatroomResponse response = createResponse(1L);

            BDDMockito.given(chatCommandService.createChatroom(any(CreateChatroomRequest.class)))
                    .willReturn(response);

            // when & then
            mockMvc.perform(post("/api/v1/chat-rooms")
                            .contentType(MediaType.APPLICATION_JSON)
                            .accept(MediaType.APPLICATION_JSON)
                            .headers(getCommonApiHeaders(memberId))
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpectAll(
                            status().isCreated(),
                            jsonPath("$.succeed").value(true),
                            jsonPath("$.code").value("CREATED"),
                            jsonPath("$.data.chatRoomId").value(1L)
                    )
                    .andDo(document.document(
                            requestHeaders(HEADER_ACCESS_TOKEN),
                            requestFields(
                                    fieldWithPath("receiverId").type(JsonFieldType.STRING).description("채팅을 시작할 상대방의 식별자(UUID) 입니다.")
                            ),
                            responseFields(commonResponseFields(
                                    fieldWithPath("data.chatRoomId").type(JsonFieldType.NUMBER).description("생성되거나 조회된 채팅방의 식별자 입니다.")
                            ))
                    ));

            BDDMockito.then(chatCommandService).should(BDDMockito.times(1)).createChatroom(any(CreateChatroomRequest.class));
        }

        @Test
        @DisplayName("자기 자신과의 채팅방 생성을 시도하면 409 Conflict 에러를 반환한다")
        void createChatRoom_Fail_SelfChat() throws Exception {
            // given
            UUID memberId = UUID.randomUUID();
            CreateChatroomRequest request = createRequest(memberId); // 자기 자신을 타겟으로 설정

            BDDMockito.given(chatCommandService.createChatroom(any(CreateChatroomRequest.class)))
                    .willThrow(new CustomException(ErrorCode.SELF_CHAT_NOT_ALLOWED));

            // when & then
            mockMvc.perform(post("/api/v1/chat-rooms")
                            .contentType(MediaType.APPLICATION_JSON)
                            .accept(MediaType.APPLICATION_JSON)
                            .headers(getCommonApiHeaders(memberId))
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpectAll(
                            status().isBadRequest(),
                            jsonPath("$.succeed").value(false),
                            jsonPath("$.code").value(ErrorCode.SELF_CHAT_NOT_ALLOWED.getCode()),
                            jsonPath("$.message").value(ErrorCode.SELF_CHAT_NOT_ALLOWED.getMessage())
                    );
        }

        @Test
        @DisplayName("요청 DTO의 targetMemberId가 null이면 400 Bad Request 에러를 반환한다")
        void createChatRoom_Fail_Validation() throws Exception {
            // given
            UUID memberId = UUID.randomUUID();
            CreateChatroomRequest requestWithNullId = createRequest(null);

            // when & then
            mockMvc.perform(post("/api/v1/chat-rooms")
                            .contentType(MediaType.APPLICATION_JSON)
                            .accept(MediaType.APPLICATION_JSON)
                            .headers(getCommonApiHeaders(memberId))
                            .content(objectMapper.writeValueAsString(requestWithNullId)))
                    .andExpect(status().isBadRequest());

            BDDMockito.then(chatCommandService).shouldHaveNoInteractions();
        }
    }
}