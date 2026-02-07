package com.project200.undabang.comment.service.impl;

import com.project200.undabang.comment.dto.request.CreateCommentRequest;
import com.project200.undabang.comment.dto.response.CreateCommentResponse;
import com.project200.undabang.comment.entity.Comment;
import com.project200.undabang.comment.repository.CommentRepository;
import com.project200.undabang.common.context.UserContextHolder;
import com.project200.undabang.common.web.exception.CustomException;
import com.project200.undabang.common.web.exception.ErrorCode;
import com.project200.undabang.feed.entity.Feed;
import com.project200.undabang.feed.entity.FeedType;
import com.project200.undabang.feed.repository.FeedRepository;
import com.project200.undabang.member.entity.Member;
import com.project200.undabang.member.enums.MemberGender;
import com.project200.undabang.member.repository.MemberRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mockStatic;

@ExtendWith(MockitoExtension.class)
class CommentCommandServiceImplTest {

    @InjectMocks
    private CommentCommandServiceImpl commentCommandService;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private FeedRepository feedRepository;

    @Mock
    private MemberRepository memberRepository;

    private Member createMember(UUID memberId) {
        return Member.builder()
                .memberId(memberId)
                .memberEmail("test@gmail.com")
                .memberNickname("테스트유저")
                .memberGender(MemberGender.UNKNOWN)
                .memberBday(LocalDate.of(1990, 1, 1))
                .memberScore((byte) 50)
                .memberDesc("테스트 자기소개입니다.")
                .build();
    }

    private FeedType createFeedType() {
        return FeedType.builder()
                .feedTypeName("일반")
                .feedTypeDesc("일반 피드")
                .feedTypeIsActive(true)
                .build();
    }

    private Feed createFeed(Long feedId, Member member) {
        Feed feed = Feed.builder()
                .member(member)
                .feedType(createFeedType())
                .feedContent("피드 내용")
                .build();
        ReflectionTestUtils.setField(feed, "id", feedId);
        return feed;
    }

    private Comment createComment(Long commentId, Member member, Feed feed, Comment parent) {
        Comment comment = Comment.builder()
                .member(member)
                .feed(feed)
                .parent(parent)
                .content("댓글 내용")
                .build();
        ReflectionTestUtils.setField(comment, "id", commentId);
        return comment;
    }

    @Nested
    @DisplayName("createComment 메소드는")
    class CreateComment {

        @Test
        @DisplayName("댓글 작성을 성공한다")
        void createsComment_Success() {
            // given
            UUID userId = UUID.randomUUID();
            Long feedId = 1L;
            CreateCommentRequest request = new CreateCommentRequest("새 댓글입니다.", null);

            Member member = createMember(userId);
            Feed feed = createFeed(feedId, member);
            Comment savedComment = createComment(1L, member, feed, null);

            try (MockedStatic<UserContextHolder> ignored = mockStatic(UserContextHolder.class)) {
                given(UserContextHolder.getUserId()).willReturn(userId);
                given(feedRepository.findById(feedId)).willReturn(Optional.of(feed));
                given(memberRepository.findByMemberIdAndMemberDeletedAtNull(userId)).willReturn(Optional.of(member));
                given(commentRepository.save(any(Comment.class))).willReturn(savedComment);

                // when
                CreateCommentResponse result = commentCommandService.createComment(feedId, request);

                // then
                assertThat(result.commentId()).isEqualTo(1L);
            }
        }

        @Test
        @DisplayName("대댓글 작성을 성공한다")
        void createsReply_Success() {
            // given
            UUID userId = UUID.randomUUID();
            Long feedId = 1L;
            Long parentCommentId = 1L;
            CreateCommentRequest request = new CreateCommentRequest("대댓글입니다.", parentCommentId);

            Member member = createMember(userId);
            Feed feed = createFeed(feedId, member);
            Comment parentComment = createComment(parentCommentId, member, feed, null);
            Comment savedReply = createComment(2L, member, feed, parentComment);

            try (MockedStatic<UserContextHolder> ignored = mockStatic(UserContextHolder.class)) {
                given(UserContextHolder.getUserId()).willReturn(userId);
                given(feedRepository.findById(feedId)).willReturn(Optional.of(feed));
                given(memberRepository.findByMemberIdAndMemberDeletedAtNull(userId)).willReturn(Optional.of(member));
                given(commentRepository.findByIdAndDeletedAtIsNull(parentCommentId))
                        .willReturn(Optional.of(parentComment));
                given(commentRepository.save(any(Comment.class))).willReturn(savedReply);

                // when
                CreateCommentResponse result = commentCommandService.createComment(feedId, request);

                // then
                assertThat(result.commentId()).isEqualTo(2L);
            }
        }

