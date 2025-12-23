package com.project200.undabang.common.websocket.manager;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class WebSocketSessionManager {
    // Key: 채팅방, Value: <SessionID, Session 객체>
    private final Map<Long, Map<String, WebSocketSession>> roomSessionMap = new ConcurrentHashMap<>();

    /**
     * 주어진 방 ID와 WebSocket 세션을 등록합니다.
     * 방 ID에 해당하는 세션 맵이 없을 경우 새롭게 생성하여 등록합니다.
     */
    public void registerSession(Long roomId, WebSocketSession session) {
        roomSessionMap.computeIfAbsent(roomId, k -> new ConcurrentHashMap<>()).put(session.getId(), session);
    }

    /**
     * 지정된 채팅방 ID에 연결된 세션 중 특정 세션을 제거합니다.
     * 세션이 제거된 후 해당 채팅방에 더 이상 세션이 존재하지 않으면
     * 메모리 절약을 위해 채팅방 정보를 삭제합니다.
     */
    public void removeSession(Long roomId, String sessionId) {
        Map<String, WebSocketSession> sessions = roomSessionMap.get(roomId);
        if (sessions != null) {
            sessions.remove(sessionId);

            // 방에 남은 사람이 없으면 방 키 자체를 삭제 (메모리 절약)
            if (sessions.isEmpty()) {
                roomSessionMap.remove(roomId);
            }
        }
    }

    /**
     * 주어진 방 ID(roomId)에 해당하는 WebSocket 세션 맵을 반환합니다.
     */
    public Map<String, WebSocketSession> getSessions(Long roomId) {
        return roomSessionMap.get(roomId);
    }
}
