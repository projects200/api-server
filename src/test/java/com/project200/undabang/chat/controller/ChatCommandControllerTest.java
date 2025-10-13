package com.project200.undabang.chat.controller;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.project200.undabang.chat.dto.request.CreateChatroomRequest;
import com.project200.undabang.chat.dto.request.CreateMessageRequest;
import com.project200.undabang.chat.dto.response.CreateChatroomResponse;
import com.project200.undabang.chat.dto.response.CreateMessageResponse;
import com.project200.undabang.chat.service.ChatCommandService;
import com.project200.undabang.common.web.exception.CustomException;
import com.project200.undabang.common.web.exception.ErrorCode;
import com.project200.undabang.configuration.AbstractRestDocSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.UUID;

import static com.project200.undabang.configuration.DocumentFormatGenerator.getTypeFormat;
import static com.project200.undabang.configuration.HeadersGenerator.getCommonApiHeaders;
import static com.project200.undabang.configuration.RestDocsUtils.HEADER_ACCESS_TOKEN;
import static com.project200.undabang.configuration.RestDocsUtils.commonResponseFields;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
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

            given(chatCommandService.createChatroom(any(CreateChatroomRequest.class)))
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

            then(chatCommandService).should(times(1)).createChatroom(any(CreateChatroomRequest.class));
        }

        @Test
        @DisplayName("자기 자신과의 채팅방 생성을 시도하면 409 Conflict 에러를 반환한다")
        void createChatRoom_Fail_SelfChat() throws Exception {
            // given
            UUID memberId = UUID.randomUUID();
            CreateChatroomRequest request = createRequest(memberId); // 자기 자신을 타겟으로 설정

            given(chatCommandService.createChatroom(any(CreateChatroomRequest.class)))
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

            then(chatCommandService).shouldHaveNoInteractions();
        }
    }

    @Nested
    @DisplayName("POST /api/v1/chat-rooms/{chatroomId}/messages API는")
    class CreateMessage {

        private final Long chatroomId = 1L;

        @Test
        @DisplayName("메시지를 성공적으로 생성한다")
        void createMessage_Success() throws Exception {
            // given
            UUID memberId = UUID.randomUUID();
            String content = "안녕하세요! 테스트 메시지입니다.";
            CreateMessageRequest request = new CreateMessageRequest(content);
            CreateMessageResponse response = new CreateMessageResponse(100L);

            given(chatCommandService.createMessage(eq(chatroomId), any(CreateMessageRequest.class)))
                    .willReturn(response);

            // when & then
            mockMvc.perform(post("/api/v1/chat-rooms/{chatroomId}/messages", chatroomId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .accept(MediaType.APPLICATION_JSON)
                            .headers(getCommonApiHeaders(memberId))
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpectAll(
                            status().isCreated(),
                            jsonPath("$.succeed").value(true),
                            jsonPath("$.code").value("CREATED"),
                            jsonPath("$.data.chatId").value(100L)
                    )
                    .andDo(document.document(
                            requestHeaders(HEADER_ACCESS_TOKEN),
                            pathParameters(
                                    parameterWithName("chatroomId").attributes(getTypeFormat(JsonFieldType.NUMBER)).description("메시지를 보낼 채팅방의 식별자 입니다.")
                            ),
                            requestFields(
                                    fieldWithPath("content").type(JsonFieldType.STRING).description("전송할 메시지의 내용입니다. 최대 500글자 까지 입력 가능합니다.")
                            ),
                            responseFields(commonResponseFields(
                                    fieldWithPath("data.chatId").type(JsonFieldType.NUMBER).description("생성된 채팅 메시지의 식별자 입니다.")
                            ))
                    ));

            then(chatCommandService).should(times(1)).createMessage(eq(chatroomId), any(CreateMessageRequest.class));
        }

        @Test
        @DisplayName("사용자가 채팅방 멤버가 아닐 경우 404 Not Found 에러를 반환한다")
        void createMessage_Fail_NotMember() throws Exception {
            // given
            UUID memberId = UUID.randomUUID();
            String content = "안녕하세요!";
            CreateMessageRequest request = new CreateMessageRequest(content);

            given(chatCommandService.createMessage(eq(chatroomId), any(CreateMessageRequest.class)))
                    .willThrow(new CustomException(ErrorCode.CHATROOM_MEMBERS_NOT_FOUND));

            // when & then
            mockMvc.perform(post("/api/v1/chat-rooms/{chatroomId}/messages", chatroomId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .accept(MediaType.APPLICATION_JSON)
                            .headers(getCommonApiHeaders(memberId))
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpectAll(
                            status().isNotFound(),
                            jsonPath("$.succeed").value(false),
                            jsonPath("$.code").value(ErrorCode.CHATROOM_MEMBERS_NOT_FOUND.getCode()),
                            jsonPath("$.message").value(ErrorCode.CHATROOM_MEMBERS_NOT_FOUND.getMessage())
                    );
        }

        @Test
        @DisplayName("메시지 내용이 비어있으면 (Validation 실패) 400 Bad Request 에러를 반환한다")
        void createMessage_Fail_Validation_EmptyContent() throws Exception {
            // given
            UUID memberId = UUID.randomUUID();
            // content가 비어있는 요청 생성 (CreateMessageRequest에 @NotBlank 같은 validation이 걸려있다고 가정)
            CreateMessageRequest requestWithEmptyContent = new CreateMessageRequest("");

            // when & then
            mockMvc.perform(post("/api/v1/chat-rooms/{chatroomId}/messages", chatroomId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .accept(MediaType.APPLICATION_JSON)
                            .headers(getCommonApiHeaders(memberId))
                            .content(objectMapper.writeValueAsString(requestWithEmptyContent)))
                    .andExpect(status().isBadRequest());

            then(chatCommandService).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("메시지 내용이 너무 길면 (Validation 실패) 400 Bad Request 에러를 반환한다")
        void createMessage_Fail_Validation_TooLongContent() throws Exception {
            // given
            UUID memberId = UUID.randomUUID();
            // 500자를 초과하는 긴 문자열 (Chat Entity의 @Size(max=500)을 가정)
            String longContent = "a".repeat(501);
            CreateMessageRequest requestWithLongContent = new CreateMessageRequest(longContent);

            // when & then
            mockMvc.perform(post("/api/v1/chat-rooms/{chatroomId}/messages", chatroomId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .accept(MediaType.APPLICATION_JSON)
                            .headers(getCommonApiHeaders(memberId))
                            .content(objectMapper.writeValueAsString(requestWithLongContent)))
                    .andExpect(status().isBadRequest());

            then(chatCommandService).shouldHaveNoInteractions();
        }
    }
}