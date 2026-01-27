package com.project200.undabang.common.websocket.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.project200.undabang.chat.dto.request.ChatMessageRequest;
import com.project200.undabang.chat.dto.response.SaveMessageResponse;
import com.project200.undabang.chat.service.ChatCommandService;
import com.project200.undabang.common.web.exception.CustomException;
import com.project200.undabang.common.web.exception.ErrorCode;
import com.project200.undabang.common.web.response.WebSocketResponse;
import com.project200.undabang.common.web.response.WebSocketType;
import com.project200.undabang.common.websocket.manager.WebSocketSessionManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebSocketHandlerTest {

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private WebSocketHandler webSocketHandler;
    @Mock
    private ChatCommandService chatCommandService;

    @Mock
    private Validator validator;

    @Mock
    private WebSocketSessionManager sessionManager;

    @Mock
    private WebSocketSession session;

    private void setupHandler() {
        this.webSocketHandler = new WebSocketHandler(
                objectMapper,
                chatCommandService,
                validator,
                sessionManager
        );
    }

    private void mockSessionAttributes(Long roomId, UUID memberId) {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("roomId", roomId);
        attributes.put("memberId", memberId);
        given(session.getAttributes()).willReturn(attributes);
    }

    private String createPayload(WebSocketType type, String content) throws Exception {
        ChatMessageRequest request = new ChatMessageRequest(type, content);
        return objectMapper.writeValueAsString(request);
    }

    private SaveMessageResponse createSaveMessageResponse(UUID memberId, String content) {
        return SaveMessageResponse.builder()
                .chatId(100L)
                .chatContent(content)
                .senderId(memberId)
                .sentAt(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("afterConnectionEstablished 메소드는")
    class Describe_afterConnectionEstablished {

        @Test
        @DisplayName("세션에 방 정보와 회원 정보가 있으면 세션 매니저에 등록한다")
        void it_registers_session_when_attributes_exist() throws Exception {
            // given
            setupHandler();
            mockSessionAttributes(1L, UUID.randomUUID());
            given(session.getId()).willReturn("session-1");

            // when
            webSocketHandler.afterConnectionEstablished(session);

            // then
            verify(sessionManager).registerSession(eq(1L), any(WebSocketSession.class));
        }

        @Test
        @DisplayName("세션에 방 정보가 없으면 연결을 종료한다")
        void it_closes_session_when_attributes_missing() throws Exception {
            // given
            setupHandler();
            given(session.getAttributes()).willReturn(new HashMap<>());

            // when
            webSocketHandler.afterConnectionEstablished(session);

            // then
            verify(session).close(CloseStatus.BAD_DATA);
            verify(sessionManager, never()).registerSession(any(), any());
        }
    }

    @Nested
    @DisplayName("afterConnectionClosed 메소드는")
    class Describe_afterConnectionClosed {

        @Test
        @DisplayName("연결이 종료되면 세션 매니저에서 세션을 제거한다")
        void it_removes_session_from_manager() throws Exception {
            // given
            setupHandler();
            mockSessionAttributes(1L, UUID.randomUUID());
            given(session.getId()).willReturn("session-1");

            // when
            webSocketHandler.afterConnectionClosed(session, CloseStatus.NORMAL);

            // then
            verify(sessionManager).removeSession(1L, "session-1");
        }
    }

    @Nested
    @DisplayName("handleTextMessage 메소드는")
    class Describe_handleTextMessage {

        @Nested
        @DisplayName("요청 타입이 PING일 때")
        class Context_with_ping_type {

            @Test
            @DisplayName("PONG 응답을 반환한다")
            void it_returns_pong_response() throws Exception {
                // given
                setupHandler();

                String payload = createPayload(WebSocketType.PING, null);
                TextMessage message = new TextMessage(payload);

                // when
                webSocketHandler.handleTextMessage(session, message);

                // then
                ArgumentCaptor<TextMessage> messageCaptor = ArgumentCaptor.forClass(TextMessage.class);
                verify(session).sendMessage(messageCaptor.capture());

                WebSocketResponse response = objectMapper.readValue(messageCaptor.getValue().getPayload(), WebSocketResponse.class);
                assertThat(response.isSucceed()).isTrue();
                assertThat(response.getType()).isEqualTo(WebSocketType.PONG);
            }
        }

        @Nested
        @DisplayName("요청 타입이 TALK일 때")
        class Context_with_talk_type {

            private final Long chatroomId = 1L;
            private final UUID memberId = UUID.randomUUID();
            private final String content = "안녕하세요";

            @Test
            @DisplayName("정상 요청이면 메시지를 저장하고 채팅방 전체에 브로드캐스트한다")
            void it_saves_and_broadcasts_message() throws Exception {
                // given
                setupHandler();
                mockSessionAttributes(chatroomId, memberId);
                given(session.isOpen()).willReturn(true);
                given(sessionManager.getSessions(chatroomId)).willReturn(Map.of("session-1", session));

                String payload = createPayload(WebSocketType.TALK, content);
                TextMessage message = new TextMessage(payload);

                SaveMessageResponse serviceResponse = createSaveMessageResponse(memberId, content);
                given(chatCommandService.saveMessage(any())).willReturn(serviceResponse);

                // when
                webSocketHandler.handleTextMessage(session, message);

                // then
                then(chatCommandService).should(times(1)).saveMessage(any());

                ArgumentCaptor<TextMessage> messageCaptor = ArgumentCaptor.forClass(TextMessage.class);
                verify(session).sendMessage(messageCaptor.capture());

                String sentJson = messageCaptor.getValue().getPayload();
                WebSocketResponse response = objectMapper.readValue(sentJson, WebSocketResponse.class);

                assertThat(response.isSucceed()).isTrue();
                assertThat(response.getType()).isEqualTo(WebSocketType.TALK);
            }

            @Test
            @DisplayName("Validator 검증에 실패하면 에러 메시지를 보낸다")
            void it_returns_error_when_validation_fails() throws Exception {
                // given
                setupHandler();
                given(session.isOpen()).willReturn(true);
                String payload = createPayload(WebSocketType.TALK, "");
                TextMessage message = new TextMessage(payload);

                doAnswer(invocation -> {
                    Errors errors = invocation.getArgument(1);
                    errors.rejectValue("content", "empty", "내용을 입력해주세요");
                    return null;
                }).when(validator).validate(any(), any());

                // when
                webSocketHandler.handleTextMessage(session, message);

                // then
                ArgumentCaptor<TextMessage> messageCaptor = ArgumentCaptor.forClass(TextMessage.class);
                verify(session).sendMessage(messageCaptor.capture());

                Map<String, Object> response = objectMapper.readValue(messageCaptor.getValue().getPayload(), Map.class);

                assertThat(response.get("succeed")).isEqualTo(false);
                assertThat(response.get("type")).isEqualTo("ERROR");
                assertThat(response.get("message")).isEqualTo(null);
                assertThat(response.get("data")).isEqualTo("내용을 입력해주세요");
            }

            @Test
            @DisplayName("차단된 사용자가 메시지를 보내면 에러 코드(BLOCKED)를 반환한다")
            void it_returns_blocked_error_code() throws Exception {
                // given
                setupHandler();
                mockSessionAttributes(chatroomId, memberId);
                given(session.isOpen()).willReturn(true);

                String payload = createPayload(WebSocketType.TALK, content);
                TextMessage message = new TextMessage(payload);

                given(chatCommandService.saveMessage(any()))
                        .willThrow(new CustomException(ErrorCode.MESSAGE_SEND_BLOCKED));

                // when
                webSocketHandler.handleTextMessage(session, message);

                // then
                ArgumentCaptor<TextMessage> messageCaptor = ArgumentCaptor.forClass(TextMessage.class);
                verify(session).sendMessage(messageCaptor.capture());

                String json = messageCaptor.getValue().getPayload();
                Map<String, Object> response = objectMapper.readValue(json, Map.class);

                assertThat(response.get("succeed")).isEqualTo(false);
                assertThat(response.get("type")).isEqualTo("ERROR");
                assertThat(response.get("message")).isEqualTo(ErrorCode.MESSAGE_SEND_BLOCKED.getCode());
                assertThat(response.get("data")).isEqualTo(ErrorCode.MESSAGE_SEND_BLOCKED.getMessage());
            }
        }
    }

    @Nested
    @DisplayName("엣지 케이스 및 예외 처리 테스트")
    class Describe_EdgeCases {

        @Test
        @DisplayName("JSON 형식이 잘못된 경우 파싱 에러를 반환한다")
        void it_returns_error_when_json_is_broken() throws Exception {
            // given
            setupHandler();
            given(session.isOpen()).willReturn(true);
            String brokenJson = "{ \"type\": \"TALK\", \"content\": ... "; // 닫히지 않은 JSON
            TextMessage message = new TextMessage(brokenJson);

            // when
            webSocketHandler.handleTextMessage(session, message);

            // then
            ArgumentCaptor<TextMessage> messageCaptor = ArgumentCaptor.forClass(TextMessage.class);
            verify(session).sendMessage(messageCaptor.capture());

            Map<String, Object> response = objectMapper.readValue(messageCaptor.getValue().getPayload(), Map.class);

            assertThat(response.get("succeed")).isEqualTo(false);
            // ObjectMapper가 던지는 에러 메시지가 data에 담김
            assertThat(response.get("data").toString()).contains("JSON");
        }

        @Test
        @DisplayName("요청에 type 필드가 없으면 에러를 반환한다")
        void it_returns_error_when_type_is_missing() throws Exception {
            // given
            setupHandler();
            given(session.isOpen()).willReturn(true);
            // type 없이 content만 있는 JSON
            String json = "{\"content\": \"유령 메시지\"}";
            TextMessage message = new TextMessage(json);

            // when
            webSocketHandler.handleTextMessage(session, message);

            // then
            ArgumentCaptor<TextMessage> messageCaptor = ArgumentCaptor.forClass(TextMessage.class);
            verify(session).sendMessage(messageCaptor.capture());

            Map<String, Object> response = objectMapper.readValue(messageCaptor.getValue().getPayload(), Map.class);
            assertThat(response.get("succeed")).isEqualTo(false);
            assertThat(response.get("data")).isEqualTo("WebSocketType이 지정되지 않았습니다.");
        }

        @Test
        @DisplayName("TALK 요청 시 세션 속성(roomId, memberId)이 없으면 에러를 반환한다")
        void it_returns_error_when_session_attributes_missing_on_talk() throws Exception {
            // given
            setupHandler();
            given(session.isOpen()).willReturn(true);

            // 세션 속성을 빈 맵으로 설정 (로그인 정보 유실 시뮬레이션)
            given(session.getAttributes()).willReturn(new HashMap<>());

            String payload = createPayload(WebSocketType.TALK, "테스트");
            TextMessage message = new TextMessage(payload);

            // when
            webSocketHandler.handleTextMessage(session, message);

            // then
            ArgumentCaptor<TextMessage> messageCaptor = ArgumentCaptor.forClass(TextMessage.class);
            verify(session).sendMessage(messageCaptor.capture());

            Map<String, Object> response = objectMapper.readValue(messageCaptor.getValue().getPayload(), Map.class);

            assertThat(response.get("succeed")).isEqualTo(false);
            // ErrorCode.INVALID_INPUT_VALUE가 처리됨
            assertThat(response.get("message")).isEqualTo(ErrorCode.INVALID_INPUT_VALUE.getCode());
        }

        @Test
        @DisplayName("알 수 없는 시스템 오류 발생 시 공통 에러 메시지를 반환한다")
        void it_returns_system_error_on_unexpected_exception() throws Exception {
            // given
            setupHandler();
            mockSessionAttributes(1L, UUID.randomUUID());
            given(session.isOpen()).willReturn(true);

            String payload = createPayload(WebSocketType.TALK, "테스트");
            TextMessage message = new TextMessage(payload);

            // 서비스에서 RuntimeException 강제 발생
            given(chatCommandService.saveMessage(any())).willThrow(new RuntimeException("DB Connection Died"));

            // when
            webSocketHandler.handleTextMessage(session, message);

            // then
            ArgumentCaptor<TextMessage> messageCaptor = ArgumentCaptor.forClass(TextMessage.class);
            verify(session).sendMessage(messageCaptor.capture());

            Map<String, Object> response = objectMapper.readValue(messageCaptor.getValue().getPayload(), Map.class);

            assertThat(response.get("succeed")).isEqualTo(false);
            assertThat(response.get("data")).isEqualTo("시스템 오류가 발생했습니다.");
        }

        @Test
        @DisplayName("브로드캐스트 대상 세션이 없어도 에러 없이 종료된다")
        void it_does_nothing_when_no_sessions_found() throws Exception {
            // given
            setupHandler();
            mockSessionAttributes(1L, UUID.randomUUID());

            // 세션 매니저가 null 또는 빈 맵을 반환
            given(sessionManager.getSessions(1L)).willReturn(null);

            String payload = createPayload(WebSocketType.TALK, "혼잣말");
            TextMessage message = new TextMessage(payload);

            given(chatCommandService.saveMessage(any())).willReturn(SaveMessageResponse.builder().build());

            // when
            // 예외가 발생하지 않아야 함
            webSocketHandler.handleTextMessage(session, message);

            // then
            then(chatCommandService).should(times(1)).saveMessage(any());
            // 세션 매니저 조회는 했으나 전송은 안 함
            verify(session, never()).sendMessage(any());
            // 주의: 위 verify는 '브로드캐스트' 로직에서 전송이 안 된걸 확인하려는 의도지만,
            // 현재 코드 구조상 handleTextMessage 초입에서 검증용 sendMessage 등이 호출될 수 있어서
            // 정확히는 'sessionManager.getSessions' 호출 이후 로직이 안전한지 확인하는 목적임.
        }

        @Test
        @DisplayName("브로드캐스트 중 특정 세션 전송 실패(IOException) 시 로그를 남기고 계속 진행한다")
        void it_logs_error_when_broadcast_fails_io_exception() throws Exception {
            // given
            setupHandler();
            mockSessionAttributes(1L, UUID.randomUUID());

            // 다른 사용자 세션 (수신자)
            WebSocketSession receiverSession = mock(WebSocketSession.class);
            given(receiverSession.isOpen()).willReturn(true);
            given(receiverSession.getId()).willReturn("receiver-1");

            // ★ 핵심: 수신자에게 보낼 때 IOException 발생 시뮬레이션
            doThrow(new java.io.IOException("Network Broken")).when(receiverSession).sendMessage(any());

            // 세션 매니저에 수신자 포함
            given(sessionManager.getSessions(1L)).willReturn(Map.of("receiver-1", receiverSession));

            String payload = createPayload(WebSocketType.TALK, "전송");
            TextMessage message = new TextMessage(payload);

            given(chatCommandService.saveMessage(any())).willReturn(SaveMessageResponse.builder().build());

            // when
            webSocketHandler.handleTextMessage(session, message);

            // then
            // 예외가 밖으로 던져지지 않고, 로그만 찍고 정상 종료되어야 함
            verify(receiverSession).sendMessage(any());
        }
    }
}