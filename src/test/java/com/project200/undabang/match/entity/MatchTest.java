package com.project200.undabang.match.entity;

import com.project200.undabang.member.entity.Member;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

@DisplayName("Match 엔티티 테스트")
class MatchTest {

    private Member createTestMember(UUID memberId) {
        return Member.builder()
                .memberId(memberId)
                .memberNickname("test-user-" + memberId)
                .build();
    }

    @Nested
    @DisplayName("from() 정적 팩토리 메소드는")
    class Describe_from {

        @Test
        @DisplayName("요청자와 수신자 정보를 받아 PENDING 상태의 Match 객체를 생성한다")
        void creates_pending_match_from_requester_and_receiver() {
            // given
            Member requester = createTestMember(UUID.randomUUID());
            Member receiver = createTestMember(UUID.randomUUID());

            // when
            Match match = Match.from(requester, receiver);

            // then
            assertThat(match).isNotNull();
            assertThat(match.getMatchId()).isNull(); // ID는 아직 할당되지 않음
            assertThat(match.getRequester()).isEqualTo(requester);
            assertThat(match.getReceiver()).isEqualTo(receiver);
            assertThat(match.getMatchStatus()).isEqualTo(MatchStatus.PENDING);

            // 생성 시간은 현재 시간과 거의 동일해야 함
            // 테스트 실행 시간차를 고려하여 1초 이내인지 검증
            assertThat(match.getMatchCreatedAt()).isCloseTo(LocalDateTime.now(), within(1, ChronoUnit.SECONDS));

            assertThat(match.getMatchCanceledAt()).isNull();
            assertThat(match.getMatchHandledAt()).isNull();
        }

        @Test
        @DisplayName("null 멤버를 전달해도 예외 없이 객체를 생성한다")
        void creates_instance_even_with_null_members() {
            // given
            Member requester = createTestMember(UUID.randomUUID());

            // when
            Match matchWithNullReceiver = Match.from(requester, null);
            Match matchWithNullRequester = Match.from(null, requester);
            Match matchWithAllNull = Match.from(null, null);

            // then
            assertThat(matchWithNullReceiver).isNotNull();
            assertThat(matchWithNullReceiver.getRequester()).isEqualTo(requester);
            assertThat(matchWithNullReceiver.getReceiver()).isNull();

            assertThat(matchWithNullRequester).isNotNull();
            assertThat(matchWithNullRequester.getRequester()).isNull();
            assertThat(matchWithNullRequester.getReceiver()).isEqualTo(requester);

            assertThat(matchWithAllNull).isNotNull();
            assertThat(matchWithAllNull.getRequester()).isNull();
            assertThat(matchWithAllNull.getReceiver()).isNull();
        }
    }
}