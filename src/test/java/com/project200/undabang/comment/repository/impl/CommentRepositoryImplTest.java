package com.project200.undabang.comment.repository.impl;

import com.project200.undabang.comment.dto.response.CommentResponse;
import com.project200.undabang.comment.entity.Comment;
import com.project200.undabang.comment.repository.CommentRepository;
import com.project200.undabang.common.entity.Picture;
import com.project200.undabang.configuration.TestQuerydslConfig;
import com.project200.undabang.feed.entity.Feed;
import com.project200.undabang.member.entity.Member;
import com.project200.undabang.member.entity.MemberPicture;
import com.project200.undabang.member.enums.MemberGender;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(TestQuerydslConfig.class)
@DisplayName("CommentRepositoryImpl 통합 테스트")
class CommentRepositoryImplTest {

    @Autowired
    private EntityManager em;

    @Autowired
    private CommentRepository commentRepository;

    @Nested
    @DisplayName("findCommentsWithChildrenByFeedId 메소드는")
    class Describe_findCommentsWithChildrenByFeedId {

        @Test
        @DisplayName("피드의 댓글 목록을 조회할 때 프로필 사진이 있는 회원의 경우 Picture URL을 정상적으로 반환한다")
        void it_returns_comments_with_profile_picture_url() {
            // given
            Member author = createAndSaveMember("작성자", "author@test.com");
            createAndSaveMemberProfilePicture(author, "https://example.com/profile.jpg");

            Feed feed = createAndSaveFeed(author, "피드 내용");
            Comment parentComment = createAndSaveComment(feed, author, null, "부모 댓글 내용");

            flushAndClear();

            // when
            List<CommentResponse> result = commentRepository.findCommentsWithChildrenByFeedId(feed.getId(), author);

            // then
            assertThat(result).hasSize(1);
            CommentResponse response = result.get(0);
            assertThat(response.memberNickname()).isEqualTo("작성자");
            assertThat(response.memberProfileImageUrl()).isEqualTo("https://example.com/profile.jpg");
            assertThat(response.content()).isEqualTo("부모 댓글 내용");
        }

