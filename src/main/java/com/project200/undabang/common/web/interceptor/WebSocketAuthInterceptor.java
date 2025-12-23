package com.project200.undabang.common.web.interceptor;


import com.project200.undabang.chat.entity.TicketInfoRecord;
import com.project200.undabang.chat.service.ChatTicketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketAuthInterceptor implements HandshakeInterceptor {

    private final ChatTicketService chatTicketService;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {

        if (request instanceof ServletServerHttpRequest servletRequest) {

            // url에서 티켓 파라미터 가져오기
            String chatTicket = servletRequest.getServletRequest().getParameter("chatTicket");

            if (chatTicket == null || chatTicket.isBlank()) {
                return false;
            }

            try {
                // 입력받은 티켓이 caffeine cache 내부에 있는 티켓인지 확인
                // 티켓이 존재하면 cache에서 해당 티켓 정보는 삭제한다.
                TicketInfoRecord info = chatTicketService.validateTicket(UUID.fromString(chatTicket));

                if (info != null) {
                    attributes.put("memberId", info.memberId());
                    attributes.put("roomId", info.roomId());
                    log.info("[WebSocket] 웹소켓 연결이 승인되었습니다. member : {}, roomId : {}", info.memberId(), info.roomId());

                    return true;
                }
            } catch (IllegalArgumentException e) {
                log.warn("[WebSocket] 잘못된 티켓 형식입니다.", e);
            }

            return false;
        }

        return false;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Exception exception) {

    }
}
