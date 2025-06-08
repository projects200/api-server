package com.project200.undabang.common.web.interceptor;

import com.project200.undabang.common.context.UserContextHolder;
import com.project200.undabang.common.web.exception.CustomException;
import com.project200.undabang.common.web.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.servlet.HandlerInterceptor;

@Slf4j
public class XUserEmailCheckInterceptor implements HandlerInterceptor {
    public static final String USER_EMAIL_HEADER = "X-USER-EMAIL";

    /**
     * HTTP 요청이 처리되기 전에 X-USER-EMAIL 헤더를 검증하고 사용자 이메일 컨텍스트를 설정합니다.
     * @throws CustomException 유효하지 않은 이메일 형식 또는 헤더 누락 시
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String userEmail = request.getHeader(USER_EMAIL_HEADER);
        if (userEmail != null && !userEmail.isEmpty()) {
            try{
                // 이메일 형식 검증은 생략하고 컨텍스트에 저장
                // (필요시 여기에 이메일 형식 검증 로직 추가 가능)
                UserContextHolder.setUserEmail(userEmail);
                log.debug("X-USER-EMAIL 헤더가 설정되었습니다: {}", userEmail);
            }catch (IllegalArgumentException e){
                // 일반적으로 문자열 저장에는 예외가 발생하지 않지만
                // 혹시 모를 상황을 대비해 예외 처리
                log.error("X-USER-EMAIL 헤더 처리 중 오류 발생: {}", userEmail, e);
                throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
            }
        } else {
            log.error("X-USER-EMAIL header가 누락되었습니다: " + request.getRequestURI());
            throw new CustomException(ErrorCode.USER_EMAIL_HEADER_MISSING);
        }
        return true; // 항상 요청 처리 계속 진행
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        // 이미 XUserIdCheckInterceptor에서 reset()을 호출하지만
        // 이 인터셉터를 단독으로 사용할 경우를 대비해 호출
        UserContextHolder.reset();
    }
}