        @Test
        @DisplayName("프로필 사진이 없는 회원의 댓글은 memberProfileImageUrl이 null로 반환된다")
        void it_returns_null_profile_url_when_member_has_no_picture() {
            // given
            Member author = createAndSaveMember("사진없음", "nophoto@test.com");
            Feed feed = createAndSaveFeed(author, "피드");
            Comment comment = createAndSaveComment(feed, author, null, "댓글");

            flushAndClear();

            // when
            List<CommentResponse> result = commentRepository.findCommentsWithChildrenByFeedId(feed.getId(), author);

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).memberProfileImageUrl()).isNull();
        }

        @Test
        @DisplayName("부모 댓글과 대댓글을 계층 구조로 정상 조회한다")
        void it_returns_parent_and_child_comments_hierarchically() {
            // given
            Member author = createAndSaveMember("작성자", "author@test.com");
            Member replier = createAndSaveMember("답글러", "replier@test.com");
            createAndSaveMemberProfilePicture(replier, "https://example.com/replier.jpg");

            Feed feed = createAndSaveFeed(author, "피드 내용");
            Comment parent = createAndSaveComment(feed, author, null, "부모 댓글");
            Comment child1 = createAndSaveComment(feed, replier, parent, "대댓글 1");
            Comment child2 = createAndSaveComment(feed, replier, parent, "대댓글 2");

            flushAndClear();

            // when
            List<CommentResponse> result = commentRepository.findCommentsWithChildrenByFeedId(feed.getId(), author);

            // then
            assertThat(result).hasSize(1);
            CommentResponse parentResponse = result.get(0);
            assertThat(parentResponse.content()).isEqualTo("부모 댓글");
            assertThat(parentResponse.children()).hasSize(2);
            assertThat(parentResponse.children())
                    .extracting(CommentResponse::content)
                    .containsExactly("대댓글 1", "대댓글 2");
            assertThat(parentResponse.children().get(0).memberProfileImageUrl())
                    .isEqualTo("https://example.com/replier.jpg");
        }

        @Test
        @DisplayName("삭제된 댓글은 조회 결과에서 제외된다")
        void it_excludes_deleted_comments() {
            // given
            Member author = createAndSaveMember("작성자", "author@test.com");
            Feed feed = createAndSaveFeed(author, "피드");
            Comment activeComment = createAndSaveComment(feed, author, null, "살아있는 댓글");
            Comment deletedComment = createAndSaveDeletedComment(feed, author, null, "삭제된 댓글");

            flushAndClear();

            // when
            List<CommentResponse> result = commentRepository.findCommentsWithChildrenByFeedId(feed.getId(), author);

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).content()).isEqualTo("살아있는 댓글");
        }

        @Test
        @DisplayName("부모 댓글은 살아있지만 대댓글이 삭제된 경우 대댓글은 제외된다")
        void it_excludes_deleted_child_comments() {
            // given
            Member author = createAndSaveMember("작성자", "author@test.com");
            Feed feed = createAndSaveFeed(author, "피드");
            Comment parent = createAndSaveComment(feed, author, null, "부모 댓글");
            Comment activeChild = createAndSaveComment(feed, author, parent, "살아있는 대댓글");
            Comment deletedChild = createAndSaveDeletedComment(feed, author, parent, "삭제된 대댓글");

            flushAndClear();

            // when
            List<CommentResponse> result = commentRepository.findCommentsWithChildrenByFeedId(feed.getId(), author);

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).children()).hasSize(1);
            assertThat(result.get(0).children().get(0).content()).isEqualTo("살아있는 대댓글");
        }

        @Test
        @DisplayName("댓글이 없는 피드는 빈 리스트를 반환한다")
        void it_returns_empty_list_when_no_comments() {
            // given
            Member author = createAndSaveMember("작성자", "author@test.com");
            Feed feed = createAndSaveFeed(author, "댓글 없는 피드");

            flushAndClear();

            // when
            List<CommentResponse> result = commentRepository.findCommentsWithChildrenByFeedId(feed.getId(), author);

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("여러 부모 댓글과 각각의 대댓글을 정상적으로 조회한다")
        void it_returns_multiple_parents_with_each_children() {
            // given
            Member author = createAndSaveMember("작성자", "author@test.com");
            Feed feed = createAndSaveFeed(author, "피드");

            Comment parent1 = createAndSaveComment(feed, author, null, "첫 번째 부모");
            createAndSaveComment(feed, author, parent1, "첫 번째 부모의 대댓글");

            Comment parent2 = createAndSaveComment(feed, author, null, "두 번째 부모");
            createAndSaveComment(feed, author, parent2, "두 번째 부모의 대댓글 1");
            createAndSaveComment(feed, author, parent2, "두 번째 부모의 대댓글 2");

            flushAndClear();

            // when
            List<CommentResponse> result = commentRepository.findCommentsWithChildrenByFeedId(feed.getId(), author);

            // then
            assertThat(result).hasSize(2);
            assertThat(result.get(0).children()).hasSize(1);
            assertThat(result.get(1).children()).hasSize(2);
        }

        @Test
        @DisplayName("댓글의 likesCount를 정상적으로 조회한다")
        void it_returns_comments_with_likes_count() {
            // given
            Member author = createAndSaveMember("작성자", "author@test.com");
            Feed feed = createAndSaveFeed(author, "피드");
            Comment comment = Comment.builder()
                    .feed(feed)
                    .member(author)
                    .content("좋아요 5개 댓글")
                    .likesCount(5)
                    .createdAt(LocalDateTime.now())
                    .build();
            em.persist(comment);

            flushAndClear();

            // when
            List<CommentResponse> result = commentRepository.findCommentsWithChildrenByFeedId(feed.getId(), author);

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).likesCount()).isEqualTo(5);
        }
    }

    // ==================== Helper Methods ====================

    private Member createAndSaveMember(String nickname, String email) {
        Member member = Member.builder()
                .memberId(UUID.randomUUID())
                .memberEmail(email)
                .memberNickname(nickname)
                .memberGender(MemberGender.MALE)
                .memberBday(LocalDate.of(1990, 1, 1))
                .memberScore((byte) 50)
                .build();
        em.persist(member);
        return member;
    }

    private void createAndSaveMemberProfilePicture(Member member, String url) {
        Picture picture = Picture.builder()
                .pictureUrl(url)
                .pictureName("profile.jpg")
                .pictureSize(1024)
                .build();
        em.persist(picture);

        MemberPicture memberPicture = MemberPicture.builder()
                .member(member)
                .picture(picture)
                .memberPicturesUrl(url)
                .build();
        em.persist(memberPicture);

        member.updateProfilePicture(memberPicture);
        em.flush();
    }

    private Feed createAndSaveFeed(Member member, String content) {
        Feed feed = Feed.builder()
                .member(member)
                .feedContent(content)
                .likesCount(0)
                .commentsCount(0)
                .createdAt(LocalDateTime.now())
                .build();
        em.persist(feed);
        return feed;
    }

    private Comment createAndSaveComment(Feed feed, Member member, Comment parent, String content) {
        Comment comment = Comment.builder()
                .feed(feed)
                .member(member)
                .parent(parent)
                .content(content)
                .likesCount(0)
                .createdAt(LocalDateTime.now())
                .build();
        em.persist(comment);
        return comment;
    }

    private Comment createAndSaveDeletedComment(Feed feed, Member member, Comment parent, String content) {
        Comment comment = Comment.builder()
                .feed(feed)
                .member(member)
                .parent(parent)
                .content(content)
                .likesCount(0)
                .createdAt(LocalDateTime.now())
                .deletedAt(LocalDateTime.now())
                .build();
        em.persist(comment);
        return comment;
    }

    private void flushAndClear() {
        em.flush();
        em.clear();
    }
}
