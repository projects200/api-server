package com.project200.undabang.like.service.impl;

import com.project200.undabang.common.context.UserContextHolder;
import com.project200.undabang.common.web.exception.CustomException;
import com.project200.undabang.common.web.exception.ErrorCode;
import com.project200.undabang.feed.entity.Feed;
import com.project200.undabang.feed.repository.FeedRepository;
import com.project200.undabang.like.dto.CreateFeedLikeRequest;
import com.project200.undabang.like.entity.FeedLike;
import com.project200.undabang.like.repository.FeedLikeRepository;
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

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class FeedLikeCommandServiceImplTest {

    @InjectMocks
    private FeedLikeCommandServiceImpl feedLikeCommandService;

    @Mock
    private FeedLikeRepository feedLikeRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private FeedRepository feedRepository;

    @Nested
    @DisplayName("createFeedLike 메소드는")
    class CreateFeedLike {

        @Test
        @DisplayName("좋아요 생성 요청 시, 좋아요가 없으면 생성한다")
        void createFeedLike_success_create() {
            try (MockedStatic<UserContextHolder> ignored = BDDMockito.mockStatic(UserContextHolder.class)) {
                // given
                Long feedId = 1L;
                UUID userId = UUID.randomUUID();
                CreateFeedLikeRequest request = new CreateFeedLikeRequest(true);

                Member member = Member.builder().memberId(userId).build();
                Feed feed = BDDMockito.mock(Feed.class);

                given(UserContextHolder.getUserId()).willReturn(userId);
                given(feedRepository.findByIdAndDeletedAtNull(feedId)).willReturn(Optional.of(feed));
                given(memberRepository.findByMemberIdAndMemberDeletedAtNull(userId)).willReturn(Optional.of(member));
                given(feedLikeRepository.findByFeedAndMember(feed, member)).willReturn(Optional.empty());

                // when
                feedLikeCommandService.createFeedLike(feedId, request);

                // then
                then(feedLikeRepository).should(times(1)).save(any(FeedLike.class));
            }
        }

        @Test
        @DisplayName("좋아요 생성 요청 시, 이미 좋아요가 존재하면 아무것도 하지 않는다")
        void createFeedLike_success_noop_create() {
            try (MockedStatic<UserContextHolder> ignored = BDDMockito.mockStatic(UserContextHolder.class)) {
                // given
                Long feedId = 1L;
                UUID userId = UUID.randomUUID();
                CreateFeedLikeRequest request = new CreateFeedLikeRequest(true);

                Member member = Member.builder().memberId(userId).build();
                Feed feed = BDDMockito.mock(Feed.class);
                FeedLike existingLike = FeedLike.create(feed, member);

                given(UserContextHolder.getUserId()).willReturn(userId);
                given(feedRepository.findByIdAndDeletedAtNull(feedId)).willReturn(Optional.of(feed));
                given(memberRepository.findByMemberIdAndMemberDeletedAtNull(userId)).willReturn(Optional.of(member));
                given(feedLikeRepository.findByFeedAndMember(feed, member))
                        .willReturn(Optional.of(existingLike));

                // when
                feedLikeCommandService.createFeedLike(feedId, request);

                // then
                then(feedLikeRepository).should(never()).save(any(FeedLike.class));
            }
        }

        @Test
        @DisplayName("좋아요 취소 요청 시, 좋아요가 존재하면 삭제한다")
        void createFeedLike_success_delete() {
            try (MockedStatic<UserContextHolder> ignored = BDDMockito.mockStatic(UserContextHolder.class)) {
                // given
                Long feedId = 1L;
                UUID userId = UUID.randomUUID();
                CreateFeedLikeRequest request = new CreateFeedLikeRequest(false);

                Member member = Member.builder().memberId(userId).build();
                Feed feed = BDDMockito.mock(Feed.class);
                FeedLike existingLike = FeedLike.create(feed, member);

                given(UserContextHolder.getUserId()).willReturn(userId);
                given(feedRepository.findByIdAndDeletedAtNull(feedId)).willReturn(Optional.of(feed));
                given(memberRepository.findByMemberIdAndMemberDeletedAtNull(userId)).willReturn(Optional.of(member));
                given(feedLikeRepository.findByFeedAndMember(feed, member))
                        .willReturn(Optional.of(existingLike));

                // when
                feedLikeCommandService.createFeedLike(feedId, request);

                // then
                then(feedLikeRepository).should(times(1)).delete(existingLike);
            }
        }

        @Test
        @DisplayName("피드가 존재하지 않으면 예외를 던진다")
        void createFeedLike_fail_feedNotFound() {
            try (MockedStatic<UserContextHolder> ignored = BDDMockito.mockStatic(UserContextHolder.class)) {
                // given
                Long feedId = 1L;
                CreateFeedLikeRequest request = new CreateFeedLikeRequest(true);

                given(UserContextHolder.getUserId()).willReturn(UUID.randomUUID());
                given(feedRepository.findByIdAndDeletedAtNull(feedId)).willReturn(Optional.empty());

                // when & then
                assertThatThrownBy(() -> feedLikeCommandService.createFeedLike(feedId, request))
                        .isInstanceOf(CustomException.class)
                        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FEED_NOT_FOUND);
            }
        }

        @Test
        @DisplayName("회원이 존재하지 않으면 예외를 던진다")
        void createFeedLike_fail_memberNotFound() {
            try (MockedStatic<UserContextHolder> ignored = BDDMockito.mockStatic(UserContextHolder.class)) {
                // given
                Long feedId = 1L;
                UUID userId = UUID.randomUUID();
                CreateFeedLikeRequest request = new CreateFeedLikeRequest(true);

                Member member = Member.builder().memberId(userId).build();
                Feed feed = BDDMockito.mock(Feed.class);

                given(UserContextHolder.getUserId()).willReturn(userId);
                given(feedRepository.findByIdAndDeletedAtNull(feedId)).willReturn(Optional.of(feed));
                given(memberRepository.findByMemberIdAndMemberDeletedAtNull(userId)).willReturn(Optional.empty());

                // when & then
                assertThatThrownBy(() -> feedLikeCommandService.createFeedLike(feedId, request))
                        .isInstanceOf(CustomException.class)
                        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MEMBER_NOT_FOUND);
            }
        }
    }
}