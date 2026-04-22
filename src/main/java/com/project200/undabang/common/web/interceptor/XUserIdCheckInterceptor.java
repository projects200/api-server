package com.project200.undabang.common.web.interceptor;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.project200.undabang.common.context.UserContextHolder;
import com.project200.undabang.common.web.exception.CustomException;
import com.project200.undabang.common.web.exception.ErrorCode;
import com.project200.undabang.member.entity.Member;
import com.project200.undabang.member.repository.MemberRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.Nullable;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Duration;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * HTTP 요청의 X-USER-ID / X-USER-EMAIL 헤더를 검증하고 사용자 컨텍스트를 설정하는 인터셉터
 * <p>
 * 우선순위:
 * <ol>
 *   <li>X-USER-ID(sub) 가 유효한 UUID 이고 해당 회원이 DB 에 존재(soft-delete 되지 않음) → 그대로 컨텍스트에 설정 (대부분의 요청 경로)</li>
 *   <li>X-USER-ID 는 있으나 DB 에 없고 X-USER-EMAIL 로 회원을 찾으면 그 회원의 member_id 로 설정 (Cognito 이전 fallback).
 *       이 fallback 은 반드시 X-USER-ID 가 선행되어야 동작하므로, email 헤더만 단독으로 위조해 가장하는 경로는 차단된다.</li>
 *   <li>{@link #UNREGISTERED_USER_ALLOWED_URIS} 에 등록된 URI (e.g. /auth/v1/sign-up) 에서는 DB/email 모두 찾지 못해도
 *       X-USER-ID 로 컨텍스트만 세팅하고 pass-through 한다 (아직 가입되지 않은 사용자용).</li>
 * </ol>
 * X-USER-ID / X-USER-EMAIL 은 API Gateway Cognito Authorizer 가 검증된 JWT 클레임에서 주입/덮어쓰는 것으로 신뢰된다.
 * 요청 처리가 완료된 후에는 사용자 컨텍스트를 자동으로 초기화한다.
 * <p>
 * 존재 검증은 positive 캐시({@link Caffeine}, TTL 5 분)를 통해 요청당 DB 조회 부담을 완화한다.
 * 존재하지 않는 결과(false) 는 캐시하지 않아 가입/복구 직후 즉시 반영된다.
 */
@Slf4j
public class XUserIdCheckInterceptor implements HandlerInterceptor {

    /** X-USER-ID 헤더 상수 */
    private static final String USER_ID_HEADER = "X-USER-ID";

    /** X-USER-EMAIL 헤더 상수 */
    private static final String USER_EMAIL_HEADER = "X-USER-EMAIL";

    /**
     * DB 에 회원이 아직 존재하지 않아도 통과시켜야 하는 URI allowlist.
     * 회원 가입 플로우는 신규 사용자를 DB 에 저장하기 전 단계이므로 컨텍스트만 세팅하고 pass-through 한다.
     */
    private static final Set<String> UNREGISTERED_USER_ALLOWED_URIS = Set.of(
            "/auth/v1/sign-up"
    );

    /**
     * MemberRepository 는 @WebMvcTest 등 JPA가 없는 슬라이스 테스트 컨텍스트에서는 null 일 수 있으며,
     * 이 경우 email 기반 조회 및 존재 검증은 생략되고 X-USER-ID(sub) 파싱만 수행됩니다.
     */
    @Nullable
    private final MemberRepository memberRepository;

    /** sub UUID 단위 positive 존재 캐시 (soft-delete 되지 않은 회원만 true 로 저장) */
    private final Cache<UUID, Boolean> memberExistenceCache;

    public XUserIdCheckInterceptor(@Nullable MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
        this.memberExistenceCache = Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofMinutes(5))
                .maximumSize(10_000)
                .build();
    }

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
        if (parsedUserId != null && memberExists(parsedUserId)) {
            setContext(parsedUserId, userEmail);
            return true;
        }

        // 우선순위 2: X-USER-EMAIL fallback (Cognito 이전으로 sub 가 바뀐 기존 회원 대응)
        // X-USER-ID 가 선행 존재해야만 동작시켜, email 헤더 단독 위조로 타 계정을 가장하는 것을 차단한다.
        if (parsedUserId != null && memberRepository != null && userEmail != null && !userEmail.isEmpty()) {
            Optional<Member> memberOpt = memberRepository.findByMemberEmailAndMemberDeletedAtNull(userEmail);
            if (memberOpt.isPresent()) {
                UUID memberId = memberOpt.get().getMemberId();
                memberExistenceCache.put(memberId, Boolean.TRUE);
                setContext(memberId, userEmail);
                return true;
            }
        }

        // 우선순위 3: 회원 가입 등 allowlist URI 에서만, X-USER-ID 로 컨텍스트 세팅 후 pass-through
        String uri = request.getRequestURI();
        if (parsedUserId != null && UNREGISTERED_USER_ALLOWED_URIS.contains(uri)) {
            log.debug("신규 사용자 pass-through: userId={}, email={}, uri={}",
                    parsedUserId, maskEmail(userEmail), uri);
            setContext(parsedUserId, userEmail);
            return true;
        }

        // 그 외: 회원을 특정할 수 없어 인증 실패로 처리
        if (parsedUserId == null) {
            log.error("X-USER-ID 헤더가 누락되었습니다: {}", uri);
            throw new CustomException(ErrorCode.USER_ID_HEADER_MISSING);
        }
        log.error("X-USER-ID / X-USER-EMAIL 로 회원을 특정하지 못했습니다: userId={}, email={}, uri={}",
                parsedUserId, maskEmail(userEmail), uri);
        throw new CustomException(ErrorCode.AUTHENTICATION_FAILED);
    }

    /**
     * soft-delete 되지 않은 회원인지 확인한다. Positive 캐시로 반복 조회 부담을 줄이되, false 는 캐시하지 않아
     * 가입 직후 다음 요청에서 즉시 반영되도록 한다. 슬라이스 테스트 컨텍스트(memberRepository=null) 에서는 true 로 간주.
     */
    private boolean memberExists(UUID userId) {
        if (memberRepository == null) {
            return true;
        }
        Boolean cached = memberExistenceCache.getIfPresent(userId);
        if (cached != null) {
            return cached;
        }
        boolean exists = memberRepository.existsByMemberIdAndMemberDeletedAtNull(userId);
        if (exists) {
            memberExistenceCache.put(userId, Boolean.TRUE);
        }
        return exists;
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
