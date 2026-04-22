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
 * 우선순위:
 * <ol>
 *   <li>X-USER-ID(sub) 가 유효한 UUID 이고 해당 회원이 DB 에 존재 → 그대로 컨텍스트에 설정 (대부분의 요청 경로)</li>
 *   <li>X-USER-ID 로 회원을 찾지 못했고 X-USER-EMAIL 로 회원을 찾으면 그 회원의 member_id 로 설정 (Cognito 이전 fallback)</li>
 *   <li>둘 다 실패했지만 X-USER-ID 가 유효한 UUID 인 경우 → '아직 가입되지 않은 사용자' (e.g. /auth/v1/sign-up) 로 간주하고
 *       X-USER-ID 를 그대로 컨텍스트에 설정하여 하위 핸들러가 처리하도록 pass-through 한다</li>
 * </ol>
 * X-USER-ID / X-USER-EMAIL 은 API Gateway Cognito Authorizer 가 검증된 JWT 클레임에서 주입/덮어쓰는 것으로
 * 신뢰되며, 외부에서 email 헤더만 위조해 타 계정을 가장하는 것은 Authorizer 계층에서 차단된다.
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
     *
     * @param request 현재 HTTP 요청
     * @param response 현재 HTTP 응답
     * @param handler 요청을 처리할 핸들러
     * @return 요청 처리를 계속 진행할지 여부 (true: 계속 진행, false: 중단)
     * @throws CustomException X-USER-ID 가 유효하지 않은 UUID 형식이거나 헤더가 전부 누락된 경우
     */
    @Override
    public boolean preHandle(final HttpServletRequest request, final HttpServletResponse response, final Object handler) {
        String userEmail = request.getHeader(USER_EMAIL_HEADER);
        String userIdString = request.getHeader(USER_ID_HEADER);

        UUID parsedUserId = null;
        if (userIdString != null && !userIdString.isEmpty()) {
            try {
                parsedUserId = UUID.fromString(userIdString);
            } catch (IllegalArgumentException e) {
                log.error("X-USER-ID header가 유효하지 않은 UUID 형식입니다: {}", userIdString, e);
                throw new CustomException(ErrorCode.INVALID_USER_ID_FORMAT);
            }
        }

        // 우선순위 1: X-USER-ID(sub) 가 회원 ID 와 일치하면 그대로 사용 (대부분의 요청)
        // memberRepository 가 없는 슬라이스 테스트 컨텍스트에서는 존재 검증을 생략한다.
        if (parsedUserId != null && (memberRepository == null || memberRepository.existsById(parsedUserId))) {
            setContext(parsedUserId, userEmail);
            return true;
        }

        // 우선순위 2: X-USER-EMAIL fallback (Cognito 이전으로 sub 가 바뀐 기존 회원 대응)
        if (memberRepository != null && userEmail != null && !userEmail.isEmpty()) {
            Optional<Member> memberOpt = memberRepository.findByMemberEmailAndMemberDeletedAtNull(userEmail);
            if (memberOpt.isPresent()) {
                setContext(memberOpt.get().getMemberId(), userEmail);
                return true;
            }
        }

        // 우선순위 3: X-USER-ID 는 유효하지만 DB 에 없고 email 로도 찾지 못한 경우
        //            → '아직 가입되지 않은 사용자' (e.g. /auth/v1/sign-up) 플로우로 간주하고
        //              X-USER-ID 로 컨텍스트만 세팅한 뒤 pass-through 한다.
        if (parsedUserId != null) {
            log.debug("신규 사용자 추정 pass-through: userId={}, email={}, uri={}",
                    parsedUserId, maskEmail(userEmail), request.getRequestURI());
            setContext(parsedUserId, userEmail);
            return true;
        }

        // X-USER-ID 자체가 없는 경우
        log.error("X-USER-ID 헤더가 누락되었습니다: {}", request.getRequestURI());
        throw new CustomException(ErrorCode.USER_ID_HEADER_MISSING);
    }

    private static void setContext(UUID userId, String userEmail) {
        UserContextHolder.setUserId(userId);
        if (userEmail != null && !userEmail.isEmpty()) {
            UserContextHolder.setUserEmail(userEmail);
        }
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
     * 이메일의 로컬 파트를 마스킹한다 (예: dongwon@example.com → d***n@example.com).
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