        @Test
        @DisplayName("존재하지 않는 피드에 댓글 작성 시 예외를 던진다")
        void throwsException_WhenFeedNotFound() {
            // given
            UUID userId = UUID.randomUUID();
            Long feedId = 999L;
            CreateCommentRequest request = new CreateCommentRequest("새 댓글입니다.", null);

            try (MockedStatic<UserContextHolder> ignored = mockStatic(UserContextHolder.class)) {
                given(UserContextHolder.getUserId()).willReturn(userId);
                given(feedRepository.findById(feedId)).willReturn(Optional.empty());

                // when & then
                assertThatThrownBy(() -> commentCommandService.createComment(feedId, request))
                        .isInstanceOf(CustomException.class)
                        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FEED_NOT_FOUND);
            }
        }

        @Test
        @DisplayName("존재하지 않는 부모 댓글에 대댓글 작성 시 예외를 던진다")
        void throwsException_WhenParentNotFound() {
            // given
            UUID userId = UUID.randomUUID();
            Long feedId = 1L;
            Long parentCommentId = 999L;
            CreateCommentRequest request = new CreateCommentRequest("대댓글입니다.", parentCommentId);

            Member member = createMember(userId);
            Feed feed = createFeed(feedId, member);

            try (MockedStatic<UserContextHolder> ignored = mockStatic(UserContextHolder.class)) {
                given(UserContextHolder.getUserId()).willReturn(userId);
                given(feedRepository.findById(feedId)).willReturn(Optional.of(feed));
                given(memberRepository.findByMemberIdAndMemberDeletedAtNull(userId)).willReturn(Optional.of(member));
                given(commentRepository.findByIdAndDeletedAtIsNull(parentCommentId)).willReturn(Optional.empty());

                // when & then
                assertThatThrownBy(() -> commentCommandService.createComment(feedId, request))
                        .isInstanceOf(CustomException.class)
                        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.COMMENT_PARENT_NOT_FOUND);
            }
        }
    }

    @Nested
    @DisplayName("deleteComment 메소드는")
    class DeleteComment {

        @Test
        @DisplayName("댓글 삭제를 성공한다")
        void deletesComment_Success() {
            // given
            UUID userId = UUID.randomUUID();
            Long commentId = 1L;

            Member member = createMember(userId);
            Feed feed = createFeed(1L, member);
            Comment comment = createComment(commentId, member, feed, null);

            try (MockedStatic<UserContextHolder> ignored = mockStatic(UserContextHolder.class)) {
                given(UserContextHolder.getUserId()).willReturn(userId);
                given(memberRepository.findByMemberIdAndMemberDeletedAtNull(userId)).willReturn(Optional.of(member));
                given(commentRepository.findByIdAndDeletedAtIsNull(commentId)).willReturn(Optional.of(comment));

                // when
                commentCommandService.deleteComment(commentId);

                // then
                assertThat(comment.isDeleted()).isTrue();
            }
        }

        @Test
        @DisplayName("존재하지 않는 댓글 삭제 시 예외를 던진다")
        void throwsException_WhenCommentNotFound() {
            // given
            UUID userId = UUID.randomUUID();
            Long commentId = 999L;

            try (MockedStatic<UserContextHolder> ignored = mockStatic(UserContextHolder.class)) {
                given(UserContextHolder.getUserId()).willReturn(userId);
                given(commentRepository.findByIdAndDeletedAtIsNull(commentId)).willReturn(Optional.empty());

                // when & then
                assertThatThrownBy(() -> commentCommandService.deleteComment(commentId))
                        .isInstanceOf(CustomException.class)
                        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.COMMENT_NOT_FOUND);
            }
        }

        @Test
        @DisplayName("작성자가 아닌 회원이 삭제 시도 시 예외를 던진다")
        void throwsException_WhenNotAuthor() {
            // given
            UUID currentUserId = UUID.randomUUID();
            UUID authorId = UUID.randomUUID();
            Long commentId = 1L;

            Member author = createMember(authorId);
            Member nonAuthor = createMember(currentUserId);
            Feed feed = createFeed(1L, author);
            Comment comment = createComment(commentId, author, feed, null);

            try (MockedStatic<UserContextHolder> ignored = mockStatic(UserContextHolder.class)) {
                given(UserContextHolder.getUserId()).willReturn(currentUserId);
                given(memberRepository.findByMemberIdAndMemberDeletedAtNull(currentUserId))
                        .willReturn(Optional.of(nonAuthor));
                given(commentRepository.findByIdAndDeletedAtIsNull(commentId)).willReturn(Optional.of(comment));

                // when & then
                assertThatThrownBy(() -> commentCommandService.deleteComment(commentId))
                        .isInstanceOf(CustomException.class)
                        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.COMMENT_DELETE_FORBIDDEN);
            }
        }
    }
}
