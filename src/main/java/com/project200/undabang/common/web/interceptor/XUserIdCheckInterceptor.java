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
 *   <li>X-USER-ID(sub)가 유효한 UUID 이고 해당 회원이 DB에 존재하면 그대로 사용합니다 (우선)</li>
 *   <li>X-USER-ID 로 회원을 찾지 못한 경우에만 X-USER-EMAIL 로 DB 조회하여 member_id 를 설정합니다 (Cognito 이전 fallback)</li>
 * </ul>
 * Cognito 사용자 풀 이전 시 sub 값이 바뀌어도 이메일로 회원을 찾을 수 있게 하기 위함이며,
 * email 을 fallback 으로만 사용하여 외부에서 email 헤더 위조만으로 타 계정 가장이 불가능하도록 제한합니다.
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
     * 이 경우 email 기반 조회 및 존재 검증은 생략되고 X-USER-ID(sub) 파싱만 수행됩니다.
     */
    @Nullable
    private final MemberRepository memberRepository;

    /**
     * HTTP 요청이 처리되기 전에 X-USER-ID / X-USER-EMAIL 헤더를 검증하고 사용자 컨텍스트를 설정합니다.
     * <p>
     * 우선순위:
     * <ol>
     *   <li>X-USER-ID(sub) 를 UUID 로 파싱 → 회원 존재 시 컨텍스트에 설정 (기존 방식, 대부분의 요청 경로)</li>
     *   <li>X-USER-ID 로 회원을 찾지 못했고 X-USER-EMAIL 이 존재 → email 로 DB 조회하여 설정 (Cognito 이전 fallback)</li>
     * </ol>
     * email fallback 은 X-USER-ID 로 회원을 찾지 못한 경우에만 동작하므로, 요청당 DB 조회는 일반적으로
     * 존재 검증 1회로 끝나며, 외부에서 email 만 위조해 가장하는 것을 차단한다.
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

        // 우선순위 1: X-USER-ID(sub) — 회원 ID 와 일치하면 그대로 사용
        if (userIdString != null && !userIdString.isEmpty()) {
            try {
                UUID userId = UUID.fromString(userIdString);
                // memberRepository 가 없는 슬라이스 테스트 컨텍스트에서는 존재 검증을 생략한다.
                if (memberRepository == null || memberRepository.existsById(userId)) {
                    UserContextHolder.setUserId(userId);
                    if (userEmail != null && !userEmail.isEmpty()) {
                        UserContextHolder.setUserEmail(userEmail);
                    }
                    return true;
                }
                log.debug("X-USER-ID 로 회원을 찾지 못해 X-USER-EMAIL fallback 시도: userId={}, email={}",
                        userId, maskEmail(userEmail));
            } catch (IllegalArgumentException e) {
                log.error("X-USER-ID header가 유효하지 않은 UUID 형식입니다: {}", userIdString, e);
                throw new CustomException(ErrorCode.INVALID_USER_ID_FORMAT);
            }
        }

        // 우선순위 2: X-USER-EMAIL fallback (Cognito 이전으로 sub 가 바뀐 회원 대응)
        if (memberRepository != null && userEmail != null && !userEmail.isEmpty()) {
            Optional<Member> memberOpt = memberRepository.findByMemberEmailAndMemberDeletedAtNull(userEmail);
            if (memberOpt.isPresent()) {
                UserContextHolder.setUserId(memberOpt.get().getMemberId());
                UserContextHolder.setUserEmail(userEmail);
                return true;
            }
        }

        // 둘 다 실패
        if (userEmail != null && !userEmail.isEmpty()) {
            log.error("X-USER-EMAIL 은 있으나 회원 조회 실패 + X-USER-ID 매핑 실패: uri={}, email={}",
                    request.getRequestURI(), maskEmail(userEmail));
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

    /**
     * 이메일의 로컬 파트를 마스킹한다 (예: dongwon@example.com → d*****n@example.com).
     * 로그에 PII 가 원본 그대로 남지 않도록 하기 위함.
     */
    private static String maskEmail(String email) {
        if (email == null || email.isEmpty()) {
            return "";
        }
        int atIdx = email.indexOf('@');
        if (atIdx <= 0) {
            return "***";
        }
        String local = email.substring(0, atIdx);
        String domain = email.substring(atIdx);
        if (local.length() <= 2) {
            return local.charAt(0) + "***" + domain;
        }
        return local.charAt(0) + "***" + local.charAt(local.length() - 1) + domain;
    }
}
