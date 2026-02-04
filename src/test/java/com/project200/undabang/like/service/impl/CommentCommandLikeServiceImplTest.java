package com.project200.undabang.like.service.impl;

import com.project200.undabang.comment.entity.Comment;
import com.project200.undabang.comment.repository.CommentRepository;
import com.project200.undabang.common.context.UserContextHolder;
import com.project200.undabang.common.web.exception.CustomException;
import com.project200.undabang.common.web.exception.ErrorCode;
import com.project200.undabang.feed.entity.Feed;
import com.project200.undabang.like.dto.request.CreateCommentLikeRequest;
import com.project200.undabang.like.entity.CommentLike;
import com.project200.undabang.like.repository.CommentLikeRepository;
import com.project200.undabang.member.entity.Member;
import com.project200.undabang.member.repository.MemberRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.BDDMockito;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class CommentCommandLikeServiceImplTest {

    @InjectMocks
    private CommentCommandLikeServiceImpl commentCommandLikeService;

    @Mock
    private CommentLikeRepository commentLikeRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private CommentRepository commentRepository;

    @Nested
    @DisplayName("createCommentLike 메소드는")
    class CreateCommentLike {

        @Test
        @DisplayName("좋아요 생성 요청 시, 좋아요가 없으면 생성한다")
        void createCommentLike_success_create() {
            try (MockedStatic<UserContextHolder> ignored = BDDMockito.mockStatic(UserContextHolder.class)) {
                // given
                Long commentId = 1L;
                UUID userId = UUID.randomUUID();
                CreateCommentLikeRequest request = new CreateCommentLikeRequest(true);

                Member member = Member.builder().memberId(userId).build();
                Feed feed = BDDMockito.mock(Feed.class);
                Comment comment = Comment.builder()
                        .id(commentId)
                        .member(member)
                        .feed(feed)
                        .content("test content")
                        .build();

                given(UserContextHolder.getUserId()).willReturn(userId);
                given(commentRepository.findByIdAndDeletedAtIsNull(commentId)).willReturn(Optional.of(comment));
                given(memberRepository.findByMemberIdAndMemberDeletedAtNull(userId)).willReturn(Optional.of(member));
                given(commentLikeRepository.findByCommentAndMember(comment, member)).willReturn(Optional.empty());

                // when
                commentCommandLikeService.createCommentLike(commentId, request);

                // then
                then(commentLikeRepository).should(times(1)).save(any(CommentLike.class));
            }
        }

        @Test
        @DisplayName("좋아요 생성 요청 시, 이미 좋아요가 존재하면 아무것도 하지 않는다")
        void createCommentLike_success_noop_create() {
            try (MockedStatic<UserContextHolder> ignored = BDDMockito.mockStatic(UserContextHolder.class)) {
                // given
                Long commentId = 1L;
                UUID userId = UUID.randomUUID();
                CreateCommentLikeRequest request = new CreateCommentLikeRequest(true);

                Member member = Member.builder().memberId(userId).build();
                Feed feed = BDDMockito.mock(Feed.class);
                Comment comment = Comment.builder().id(commentId).member(member).feed(feed).content("test content")
                        .build();
                CommentLike existingLike = CommentLike.create(comment, member);

                given(UserContextHolder.getUserId()).willReturn(userId);
                given(commentRepository.findByIdAndDeletedAtIsNull(commentId)).willReturn(Optional.of(comment));
                given(memberRepository.findByMemberIdAndMemberDeletedAtNull(userId)).willReturn(Optional.of(member));
                given(commentLikeRepository.findByCommentAndMember(comment, member))
                        .willReturn(Optional.of(existingLike));

                // when
                commentCommandLikeService.createCommentLike(commentId, request);

                // then
                then(commentLikeRepository).should(never()).save(any(CommentLike.class));
            }
        }

        @Test
        @DisplayName("좋아요 취소 요청 시, 좋아요가 존재하면 삭제한다")
        void createCommentLike_success_delete() {
            try (MockedStatic<UserContextHolder> ignored = BDDMockito.mockStatic(UserContextHolder.class)) {
                // given
                Long commentId = 1L;
                UUID userId = UUID.randomUUID();
                CreateCommentLikeRequest request = new CreateCommentLikeRequest(false);

                Member member = Member.builder().memberId(userId).build();
                Feed feed = BDDMockito.mock(Feed.class);
                Comment comment = Comment.builder()
                        .id(commentId)
                        .member(member)
                        .feed(feed)
                        .content("test content")
                        .build();
                CommentLike existingLike = CommentLike.create(comment, member);

                given(UserContextHolder.getUserId()).willReturn(userId);
                given(commentRepository.findByIdAndDeletedAtIsNull(commentId)).willReturn(Optional.of(comment));
                given(memberRepository.findByMemberIdAndMemberDeletedAtNull(userId)).willReturn(Optional.of(member));
                given(commentLikeRepository.findByCommentAndMember(comment, member))
                        .willReturn(Optional.of(existingLike));

                // when
                commentCommandLikeService.createCommentLike(commentId, request);

                // then
                then(commentLikeRepository).should(times(1)).delete(existingLike);
            }
        }

        @Test
        @DisplayName("댓글이 존재하지 않으면 예외를 던진다")
        void createCommentLike_fail_commentNotFound() {
            try (MockedStatic<UserContextHolder> ignored = BDDMockito.mockStatic(UserContextHolder.class)) {
                // given
                Long commentId = 1L;
                CreateCommentLikeRequest request = new CreateCommentLikeRequest(true);

                given(UserContextHolder.getUserId()).willReturn(UUID.randomUUID());
                given(commentRepository.findByIdAndDeletedAtIsNull(commentId)).willReturn(Optional.empty());

                // when & then
                assertThatThrownBy(() -> commentCommandLikeService.createCommentLike(commentId, request))
                        .isInstanceOf(CustomException.class)
                        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.COMMENT_NOT_FOUND);
            }
        }

        @Test
        @DisplayName("회원이 존재하지 않으면 예외를 던진다")
        void createCommentLike_fail_memberNotFound() {
            try (MockedStatic<UserContextHolder> ignored = BDDMockito.mockStatic(UserContextHolder.class)) {
                // given
                Long commentId = 1L;
                UUID userId = UUID.randomUUID();
                CreateCommentLikeRequest request = new CreateCommentLikeRequest(true);

                Member member = Member.builder().memberId(userId).build();
                Feed feed = BDDMockito.mock(Feed.class);
                Comment comment = Comment.builder()
                        .id(commentId)
                        .member(member)
                        .feed(feed)
                        .content("test content")
                        .build();

                given(UserContextHolder.getUserId()).willReturn(userId);
                given(commentRepository.findByIdAndDeletedAtIsNull(commentId)).willReturn(Optional.of(comment));
                given(memberRepository.findByMemberIdAndMemberDeletedAtNull(userId)).willReturn(Optional.empty());

                // when & then
                assertThatThrownBy(() -> commentCommandLikeService.createCommentLike(commentId, request))
                        .isInstanceOf(CustomException.class)
                        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MEMBER_NOT_FOUND);
            }
        }
    }
}
