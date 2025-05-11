package com.project200.undabang.common.web.interceptor;

import com.project200.undabang.common.context.UserContextHolder;
import com.project200.undabang.common.web.exception.CustomException;
import com.project200.undabang.common.web.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.UUID;

/**
 * HTTP 요청의 X-USER-ID 헤더를 검증하고 사용자 컨텍스트를 설정하는 인터셉터
 * <p>
 * 이 인터셉터는 모든 요청에서 X-USER-ID 헤더를 확인하여:
 * <ul>
 *   <li>헤더가 존재하는지 검사합니다</li>
 *   <li>헤더 값이 유효한 UUID 형식인지 검증합니다</li>
 *   <li>유효한 경우 {@link UserContextHolder}에 사용자 ID를 설정합니다</li>
 * </ul>
 * 요청 처리가 완료된 후에는 사용자 컨텍스트를 자동으로 초기화합니다.
 */

@Slf4j
public class XUserIdCheckInterceptor implements HandlerInterceptor {

    /** X-USER-ID 헤더 상수 */
    private static final String USER_ID_HEADER = "X-USER-ID";

    /**
     * HTTP 요청이 처리되기 전에 X-USER-ID 헤더를 검증하고 사용자 컨텍스트를 설정합니다.
     *
     * @param request 현재 HTTP 요청
     * @param response 현재 HTTP 응답
     * @param handler 요청을 처리할 핸들러
     * @return 요청 처리를 계속 진행할지 여부 (true: 계속 진행, false: 중단)
     * @throws Exception 예외 발생 시
     * @throws CustomException 유효하지 않은 사용자 ID 형식 또는 헤더 누락 시
     */
    @Override
    public boolean preHandle(final HttpServletRequest request, final HttpServletResponse response, final Object handler) throws Exception {
        String userIdString = request.getHeader(USER_ID_HEADER);
        if (userIdString != null && !userIdString.isEmpty()) {
            try {
                UUID userId = UUID.fromString(userIdString);
                UserContextHolder.setUserId(userId);
            } catch (IllegalArgumentException e) {
                // UUID 형식이 잘못된 경우 로깅 또는 에러 처리
                log.error("X-USER-ID header가 유효하지 않은 UUID 형식입니다: " + userIdString, e);
                throw new CustomException(ErrorCode.INVALID_USER_ID_FORMAT);
            }
        } else {
            log.error("X-USER-ID 헤더가 누락되었습니다: " + request.getRequestURI());
            throw new CustomException(ErrorCode.USER_ID_HEADER_MISSING);
        }
        return true; // 계속해서 요청 처리 진행
    }

    /**
     * 요청 처리가 완료된 후 사용자 컨텍스트를 초기화합니다.
     *
     * @param request 현재 HTTP 요청
     * @param response 현재 HTTP 응답
     * @param handler 요청을 처리한 핸들러
     * @param ex 처리 중 발생한 예외 (있는 경우)
     * @throws Exception 예외 발생 시
     */
    @Override
    public void afterCompletion(final HttpServletRequest request, final HttpServletResponse response, final Object handler, final Exception ex) throws Exception {
        UserContextHolder.reset();
    }
}