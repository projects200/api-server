package com.project200.undabang.common.websocket.manager;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.socket.WebSocketSession;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class WebSocketSessionManagerTest {

    private final WebSocketSessionManager sessionManager = new WebSocketSessionManager();

    @Mock
    private WebSocketSession session1;

    @Mock
    private WebSocketSession session2;

    private void mockSessionId(WebSocketSession session, String id) {
        given(session.getId()).willReturn(id);
    }

    @Nested
    @DisplayName("registerSession 메소드는")
    class Describe_registerSession {

        @Test
        @DisplayName("새로운 채팅방 ID로 세션을 등록하면 방을 생성하고 세션을 저장한다")
        void it_creates_room_and_adds_session() {
            // given
            Long roomId = 1L;
            mockSessionId(session1, "session-1");

            // when
            sessionManager.registerSession(roomId, session1);

            // then
            Map<String, WebSocketSession> sessions = sessionManager.getSessions(roomId);
            assertThat(sessions).isNotNull();
            assertThat(sessions).hasSize(1);
            assertThat(sessions.get("session-1")).isEqualTo(session1);
        }

        @Test
        @DisplayName("이미 존재하는 채팅방에 세션을 추가하면 해당 방에 세션을 추가한다")
        void it_adds_session_to_existing_room() {
            // given
            Long roomId = 1L;
            mockSessionId(session1, "session-1");
            mockSessionId(session2, "session-2");

            // 먼저 하나 등록해서 방을 만듬
            sessionManager.registerSession(roomId, session1);

            // when
            sessionManager.registerSession(roomId, session2);

            // then
            Map<String, WebSocketSession> sessions = sessionManager.getSessions(roomId);
            assertThat(sessions).hasSize(2);
            assertThat(sessions).containsValues(session1, session2);
        }
    }

    @Nested
    @DisplayName("removeSession 메소드는")
    class Describe_removeSession {

        @Test
        @DisplayName("채팅방에 여러 세션이 있을 때 하나를 제거하면 해당 세션만 사라진다")
        void it_removes_specific_session() {
            // given
            Long roomId = 1L;
            mockSessionId(session1, "session-1");
            mockSessionId(session2, "session-2");

            sessionManager.registerSession(roomId, session1);
            sessionManager.registerSession(roomId, session2);

            // when
            sessionManager.removeSession(roomId, "session-1");

            // then
            Map<String, WebSocketSession> sessions = sessionManager.getSessions(roomId);
            assertThat(sessions).isNotNull();
            assertThat(sessions).hasSize(1);
            assertThat(sessions.get("session-2")).isEqualTo(session2);
        }

        @Test
        @DisplayName("방에 남은 마지막 세션을 제거하면 방 정보(Key) 자체가 삭제된다 (메모리 절약)")
        void it_removes_room_entry_when_last_session_leaves() {
            // given
            Long roomId = 1L;
            mockSessionId(session1, "session-1");
            sessionManager.registerSession(roomId, session1);

            // when
            sessionManager.removeSession(roomId, "session-1");

            // then
            // 방 자체가 맵에서 삭제되어 null이 반환되어야 함
            assertThat(sessionManager.getSessions(roomId)).isNull();
        }

        @Test
        @DisplayName("존재하지 않는 방이나 세션 ID로 삭제를 시도해도 에러가 발생하지 않는다")
        void it_does_nothing_when_target_not_found() {
            // given
            Long roomId = 999L; // 없는 방

            // when & then (예외 미발생 검증)
            sessionManager.removeSession(roomId, "unknown-session");

            // 검증: 여전히 null이어야 함
            assertThat(sessionManager.getSessions(roomId)).isNull();
        }
    }

    @Nested
    @DisplayName("getSessions 메소드는")
    class Describe_getSessions {

        @Test
        @DisplayName("존재하는 방 ID를 조회하면 세션 맵을 반환한다")
        void it_returns_session_map() {
            // given
            Long roomId = 1L;
            mockSessionId(session1, "session-1");
            sessionManager.registerSession(roomId, session1);

            // when
            Map<String, WebSocketSession> result = sessionManager.getSessions(roomId);

            // then
            assertThat(result).containsKey("session-1");
        }

        @Test
        @DisplayName("존재하지 않는 방 ID를 조회하면 null을 반환한다")
        void it_returns_null_when_room_not_found() {
            // given
            Long nonExistentRoomId = 999L;

            // when
            Map<String, WebSocketSession> result = sessionManager.getSessions(nonExistentRoomId);

            // then
            assertThat(result).isNull();
        }
    }
}