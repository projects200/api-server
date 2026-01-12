package com.project200.undabang.common.websocket.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project200.undabang.chat.dto.record.SaveMessageRecord;
import com.project200.undabang.chat.dto.request.ChatMessageRequest;
import com.project200.undabang.chat.dto.response.SaveMessageResponse;
import com.project200.undabang.chat.service.ChatCommandService;
import com.project200.undabang.common.web.exception.CustomException;
import com.project200.undabang.common.web.exception.ErrorCode;
import com.project200.undabang.common.web.response.WebSocketResponse;
import com.project200.undabang.common.web.response.WebSocketType;
import com.project200.undabang.common.websocket.manager.WebSocketSessionManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.FieldError;
import org.springframework.validation.Validator;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Component
public class WebSocketHandler extends TextWebSocketHandler {

    private final ObjectMapper objectMapper;
    private final ChatCommandService chatCommandService;
    private final Validator validator;
    private final WebSocketSessionManager sessionManager;

    /**
     * WebSocket 연결이 성공적으로 설정된 후 호출되는 메서드입니다.
     * 클라이언트의 세션을 관련된 채팅방에 추가하고, 필요한 초기 작업을 수행합니다.
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        WebSocketSession decoratedSession = createWebSocketDecorator(session);

        Long roomId = (Long) decoratedSession.getAttributes().get("roomId");
        UUID memberId = (UUID) decoratedSession.getAttributes().get("memberId");

        // 세션에서 방 번호와 회원 식별자를 확인할 수 없는 경우는 return
        if (roomId == null || memberId == null) {
            decoratedSession.close(CloseStatus.BAD_DATA);
            return;
        }

        // 세션이 없는 경우는 새로 생성해서 Map에 추가 (세션 ID, 세션 장식자)
        sessionManager.registerSession(roomId, decoratedSession);

        log.info("채팅방 {} 에 세션ID {} 입장", roomId, decoratedSession.getId());
    }

    /**
     * WebSocket 연결이 종료된 후 호출되는 메서드입니다.
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        Long roomId = (Long) session.getAttributes().get("roomId");

        if (roomId != null) {
            sessionManager.removeSession(roomId, session.getId());
        }

        log.info("채팅방 {} 에 세션ID {} 퇴장", roomId, session.getId());
    }

    /**
     * WebSocket 텍스트 메시지를 처리하는 메서드입니다.
     * 수신된 메시지를 파싱하여 해당 메시지 타입에 따라 적절한 작업을 수행합니다.
     */
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        try {
            String payload = message.getPayload();
            ChatMessageRequest request = objectMapper.readValue(payload, ChatMessageRequest.class);

            // 메시지 크기 및 요청 검증
            if (!validateRequest(request, session)) {
                return;
            }

            // 하트비트 기능 추가
            if (request.getType() == WebSocketType.PING) {
                WebSocketResponse<String> webSocketResponse = WebSocketResponse.heartbeat();
                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(webSocketResponse)));
                return;
            }

            // 실제 채팅 기능 처리
            if (request.getType() == WebSocketType.TALK) {
                Long chatroomId = (Long) session.getAttributes().get("roomId");
                UUID memberId = (UUID) session.getAttributes().get("memberId");

                if (chatroomId == null || memberId == null) {
                    throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
                }

                SaveMessageRecord record = SaveMessageRecord.of(chatroomId, memberId, request.getContent());

                // 전송받은 데이터 DB에 저장 및 fcm 알림 전송
                SaveMessageResponse response = chatCommandService.saveMessage(record);

                // 응답 객체 생성 및 같은 채팅방의 모든 인원에게 전송
                WebSocketResponse<SaveMessageResponse> webSocketResponse = WebSocketResponse.success(response);
                broadCastToAllChatroom(chatroomId, webSocketResponse);
            }
        } catch (CustomException ce) {
            sendError(session, WebSocketType.ERROR, ce.getErrorCode());
        } catch (JsonProcessingException je) {
            sendError(session, je.getMessage());
        } catch (Exception e) {
            sendError(session, "시스템 오류가 발생했습니다.");
            log.error("웹소켓 통신중 오류 발생.", e);
        }
    }

    /**
     * 지정된 채팅방에 속한 모든 WebSocket 세션에 메시지를 브로드캐스트하는 메서드입니다.
     * 주어진 응답 객체를 JSON으로 변환하여 모든 세션에 전송합니다.
     */
    private void broadCastToAllChatroom(Long roomId, WebSocketResponse<?> response) {
        Map<String, WebSocketSession> sessions = sessionManager.getSessions(roomId);

        if (sessions == null || sessions.isEmpty()) {
            return;
        }

        try {
            String json = objectMapper.writeValueAsString(response);
            TextMessage message = new TextMessage(json);

            for (WebSocketSession session : sessions.values()) {
                if (session.isOpen()) {
                    try {
                        session.sendMessage(message);
                    } catch (IOException e) {
                        log.warn("메시지 전송 실패 (세션ID: {}): {}", session.getId(), e.getMessage());
                    }
                }
            }

        } catch (JsonProcessingException e) {
            log.error("JSON 파싱중 에러 발생", e);
        }
    }

    /**
     * WebSocketSession을 특정 제한 조건과 함께 데코레이트한 ConcurrentWebSocketSessionDecorator 객체를 생성합니다.
     * 전송 제한시간 : 5초
     * 버퍼사이즈 : 20KB (한글 500자 채운 메시지가 8~10개 밀리면 연결 종료)
     */
    private ConcurrentWebSocketSessionDecorator createWebSocketDecorator(WebSocketSession session) {
        return new ConcurrentWebSocketSessionDecorator(session, 5000, 20 * 1024);
    }

    /**
     * 채팅 메시지 WebSocket 요청을 검증하는 메서드입니다.
     * WebSocketType 값의 존재 및 유효성을 확인하고, PING 타입은 콘텐츠 검증 없이 통과시킵니다.
     * 그 외의 경우에는 Bean Validation(Validator)을 통해 요청 메시지(예: content 필드)의 유효성을 검증하고,
     * 유효하지 않은 경우 클라이언트에게 오류 메시지를 전송한 뒤 {@code false} 를 반환합니다.
     */
    private boolean validateRequest(ChatMessageRequest request, WebSocketSession session) {

        // WebSocketType null 체크
        if (request.getType() == null) {
            sendError(session, "WebSocketType이 지정되지 않았습니다.");
            return false;
        }

        // PING 을 확인하는 경우는 CONTENT 내용 검증할 필요 없이 통과하도록 설정
        if (request.getType() == WebSocketType.PING) {
            return true;
        }

        // Bean Validation 수행
        Errors errors = new BeanPropertyBindingResult(request, "chatMessageRequest");
        validator.validate(request, errors);

        // 에러 있으면 예외 발생
        if (errors.hasErrors()) {
            FieldError contentError = errors.getFieldError("content");

            if (contentError != null) {
                String errorMsg = contentError.getDefaultMessage();
                sendError(session, errorMsg);
                return false;
            }
        }

        // 에러가 없거나, content 관련 에러가 아니면 통과
        return true;
    }

    /**
     * WebSocket 세션에 에러 메시지를 전송하는 메서드입니다.
     */
    private void sendError(WebSocketSession session, String errorMessage) {
        try {
            if (session.isOpen()) {
                WebSocketResponse<String> errorResponse = WebSocketResponse.error(errorMessage);
                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(errorResponse)));
            }
        } catch (IOException e) {
            log.warn("웹소켓 오류 메시지 전송 실패 {}: {}", session.getId(), e.getMessage(), e);
        }
    }

    /**
     * WebSocket 세션에 특정 타입과 에러 코드를 포함한 에러 메시지를 전송하는 메서드입니다.
     */
    private void sendError(WebSocketSession session, WebSocketType type, ErrorCode errorCode) {
        try {
            if (session.isOpen()) {
                WebSocketResponse<?> errorResponse = WebSocketResponse.error(type, errorCode);
                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(errorResponse)));
            }
        } catch (IOException e) {
            log.warn("웹소켓 오류 메시지 전송 실패 {}: {}", session.getId(), e.getMessage(), e);
        }
    }
}
