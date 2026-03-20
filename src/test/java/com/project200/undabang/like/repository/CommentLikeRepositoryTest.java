package com.project200.undabang.like.repository;

import com.project200.undabang.comment.entity.Comment;
import com.project200.undabang.configuration.TestQuerydslConfig;
import com.project200.undabang.feed.entity.Feed;
import com.project200.undabang.feed.entity.FeedType;
import com.project200.undabang.like.entity.CommentLike;
import com.project200.undabang.member.entity.Member;
import com.project200.undabang.member.enums.MemberGender;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(TestQuerydslConfig.class)
class CommentLikeRepositoryTest {

    @Autowired
    private CommentLikeRepository commentLikeRepository;

    @Autowired
    private EntityManager em;

    @Nested
    @DisplayName("findByCommentAndMember 메소드는")
    class FindByCommentAndMember {

        @Test
        @DisplayName("댓글과 회원이 주어지면 해당 좋아요를 반환한다")
        void findByCommentAndMember_success() {
            // given
            Member member = createAndSaveMember();
            FeedType feedType = createAndSaveFeedType();
            Feed feed = createAndSaveFeed(member, feedType);
            Comment comment = createAndSaveComment(member, feed);
            CommentLike commentLike = createAndSaveCommentLike(comment, member);

            em.flush();
            em.clear();

            // when
            Optional<CommentLike> foundLike = commentLikeRepository.findByCommentAndMember(comment, member);

            // then
            assertThat(foundLike).isPresent();
            assertThat(foundLike.get().getId()).isEqualTo(commentLike.getId());
        }

        @Test
        @DisplayName("좋아요가 존재하지 않으면 빈 Optional을 반환한다")
        void findByCommentAndMember_isEmpty() {
            // given
            Member member = createAndSaveMember();
            FeedType feedType = createAndSaveFeedType();
            Feed feed = createAndSaveFeed(member, feedType);
            Comment comment = createAndSaveComment(member, feed);

            em.flush();
            em.clear();

            // when
            Optional<CommentLike> foundLike = commentLikeRepository.findByCommentAndMember(comment, member);

            // then
            assertThat(foundLike).isEmpty();
        }
    }


    private Member createAndSaveMember() {
        Member member = Member.builder()
                .memberId(UUID.randomUUID())
                .memberEmail("test@test.com")
                .memberNickname("testUser")
                .memberGender(MemberGender.MALE)
                .memberBday(LocalDate.of(1990, 1, 1))
                .memberScore((byte) 50)
                .build();
        em.persist(member);
        return member;
    }

    private FeedType createAndSaveFeedType() {
        FeedType feedType = FeedType.builder()
                .feedTypeName("General")
                .feedTypeDesc("General feed type")
                .feedTypeIsActive(true)
                .build();
        em.persist(feedType);
        return feedType;
    }

    private Feed createAndSaveFeed(Member member, FeedType feedType) {
        Feed feed = Feed.builder()
                .member(member)
                .feedType(feedType)
                .feedContent("Test Feed Content")
                .build();
        em.persist(feed);
        return feed;
    }

    private Comment createAndSaveComment(Member member, Feed feed) {
        Comment comment = Comment.builder()
                .member(member)
                .feed(feed)
                .content("Test Comment")
                .build();
        em.persist(comment);
        return comment;
    }

    private CommentLike createAndSaveCommentLike(Comment comment, Member member) {
        CommentLike commentLike = CommentLike.create(comment, member);
        em.persist(commentLike);
        return commentLike;
    }
}
