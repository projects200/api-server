package com.project200.undabang.common.web.interceptor;

import com.project200.undabang.common.context.UserContextHolder;
import com.project200.undabang.common.web.exception.CustomException;
import com.project200.undabang.common.web.exception.ErrorCode;
import com.project200.undabang.member.entity.Member;
import com.project200.undabang.member.repository.MemberRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.Nullable;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Optional;
import java.util.UUID;

/**
 * HTTP 요청의 X-USER-ID / X-USER-EMAIL 헤더를 검증하고 사용자 컨텍스트를 설정하는 인터셉터
 * <p>
 * 이 인터셉터는 모든 요청에서 헤더를 확인하여:
 * <ul>
 *   <li>X-USER-EMAIL 헤더가 존재하면 이메일로 DB 조회하여 member_id를 컨텍스트에 설정합니다 (우선)</li>
 *   <li>없거나 조회 실패 시 X-USER-ID(sub) 헤더가 유효한 UUID 형식인지 검증합니다</li>
 *   <li>유효한 경우 {@link UserContextHolder}에 사용자 ID를 설정합니다</li>
 * </ul>
 * Cognito 사용자 풀 이전 시 sub 값이 바뀌어도 이메일로 회원을 찾을 수 있게 하기 위함입니다.
 * 요청 처리가 완료된 후에는 사용자 컨텍스트를 자동으로 초기화합니다.
 */

@Slf4j
@RequiredArgsConstructor
public class XUserIdCheckInterceptor implements HandlerInterceptor {

    /** X-USER-ID 헤더 상수 */
    private static final String USER_ID_HEADER = "X-USER-ID";

    /** X-USER-EMAIL 헤더 상수 */
    private static final String USER_EMAIL_HEADER = "X-USER-EMAIL";

    /**
     * MemberRepository 는 @WebMvcTest 등 JPA가 없는 슬라이스 테스트 컨텍스트에서는 null 일 수 있으며,
     * 이 경우 email 기반 조회는 생략되고 기존 X-USER-ID(sub) fallback 만 수행됩니다.
     */
    @Nullable
    private final MemberRepository memberRepository;

    /**
     * HTTP 요청이 처리되기 전에 X-USER-EMAIL / X-USER-ID 헤더를 검증하고 사용자 컨텍스트를 설정합니다.
     * <p>
     * 우선순위:
     * <ol>
     *   <li>X-USER-EMAIL 로 DB 조회 성공 → member_id를 컨텍스트에 설정 (Cognito 이전 대응)</li>
     *   <li>그 외 X-USER-ID(sub)를 UUID로 파싱해서 컨텍스트에 설정 (기존 방식)</li>
     * </ol>
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
        String userEmail = request.getHeader(USER_EMAIL_HEADER);
        String userIdString = request.getHeader(USER_ID_HEADER);

        // 우선순위 1: email로 DB 조회 (Cognito 이전 후 sub 값이 바뀌어도 동작)
        // memberRepository 가 주입되지 않은 테스트 컨텍스트에서는 건너뛰고 X-USER-ID fallback 사용
        if (memberRepository != null && userEmail != null && !userEmail.isEmpty()) {
            Optional<Member> memberOpt = memberRepository.findByMemberEmailAndMemberDeletedAtNull(userEmail);
            if (memberOpt.isPresent()) {
                UserContextHolder.setUserId(memberOpt.get().getMemberId());
                UserContextHolder.setUserEmail(userEmail);
                return true;
            }
            log.debug("X-USER-EMAIL로 회원을 찾을 수 없어 X-USER-ID로 fallback: {}", userEmail);
        }

        // 우선순위 2: sub(X-USER-ID) 기반 (기존 방식 — 신규 가입 직후 혹은 email 매핑 헤더가 없는 경우)
        if (userIdString != null && !userIdString.isEmpty()) {
            try {
                UUID userId = UUID.fromString(userIdString);
                UserContextHolder.setUserId(userId);
                if (userEmail != null && !userEmail.isEmpty()) {
                    UserContextHolder.setUserEmail(userEmail);
                }
                return true;
            } catch (IllegalArgumentException e) {
                // UUID 형식이 잘못된 경우 로깅 또는 에러 처리
                log.error("X-USER-ID header가 유효하지 않은 UUID 형식입니다: {}", userIdString, e);
                throw new CustomException(ErrorCode.INVALID_USER_ID_FORMAT);
            }
        }

        // X-USER-ID 가 없어 컨텍스트를 설정할 수 없음
        // (X-USER-EMAIL 이 있었지만 회원을 찾지 못한 케이스도 여기 포함 — 가입 전 유저일 수 있음)
        if (userEmail != null && !userEmail.isEmpty()) {
            log.error("X-USER-EMAIL은 있으나 회원 조회 실패 + X-USER-ID 누락: uri={}, email={}",
                    request.getRequestURI(), userEmail);
        } else {
            log.error("X-USER-ID / X-USER-EMAIL 헤더가 모두 누락되었습니다: {}", request.getRequestURI());
        }
        throw new CustomException(ErrorCode.USER_ID_HEADER_MISSING);
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
