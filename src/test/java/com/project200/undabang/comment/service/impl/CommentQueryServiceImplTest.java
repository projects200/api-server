package com.project200.undabang.comment.service.impl;

import com.project200.undabang.comment.dto.response.CommentResponse;
import com.project200.undabang.comment.repository.CommentRepository;
import com.project200.undabang.common.web.exception.CustomException;
import com.project200.undabang.common.web.exception.ErrorCode;
import com.project200.undabang.feed.repository.FeedRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class CommentQueryServiceImplTest {

    @InjectMocks
    private CommentQueryServiceImpl commentQueryService;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private FeedRepository feedRepository;

    private List<CommentResponse> createSampleComments() {
        CommentResponse reply = new CommentResponse(
                2L,
                UUID.randomUUID(),
                "대댓글유저",
                "http://example.com/reply_profile.jpg",
                null,
                "대댓글 내용입니다.",
                3,
                LocalDateTime.now().minusMinutes(30),
                new ArrayList<>());

        CommentResponse parent = new CommentResponse(
                1L,
                UUID.randomUUID(),
                "댓글유저",
                "http://example.com/profile.jpg",
                null,
                "부모 댓글 내용입니다.",
                5,
                LocalDateTime.now().minusHours(1),
                List.of(reply));

        return List.of(parent);
    }

    @Nested
    @DisplayName("getComments 메소드는")
    class GetComments {

        @Test
        @DisplayName("피드 존재 시 댓글 목록을 정상 반환한다")
        void returnsComments_Success() {
            // given
            Long feedId = 1L;
            List<CommentResponse> mockComments = createSampleComments();

            given(feedRepository.existsById(feedId)).willReturn(true);
            given(commentRepository.findCommentsWithChildrenByFeedId(feedId)).willReturn(mockComments);

            // when
            List<CommentResponse> result = commentQueryService.getComments(feedId);

            // then
            assertThat(result).hasSize(1);
            assertThat(result.getFirst().children()).hasSize(1);
        }

        @Test
        @DisplayName("존재하지 않는 피드 조회 시 FEED_NOT_FOUND 예외를 던진다")
        void throwsException_WhenFeedNotFound() {
            // given
            Long feedId = 999L;

            given(feedRepository.existsById(feedId)).willReturn(false);

            // when & then
            assertThatThrownBy(() -> commentQueryService.getComments(feedId))
                    .isInstanceOf(CustomException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FEED_NOT_FOUND);
        }

        @Test
        @DisplayName("댓글이 없는 피드 조회 시 빈 목록을 반환한다")
        void returnsEmptyList_WhenNoComments() {
            // given
            Long feedId = 1L;

            given(feedRepository.existsById(feedId)).willReturn(true);
            given(commentRepository.findCommentsWithChildrenByFeedId(feedId)).willReturn(new ArrayList<>());

            // when
            List<CommentResponse> result = commentQueryService.getComments(feedId);

            // then
            assertThat(result).isEmpty();
        }
    }
}
