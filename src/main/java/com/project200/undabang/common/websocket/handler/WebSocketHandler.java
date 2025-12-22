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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

@Slf4j
@RequiredArgsConstructor
@Component
public class WebSocketHandler extends TextWebSocketHandler {

    private final ObjectMapper objectMapper;
    private final ChatCommandService chatCommandService;
    private final Validator validator;

    // <방 번호, 세션 집합>의 Key Value로 저장
    private final ConcurrentHashMap<Long, Set<WebSocketSession>> roomSessions = new ConcurrentHashMap<>();

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

        // 세션이 없는 경우는 새로 생성해서 Set에 추가해주고, 있는 경우는 바로 추가함
        roomSessions.computeIfAbsent(roomId, k -> new CopyOnWriteArraySet<>()).add(decoratedSession);

        log.info("채팅방 {} 에 세션ID {} 입장", roomId, decoratedSession.getId());
    }

    /**
     * WebSocket 연결이 종료된 후 호출되는 메서드입니다.
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        Long roomId = (Long) session.getAttributes().get("roomId");

        if (roomId != null) {
            removeSession(session, roomId);
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

            // 메시지 크기 검증
            if (!validateChatContentSize(request, session)) {
                return;
            }

            // 하트비트 기능 추가
            if (request.getWebSocketType() == WebSocketType.PING) {
                WebSocketResponse<String> webSocketResponse = WebSocketResponse.heartbeat();
                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(webSocketResponse)));
                return;
            }

            // 실제 TALK 기능
            if (request.getWebSocketType() == WebSocketType.TALK) {
                Long chatroomId = (Long) session.getAttributes().get("roomId");
                UUID memberId = (UUID) session.getAttributes().get("memberId");

                if (chatroomId == null || memberId == null) {
                    throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
                }

                SaveMessageRecord record = SaveMessageRecord.of(chatroomId, memberId, request.getContent());

                // 전송받은 데이터 DB에 저장 및 fcm 알림 전송
                SaveMessageResponse response = chatCommandService.saveMessage(record);
                WebSocketResponse<SaveMessageResponse> webSocketResponse = WebSocketResponse.success(response);

                broadCastToAllChatroom(chatroomId, webSocketResponse);
            }
        } catch (CustomException ce) {
            sendError(session, ce.getErrorCode().getMessage());
        } catch (Exception e) {
            log.error("웹소켓 통신중 오류 발생.", e);
            sendError(session, "시스템 오류가 발생했습니다.");
        }
    }

    /**
     * 지정된 채팅방에 속한 모든 WebSocket 세션에 메시지를 브로드캐스트하는 메서드입니다.
     * 주어진 응답 객체를 JSON으로 변환하여 모든 세션에 전송합니다.
     */
    private void broadCastToAllChatroom(Long roomId, WebSocketResponse<?> response) {
        Set<WebSocketSession> sessions = roomSessions.get(roomId);

        if (sessions == null || sessions.isEmpty()) {
            return;
        }

        try {
            String json = objectMapper.writeValueAsString(response);
            TextMessage message = new TextMessage(json);

            for (WebSocketSession session : sessions) {
                if (session.isOpen()) {
                    try {
                        session.sendMessage(message);
                    } catch (IOException e) {
                        log.warn("각 기기별로 메시지 전송중 오류 발생", e);
                    }
                }
            }

        } catch (JsonProcessingException e) {
            log.error("JSON 파싱중 에러 발생");
        }
    }

    /**
     * WebSocket 세션에 에러 메시지를 전송하는 메서드입니다.
     */
    private void sendError(WebSocketSession session, String errorMessage) {
        try {
            WebSocketResponse<String> errorResponse = WebSocketResponse.error(errorMessage);

            String json = objectMapper.writeValueAsString(errorResponse);
            session.sendMessage(new TextMessage(json));
        } catch (IOException ignored) {

        }
    }

    /**
     * 주어진 WebSocketSession을 특정 채팅방에서 제거하는 메서드입니다.
     * 채팅방에 남아 있는 세션이 없을 경우, 해당 채팅방 정보를 메모리에서 삭제합니다.
     */
    private void removeSession(WebSocketSession session, Long roomId) {
        Set<WebSocketSession> sessions = roomSessions.get(roomId);

        if (sessions != null) {
            sessions.removeIf(s -> s.getId().equals(session.getId())); // ID가 같은 세션을 지워줌

            log.info("[채팅방 퇴장] 채팅방 : {}, Session Id : {}", roomId, session.getId());

            if (sessions.isEmpty()) {
                roomSessions.remove(roomId); // value에 남은 세션값이 없으면 메모리에서 채팅방 제거
            }
        }
    }

    /**
     * WebSocketSession을 특정 제한 조건과 함께 데코레이트한 ConcurrentWebSocketSessionDecorator 객체를 생성합니다.
     * 전송 제한시간 : 5초
     * 버퍼사이즈 : 16KB (한글 500자 채운 메시지가 8~10개 밀리면 연결 종료)
     */
    private ConcurrentWebSocketSessionDecorator createWebSocketDecorator(WebSocketSession session) {
        return new ConcurrentWebSocketSessionDecorator(session, 5000, 20 * 1024);
    }

    /**
     * 채팅 메시지의 콘텐츠 크기를 검증하는 메서드입니다.
     * 요청된 메시지 내용이 허용된 최대 크기를 초과하는지 확인합니다.
     */
    private boolean validateChatContentSize(ChatMessageRequest request, WebSocketSession session) {
        Errors errors = new BeanPropertyBindingResult(request, "chatMessageRequest");
        validator.validate(request, errors);

        // 에러 있으면 예외 발생
        if (errors.hasErrors()) {
            String errorMsg = errors.getFieldError("content").getDefaultMessage();
            sendError(session, errorMsg);
            return false;
        }

        return true;
    }
}
