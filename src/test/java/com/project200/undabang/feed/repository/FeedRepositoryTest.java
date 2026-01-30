package com.project200.undabang.feed.repository;

import com.project200.undabang.configuration.TestQuerydslConfig;
import com.project200.undabang.feed.entity.Feed;
import com.project200.undabang.member.entity.Member;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(TestQuerydslConfig.class)
class FeedRepositoryTest {

    @Autowired
    private FeedRepository feedRepository;

    @Autowired
    private TestEntityManager em;

    @Nested
    @DisplayName("findByIdAndMemberAndDeletedAtNull 메소드는")
    class Describe_findByIdAndMemberAndDeletedAtNull {

        @Nested
        @DisplayName("본인이 작성하고 삭제되지 않은 피드 ID가 주어지면")
        class Context_with_valid_my_feed {

            @Test
            @DisplayName("해당 피드 엔티티를 Optional로 반환한다")
            void it_returns_optional_feed() {
                // given
                Member member = persistMember();
                Feed myFeed = persistFeed(member, "내 피드 내용");

                // when
                Optional<Feed> result = feedRepository.findByIdAndMemberAndDeletedAtNull(myFeed.getId(), member);

                // then
                assertThat(result).isPresent();
                assertThat(result.get().getFeedContent()).isEqualTo("내 피드 내용");
            }
        }

        @Nested
        @DisplayName("삭제된 피드(deletedAt이 null이 아님) ID가 주어지면")
        class Context_with_deleted_feed {

            @Test
            @DisplayName("빈 Optional을 반환한다")
            void it_returns_empty_optional() {
                // given
                Member member = persistMember();
                // 헬퍼 메서드를 통해 삭제된 피드 생성
                Feed deletedFeed = persistDeletedFeed(member, "삭제된 글");

                // when
                Optional<Feed> result = feedRepository.findByIdAndMemberAndDeletedAtNull(deletedFeed.getId(), member);

                // then
                assertThat(result).isEmpty();
            }
        }
    }

    private Member persistMember() {
        Member member = Member.builder()
                .memberId(UUID.randomUUID())
                .memberEmail(UUID.randomUUID() + "@test.com")
                .memberNickname("유저_" + UUID.randomUUID().toString().substring(0, 5))
                .memberBday(LocalDate.of(1995, 1, 1))
                .build();
        return em.persist(member);
    }

    private Feed persistFeed(Member member, String content) {
        Feed feed = Feed.builder()
                .member(member)
                .feedContent(content)
                .createdAt(LocalDateTime.now())
                .likesCount(0)
                .commentsCount(0)
                .build();
        return em.persist(feed);
    }

    private Feed persistDeletedFeed(Member member, String content) {
        Feed feed = Feed.builder()
                .member(member)
                .feedContent(content)
                .createdAt(LocalDateTime.now())
                .deletedAt(LocalDateTime.now())
                .likesCount(0)
                .commentsCount(0)
                .build();
        return em.persist(feed);
    }
}