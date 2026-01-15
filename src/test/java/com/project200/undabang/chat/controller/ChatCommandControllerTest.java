package com.project200.undabang.chat.controller;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.project200.undabang.chat.dto.request.CreateChatroomRequest;
import com.project200.undabang.chat.dto.request.CreateMessageRequest;
import com.project200.undabang.chat.dto.response.CreateChatroomResponse;
import com.project200.undabang.chat.dto.response.CreateMessageResponse;
import com.project200.undabang.chat.dto.response.TicketResponse;
import com.project200.undabang.chat.service.ChatCommandService;
import com.project200.undabang.chat.service.ChatTicketService;
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
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.times;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.delete;
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

    @MockitoBean
    private ChatTicketService chatTicketService;

    @Autowired
    private ObjectMapper objectMapper;

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
                                    fieldWithPath("receiverId").type(JsonFieldType.STRING).description("채팅을 시작할 상대방의 식별자(UUID) 정보를 의미합니다.."),
                                    fieldWithPath("exerciseLocationId").type(JsonFieldType.NUMBER).description("상대방의 운동 장소 식별자를 의미합니다."),
                                    fieldWithPath("requesterLatitude").type(JsonFieldType.NUMBER).description("요청자의 현재 위도를 의미합니다. 위도의 범위는 -90 ~ 90 사이여야 합니다."),
                                    fieldWithPath("requesterLongitude").type(JsonFieldType.NUMBER).description("요청자의 현재 경도를 의미합니다. 경도의 범위는 -180 ~ 180 사이여야 합니다.")
                            ),
                            responseFields(commonResponseFields(
                                    fieldWithPath("data.chatRoomId").type(JsonFieldType.NUMBER).description("생성되거나 조회된 채팅방의 식별자")
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
                            status().isBadRequest(), // or isConflict() 에러 코드 매핑에 따라 다름 (보통 400 or 409)
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
                    .andExpect(status().isBadRequest()); // @Valid 실패 시 400

            then(chatCommandService).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("차단 관계일 경우 403 Forbidden 에러를 반환한다")
        void createChatRoom_Fail_Blocked() throws Exception {
            // given
            UUID memberId = UUID.randomUUID();
            UUID targetMemberId = UUID.randomUUID();
            CreateChatroomRequest request = createRequest(targetMemberId);

            given(chatCommandService.createChatroom(any(CreateChatroomRequest.class)))
                    .willThrow(new CustomException(ErrorCode.CHATROOM_CREATE_BLOCKED));

            // when & then
            mockMvc.perform(post("/api/v1/chat-rooms")
                            .contentType(MediaType.APPLICATION_JSON)
                            .accept(MediaType.APPLICATION_JSON)
                            .headers(getCommonApiHeaders(memberId))
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpectAll(
                            status().isForbidden(),
                            jsonPath("$.succeed").value(false),
                            jsonPath("$.code").value(ErrorCode.CHATROOM_CREATE_BLOCKED.getCode()),
                            jsonPath("$.message").value(ErrorCode.CHATROOM_CREATE_BLOCKED.getMessage())
                    );

            then(chatCommandService).should(times(1)).createChatroom(any(CreateChatroomRequest.class));
        }

        private CreateChatroomRequest createRequest(UUID targetMemberId) {
            return new CreateChatroomRequest(
                    targetMemberId,
                    1L,
                    37.555946,
                    126.972317
            );
        }

        private CreateChatroomResponse createResponse(long chatroomId) {
            return new CreateChatroomResponse(chatroomId);
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

        @Test
        @DisplayName("차단한 사용자에게 메시지를 보내려 하면 403 Forbidden 에러를 반환한다")
        void createMessage_Fail_BlockedUser() throws Exception {
            // given
            UUID memberId = UUID.randomUUID();
            String content = "이 메시지는 차단되어 전송되지 않아야 합니다.";
            CreateMessageRequest request = new CreateMessageRequest(content);

            // [핵심] 서비스 레이어가 '차단' 예외를 던지는 상황을 Mocking
            given(chatCommandService.createMessage(eq(chatroomId), any(CreateMessageRequest.class)))
                    .willThrow(new CustomException(ErrorCode.MESSAGE_SEND_BLOCKED));

            // when & then
            mockMvc.perform(post("/api/v1/chat-rooms/{chatroomId}/messages", chatroomId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .accept(MediaType.APPLICATION_JSON)
                            .headers(getCommonApiHeaders(memberId))
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpectAll(
                            status().isForbidden(), // 403 Forbidden 상태 코드 검증
                            jsonPath("$.succeed").value(false),
                            jsonPath("$.code").value(ErrorCode.MESSAGE_SEND_BLOCKED.getCode()),
                            jsonPath("$.message").value(ErrorCode.MESSAGE_SEND_BLOCKED.getMessage())
                    );

            then(chatCommandService).should(times(1)).createMessage(eq(chatroomId), any(CreateMessageRequest.class));
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/chat-rooms/{chatroomId} API는")
    class LeaveChatRoom {

        private final Long chatroomId = 1L;

        @Test
        @DisplayName("채팅방 나가기를 성공적으로 처리한다")
        void LeaveChatRoom_Success() throws Exception {
            // given
            UUID memberId = UUID.randomUUID();
            // deleteChatroom 서비스 메소드는 void를 반환하므로, 아무것도 하지 않도록 설정
            willDoNothing().given(chatCommandService).leaveChatroom(chatroomId);

            // when & then
            mockMvc.perform(delete("/api/v1/chat-rooms/{chatroomId}", chatroomId)
                            .headers(getCommonApiHeaders(memberId))
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpectAll(
                            status().isOk(),
                            jsonPath("$.succeed").value(true),
                            jsonPath("$.code").value("DELETED"),
                            jsonPath("$.data").doesNotExist() // data 필드가 없는지 확인
                    )
                    .andDo(document.document(
                            requestHeaders(HEADER_ACCESS_TOKEN),
                            pathParameters(
                                    parameterWithName("chatroomId").attributes(getTypeFormat(JsonFieldType.NUMBER)).description("나가기를 원하는 채팅방의 식별자 ID값을 의미합니다.")
                            ),
                            responseFields(
                                    fieldWithPath("succeed").type(JsonFieldType.BOOLEAN).description("요청 성공 여부"),
                                    fieldWithPath("code").type(JsonFieldType.STRING).description("결과 코드"),
                                    fieldWithPath("message").type(JsonFieldType.STRING).description("결과 메시지"),
                                    fieldWithPath("data").type(JsonFieldType.NULL).description("응답 데이터를 나타냅니다.")
                            )
                    ));

            then(chatCommandService).should(times(1)).leaveChatroom(chatroomId);
        }

        @Test
        @DisplayName("사용자가 채팅방 멤버가 아닐 경우 404 Not Found 에러를 반환한다")
        void deleteChatRoom_Fail_NotMember() throws Exception {
            // given
            UUID memberId = UUID.randomUUID();
            // 서비스가 예외를 던지는 상황을 Mocking
            willThrow(new CustomException(ErrorCode.CHATROOM_MEMBERS_NOT_FOUND))
                    .given(chatCommandService).leaveChatroom(chatroomId);

            // when & then
            mockMvc.perform(delete("/api/v1/chat-rooms/{chatroomId}", chatroomId)
                            .headers(getCommonApiHeaders(memberId))
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.succeed").value(false))
                    .andExpect(jsonPath("$.code").value(ErrorCode.CHATROOM_MEMBERS_NOT_FOUND.getCode()));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/chat-rooms/{chatroomId}/ticket API는")
    class IssueTicket {

        private final Long chatroomId = 1L;

        @Test
        @DisplayName("티켓을 성공적으로 생성한다")
        void issueTicket_Success() throws Exception {
            // given
            UUID memberId = UUID.randomUUID();
            TicketResponse ticketResponse = new TicketResponse(UUID.randomUUID());

            given(chatTicketService.issueTicket(eq(chatroomId)))
                    .willReturn(ticketResponse);

            // when & then
            mockMvc.perform(post("/api/v1/chat-rooms/{chatroomId}/ticket", chatroomId)
                            .headers(getCommonApiHeaders(memberId))
                            .contentType(MediaType.APPLICATION_JSON)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpectAll(
                            status().isCreated(),
                            jsonPath("$.succeed").value(true),
                            jsonPath("$.code").value("CREATED")
                    )
                    .andDo(document.document(
                            requestHeaders(HEADER_ACCESS_TOKEN),
                            pathParameters(
                                    parameterWithName("chatroomId").attributes(getTypeFormat(JsonFieldType.NUMBER)).description("입장하기 원하는 채팅방의 식별자 ID값을 의미합니다.")
                            ),
                            responseFields(
                                    fieldWithPath("succeed").type(JsonFieldType.BOOLEAN).description("요청 성공 여부를 의미합니다."),
                                    fieldWithPath("code").type(JsonFieldType.STRING).description("결과 코드를 나타냅니다."),
                                    fieldWithPath("message").type(JsonFieldType.STRING).description("결과 메시지를 의미합니다."),
                                    fieldWithPath("data").type(JsonFieldType.OBJECT).description("응답 데이터를 나타냅니다."),
                                    fieldWithPath("data.chatTicket").type(JsonFieldType.STRING).description("발급이 완료된 채팅 티켓 식별자 (UUID) 정보입니다. 이 값을 가지고 웹 소켓 연결시 파라미터로 사용하면 됩니다.")
                            )
                    ));

            then(chatTicketService).should(times(1)).issueTicket(eq(chatroomId));
        }

        @Test
        @DisplayName("사용자가 채팅방 멤버가 아닐 경우 404 Not Found 를 반환한다")
        void issueTicket_Fail_NotMember() throws Exception {
            // given
            UUID memberId = UUID.randomUUID();

            given(chatTicketService.issueTicket(eq(chatroomId)))
                    .willThrow(new CustomException(ErrorCode.CHATROOM_MEMBERS_NOT_FOUND));

            // when & then
            mockMvc.perform(post("/api/v1/chat-rooms/{chatroomId}/ticket", chatroomId)
                            .headers(getCommonApiHeaders(memberId))
                            .contentType(MediaType.APPLICATION_JSON)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpectAll(
                            status().isNotFound(),
                            jsonPath("$.succeed").value(false),
                            jsonPath("$.code").value(ErrorCode.CHATROOM_MEMBERS_NOT_FOUND.getCode()),
                            jsonPath("$.message").value(ErrorCode.CHATROOM_MEMBERS_NOT_FOUND.getMessage())
                    );

            then(chatTicketService).should(times(1)).issueTicket(eq(chatroomId));
        }
    }
}