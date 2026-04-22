package com.project200.undabang.common.web.interceptor;

import com.project200.undabang.common.context.UserContextHolder;
import com.project200.undabang.common.web.exception.CustomException;
import com.project200.undabang.common.web.exception.ErrorCode;
import com.project200.undabang.member.entity.Member;
import com.project200.undabang.member.repository.MemberRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@DisplayName("XUserIdCheckInterceptor 단위 테스트")
class XUserIdCheckInterceptorTest {

    private MemberRepository memberRepository;
    private XUserIdCheckInterceptor interceptor;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        memberRepository = mock(MemberRepository.class);
        interceptor = new XUserIdCheckInterceptor(memberRepository);
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        UserContextHolder.reset();
    }

    @AfterEach
    void tearDown() {
        UserContextHolder.reset();
    }

    @Nested
    @DisplayName("X-USER-ID 기반 정상 경로")
    class UserIdHappyPath {

        @Test
        @DisplayName("X-USER-ID 가 DB 에 존재하면 해당 ID 로 컨텍스트 세팅하고 통과")
        void existingUserId_setsContext() {
            UUID userId = UUID.randomUUID();
            request.addHeader("X-USER-ID", userId.toString());
            request.addHeader("X-USER-EMAIL", "user@example.com");
            given(memberRepository.existsByMemberIdAndMemberDeletedAtNull(userId)).willReturn(true);

            boolean result = interceptor.preHandle(request, response, new Object());

            assertThat(result).isTrue();
            assertThat(UserContextHolder.getUserId()).isEqualTo(userId);
            assertThat(UserContextHolder.getUserEmail()).isEqualTo("user@example.com");
        }

        @Test
        @DisplayName("X-USER-ID 만 있고 email 이 없어도 정상 통과")
        void existingUserId_withoutEmail_setsContext() {
            UUID userId = UUID.randomUUID();
            request.addHeader("X-USER-ID", userId.toString());
            given(memberRepository.existsByMemberIdAndMemberDeletedAtNull(userId)).willReturn(true);

            boolean result = interceptor.preHandle(request, response, new Object());

            assertThat(result).isTrue();
            assertThat(UserContextHolder.getUserId()).isEqualTo(userId);
            assertThat(UserContextHolder.getUserEmail()).isNull();
        }
    }

    @Nested
    @DisplayName("X-USER-EMAIL fallback (Cognito 이전)")
    class EmailFallback {

        @Test
        @DisplayName("X-USER-ID 가 DB 에 없지만 email 로 회원을 찾으면 그 회원 ID 로 세팅")
        void unknownUserId_butEmailMatches_usesMemberIdFromEmail() {
            UUID incomingSub = UUID.randomUUID();
            UUID dbMemberId = UUID.randomUUID();
            Member existing = Member.builder().memberId(dbMemberId).memberEmail("user@example.com").build();

            request.addHeader("X-USER-ID", incomingSub.toString());
            request.addHeader("X-USER-EMAIL", "user@example.com");
            given(memberRepository.existsByMemberIdAndMemberDeletedAtNull(incomingSub)).willReturn(false);
            given(memberRepository.findByMemberEmailAndMemberDeletedAtNull("user@example.com"))
                    .willReturn(Optional.of(existing));

            boolean result = interceptor.preHandle(request, response, new Object());

            assertThat(result).isTrue();
            assertThat(UserContextHolder.getUserId()).isEqualTo(dbMemberId);
            assertThat(UserContextHolder.getUserEmail()).isEqualTo("user@example.com");
        }
    }

    @Nested
    @DisplayName("신규 사용자 pass-through")
    class NewUserPassThrough {

        @Test
        @DisplayName("/auth/v1/sign-up 에서 X-USER-ID 가 유효 UUID 이고 DB/email 모두 찾지 못해도 통과")
        void signUpUri_unknownUserIdAndEmail_passesThrough() {
            UUID incomingSub = UUID.randomUUID();
            request.addHeader("X-USER-ID", incomingSub.toString());
            request.addHeader("X-USER-EMAIL", "newuser@example.com");
            request.setRequestURI("/auth/v1/sign-up");
            given(memberRepository.existsByMemberIdAndMemberDeletedAtNull(incomingSub)).willReturn(false);
            given(memberRepository.findByMemberEmailAndMemberDeletedAtNull("newuser@example.com"))
                    .willReturn(Optional.empty());

            boolean result = interceptor.preHandle(request, response, new Object());

            assertThat(result).isTrue();
            assertThat(UserContextHolder.getUserId()).isEqualTo(incomingSub);
            assertThat(UserContextHolder.getUserEmail()).isEqualTo("newuser@example.com");
        }

        @Test
        @DisplayName("/auth/v1/sign-up 에서 X-USER-EMAIL 없이 X-USER-ID 만 있어도 통과")
        void signUpUri_userIdOnly_passesThrough() {
            UUID incomingSub = UUID.randomUUID();
            request.addHeader("X-USER-ID", incomingSub.toString());
            request.setRequestURI("/auth/v1/sign-up");
            given(memberRepository.existsByMemberIdAndMemberDeletedAtNull(incomingSub)).willReturn(false);

            boolean result = interceptor.preHandle(request, response, new Object());

            assertThat(result).isTrue();
            assertThat(UserContextHolder.getUserId()).isEqualTo(incomingSub);
        }
    }

    @Nested
    @DisplayName("에러 케이스")
    class Errors {

        @Test
        @DisplayName("X-USER-ID 가 유효하지 않은 UUID 형식이면 INVALID_USER_ID_FORMAT 예외")
        void invalidUuidFormat_throwsInvalidUserIdFormat() {
            request.addHeader("X-USER-ID", "not-a-uuid");

            assertThatThrownBy(() -> interceptor.preHandle(request, response, new Object()))
                    .isInstanceOf(CustomException.class)
                    .extracting(ex -> ((CustomException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.INVALID_USER_ID_FORMAT);
        }

        @Test
        @DisplayName("X-USER-ID 헤더가 아예 누락되면 USER_ID_HEADER_MISSING 예외")
        void missingAllHeaders_throwsUserIdHeaderMissing() {
            assertThatThrownBy(() -> interceptor.preHandle(request, response, new Object()))
                    .isInstanceOf(CustomException.class)
                    .extracting(ex -> ((CustomException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.USER_ID_HEADER_MISSING);
        }

        @Test
        @DisplayName("X-USER-ID 없이 X-USER-EMAIL 만 있어도 email fallback 은 동작하지 않고 USER_ID_HEADER_MISSING")
        void onlyEmailHeader_doesNotTriggerFallbackAndThrowsUserIdHeaderMissing() {
            // email 단독 위조로 타 계정을 가장할 수 없도록, fallback 은 X-USER-ID 가 선행되어야 한다.
            Member existing = Member.builder().memberId(UUID.randomUUID()).memberEmail("victim@example.com").build();
            request.addHeader("X-USER-EMAIL", "victim@example.com");
            // findByMemberEmail 이 호출되더라도 fallback 이 동작하지 않아야 함을 검증하기 위해 stub 를 넣는다.
            given(memberRepository.findByMemberEmailAndMemberDeletedAtNull("victim@example.com"))
                    .willReturn(Optional.of(existing));

            assertThatThrownBy(() -> interceptor.preHandle(request, response, new Object()))
                    .isInstanceOf(CustomException.class)
                    .extracting(ex -> ((CustomException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.USER_ID_HEADER_MISSING);
            assertThat(UserContextHolder.getUserId()).isNull();
        }

        @Test
        @DisplayName("sign-up 이외 URI 에서 X-USER-ID 가 DB 에 없고 email 로도 찾지 못하면 AUTHENTICATION_FAILED")
        void nonSignUpUri_unknownUserIdAndEmail_throwsAuthenticationFailed() {
            UUID incomingSub = UUID.randomUUID();
            request.addHeader("X-USER-ID", incomingSub.toString());
            request.addHeader("X-USER-EMAIL", "ghost@example.com");
            request.setRequestURI("/api/members/me");
            given(memberRepository.existsByMemberIdAndMemberDeletedAtNull(incomingSub)).willReturn(false);
            given(memberRepository.findByMemberEmailAndMemberDeletedAtNull("ghost@example.com"))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> interceptor.preHandle(request, response, new Object()))
                    .isInstanceOf(CustomException.class)
                    .extracting(ex -> ((CustomException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.AUTHENTICATION_FAILED);
            assertThat(UserContextHolder.getUserId()).isNull();
        }

        @Test
        @DisplayName("sign-up 이외 URI 에서 X-USER-ID 가 DB 에 없고 email 도 없으면 AUTHENTICATION_FAILED")
        void nonSignUpUri_unknownUserIdWithoutEmail_throwsAuthenticationFailed() {
            UUID incomingSub = UUID.randomUUID();
            request.addHeader("X-USER-ID", incomingSub.toString());
            request.setRequestURI("/api/members/me");
            given(memberRepository.existsByMemberIdAndMemberDeletedAtNull(incomingSub)).willReturn(false);

            assertThatThrownBy(() -> interceptor.preHandle(request, response, new Object()))
                    .isInstanceOf(CustomException.class)
                    .extracting(ex -> ((CustomException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.AUTHENTICATION_FAILED);
        }

        @Test
        @DisplayName("soft-delete 된 회원(member_deleted_at NOT NULL)은 존재 검증에서 false 처리되어 통과하지 못한다")
        void softDeletedMember_isNotConsideredExisting() {
            UUID deletedUserId = UUID.randomUUID();
            request.addHeader("X-USER-ID", deletedUserId.toString());
            request.addHeader("X-USER-EMAIL", "deleted@example.com");
            request.setRequestURI("/api/members/me");
            // soft-delete 된 회원은 existsByMemberIdAndMemberDeletedAtNull 이 false 를 반환한다.
            given(memberRepository.existsByMemberIdAndMemberDeletedAtNull(deletedUserId)).willReturn(false);
            // email 조회 역시 memberDeletedAtNull 조건이라 탈퇴 회원을 찾지 못한다.
            given(memberRepository.findByMemberEmailAndMemberDeletedAtNull("deleted@example.com"))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> interceptor.preHandle(request, response, new Object()))
                    .isInstanceOf(CustomException.class)
                    .extracting(ex -> ((CustomException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.AUTHENTICATION_FAILED);
            assertThat(UserContextHolder.getUserId()).isNull();
        }
    }

    @Nested
    @DisplayName("존재 검증 positive 캐시")
    class ExistenceCache {

        @Test
        @DisplayName("연속된 요청에서 동일 userId 에 대한 DB 조회는 한 번만 발생한다 (positive 캐시)")
        void existsCheck_cachedAfterFirstHit() {
            UUID userId = UUID.randomUUID();
            given(memberRepository.existsByMemberIdAndMemberDeletedAtNull(userId)).willReturn(true);

            for (int i = 0; i < 5; i++) {
                MockHttpServletRequest req = new MockHttpServletRequest();
                req.addHeader("X-USER-ID", userId.toString());
                boolean result = interceptor.preHandle(req, response, new Object());
                assertThat(result).isTrue();
                UserContextHolder.reset();
            }

            verify(memberRepository, times(1)).existsByMemberIdAndMemberDeletedAtNull(userId);
        }

        @Test
        @DisplayName("존재하지 않는 userId 는 캐시되지 않아, 이후 가입되면 즉시 반영된다")
        void existsCheck_falseNotCached_signUpReflectedImmediately() {
            UUID userId = UUID.randomUUID();
            request.addHeader("X-USER-ID", userId.toString());
            request.setRequestURI("/auth/v1/sign-up");
            given(memberRepository.existsByMemberIdAndMemberDeletedAtNull(userId)).willReturn(false, true);

            // 1번째: 가입 전 - pass-through
            boolean first = interceptor.preHandle(request, response, new Object());
            assertThat(first).isTrue();
            UserContextHolder.reset();

            // 2번째: 가입 직후 - true 반환이 즉시 반영되어야 함 (캐시에 false 가 남아있지 않음)
            MockHttpServletRequest req2 = new MockHttpServletRequest();
            req2.addHeader("X-USER-ID", userId.toString());
            req2.setRequestURI("/api/members/me");
            boolean second = interceptor.preHandle(req2, response, new Object());
            assertThat(second).isTrue();
            assertThat(UserContextHolder.getUserId()).isEqualTo(userId);

            verify(memberRepository, times(2)).existsByMemberIdAndMemberDeletedAtNull(userId);
        }
    }

    @Nested
    @DisplayName("MemberRepository 미주입 슬라이스 테스트 컨텍스트")
    class NoRepository {

        @Test
        @DisplayName("memberRepository 가 null 이면 존재 검증 없이 X-USER-ID 로만 컨텍스트 세팅")
        void nullRepository_setsContextFromUserIdOnly() {
            XUserIdCheckInterceptor noRepoInterceptor = new XUserIdCheckInterceptor(null);
            UUID userId = UUID.randomUUID();
            request.addHeader("X-USER-ID", userId.toString());
            request.addHeader("X-USER-EMAIL", "user@example.com");

            boolean result = noRepoInterceptor.preHandle(request, response, new Object());

            assertThat(result).isTrue();
            assertThat(UserContextHolder.getUserId()).isEqualTo(userId);
            assertThat(UserContextHolder.getUserEmail()).isEqualTo("user@example.com");
        }
    }

    @Nested
    @DisplayName("afterCompletion")
    class AfterCompletion {

        @Test
        @DisplayName("afterCompletion 은 UserContextHolder 를 초기화한다")
        void afterCompletion_resetsUserContext() throws Exception {
            UUID userId = UUID.randomUUID();
            UserContextHolder.setUserId(userId);
            UserContextHolder.setUserEmail("user@example.com");

            interceptor.afterCompletion(request, response, new Object(), null);

            assertThat(UserContextHolder.getUserId()).isNull();
            assertThat(UserContextHolder.getUserEmail()).isNull();
        }
    }
}
