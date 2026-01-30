package com.project200.undabang.feed.service.impl;

import com.project200.undabang.common.context.UserContextHolder;
import com.project200.undabang.common.web.exception.CustomException;
import com.project200.undabang.common.web.exception.ErrorCode;
import com.project200.undabang.feed.dto.request.CreateFeedRequest;
import com.project200.undabang.feed.dto.request.UpdateFeedRequest;
import com.project200.undabang.feed.dto.response.CreateFeedResponse;
import com.project200.undabang.feed.dto.response.UpdateFeedResponse;
import com.project200.undabang.feed.entity.Feed;
import com.project200.undabang.feed.entity.FeedType;
import com.project200.undabang.feed.repository.FeedRepository;
import com.project200.undabang.feed.repository.FeedTypeRepository;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
@ExtendWith(MockitoExtension.class)
class FeedCommandServiceImplTest {

    @InjectMocks
    private FeedCommandServiceImpl feedCommandService;

    @Mock
    private FeedRepository feedRepository;
    @Mock
    private FeedTypeRepository feedTypeRepository;
    @Mock
    private MemberRepository memberRepository;

    @Nested
    @DisplayName("updateMemberFeed 메소드는")
    class Describe_updateMemberFeed {

        @Test
        @DisplayName("유효한 수정 요청이 주어지면 피드 내용과 타입을 변경하고 응답을 반환한다")
        void it_updates_feed_content_and_type() {
            // given
            UUID userId = UUID.randomUUID();
            Long feedId = 100L;
            Long newTypeId = 2L;
            UpdateFeedRequest request = new UpdateFeedRequest("수정된 내용", newTypeId);

            Member member = createMember(userId);
            FeedType newType = createFeedType(newTypeId, "러닝다방");
            Feed existingFeed = createFeed(feedId, member, createFeedType(1L, "수영다방"));

            try (MockedStatic<UserContextHolder> ignored = mockStatic(UserContextHolder.class)) {
                given(UserContextHolder.getUserId()).willReturn(userId);
                given(memberRepository.findById(userId)).willReturn(Optional.of(member));
                given(feedRepository.findByIdAndMemberAndDeletedAtNull(feedId, member)).willReturn(Optional.of(existingFeed));
                given(feedTypeRepository.findById(newTypeId)).willReturn(Optional.of(newType));

                // when
                UpdateFeedResponse response = feedCommandService.updateMemberFeed(feedId, request);

                // then
                assertThat(response.getFeedContent()).isEqualTo("수정된 내용");
                assertThat(response.getFeedTypeId()).isEqualTo(newTypeId);
                assertThat(response.getFeedTypeName()).isEqualTo("러닝다방");
            }
        }

        @Test
        @DisplayName("피드 타입 ID가 null이면 카테고리를 null로 업데이트한다")
        void it_updates_feed_and_removes_type() {
            // given
            UUID userId = UUID.randomUUID();
            Long feedId = 100L;
            UpdateFeedRequest request = new UpdateFeedRequest("내용만 수정", null);

            Member member = createMember(userId);
            Feed existingFeed = createFeed(feedId, member, createFeedType(1L, "기존타입"));

            try (MockedStatic<UserContextHolder> ignored = mockStatic(UserContextHolder.class)) {
                given(UserContextHolder.getUserId()).willReturn(userId);
                given(memberRepository.findById(userId)).willReturn(Optional.of(member));
                given(feedRepository.findByIdAndMemberAndDeletedAtNull(feedId, member)).willReturn(Optional.of(existingFeed));

                // when
                UpdateFeedResponse response = feedCommandService.updateMemberFeed(feedId, request);

                // then
                assertThat(response.getFeedTypeId()).isNull();
                verify(feedTypeRepository, never()).findById(any());
            }
        }

        @Test
        @DisplayName("존재하지 않는 회원 ID인 경우 MEMBER_NOT_FOUND 예외를 던진다")
        void it_throws_member_not_found_when_updating() {
            UUID userId = UUID.randomUUID();
            try (MockedStatic<UserContextHolder> ignored = mockStatic(UserContextHolder.class)) {
                given(UserContextHolder.getUserId()).willReturn(userId);
                given(memberRepository.findById(userId)).willReturn(Optional.empty());

                assertThatThrownBy(() -> feedCommandService.updateMemberFeed(1L, new UpdateFeedRequest("내용", null)))
                        .isInstanceOf(CustomException.class)
                        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MEMBER_NOT_FOUND);
            }
        }

        @Test
        @DisplayName("자신의 피드가 아니거나 삭제된 경우 FEED_NOT_FOUND 예외를 던진다")
        void it_throws_feed_not_found_when_updating() {
            UUID userId = UUID.randomUUID();
            Member member = createMember(userId);
            try (MockedStatic<UserContextHolder> ignored = mockStatic(UserContextHolder.class)) {
                given(UserContextHolder.getUserId()).willReturn(userId);
                given(memberRepository.findById(userId)).willReturn(Optional.of(member));
                given(feedRepository.findByIdAndMemberAndDeletedAtNull(anyLong(), eq(member))).willReturn(Optional.empty());

                assertThatThrownBy(() -> feedCommandService.updateMemberFeed(999L, new UpdateFeedRequest("내용", null)))
                        .isInstanceOf(CustomException.class)
                        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FEED_NOT_FOUND);
            }
        }

        @Test
        @DisplayName("수정하려는 피드 타입이 존재하지 않으면 FEED_TYPE_NOT_FOUND 예외를 던진다")
        void it_throws_feed_type_not_found_when_updating() {
            UUID userId = UUID.randomUUID();
            Long invalidTypeId = 999L;
            Member member = createMember(userId);
            Feed existingFeed = createFeed(1L, member, null);

            try (MockedStatic<UserContextHolder> ignored = mockStatic(UserContextHolder.class)) {
                given(UserContextHolder.getUserId()).willReturn(userId);
                given(memberRepository.findById(userId)).willReturn(Optional.of(member));
                given(feedRepository.findByIdAndMemberAndDeletedAtNull(anyLong(), eq(member))).willReturn(Optional.of(existingFeed));
                given(feedTypeRepository.findById(invalidTypeId)).willReturn(Optional.empty());

                assertThatThrownBy(() -> feedCommandService.updateMemberFeed(1L, new UpdateFeedRequest("내용", invalidTypeId)))
                        .isInstanceOf(CustomException.class)
                        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FEED_TYPE_NOT_FOUND);
            }
        }
    }

    @Nested
    @DisplayName("createMemberFeed 메소드는")
    class Describe_createMemberFeed {

        @Test
        @DisplayName("유효한 요청 정보가 주어지면 피드를 생성하고 ID를 반환한다")
        void it_saves_feed_and_returns_id() {
            // given
            UUID userId = UUID.randomUUID();
            Long feedTypeId = 1L;
            CreateFeedRequest request = new CreateFeedRequest("운동 완료!", feedTypeId);

            Member mockMember = createMember(userId);
            FeedType mockType = createFeedType(feedTypeId, "카테고리");
            Feed savedFeed = createFeed(100L, mockMember, mockType);

            try (MockedStatic<UserContextHolder> ignored = mockStatic(UserContextHolder.class)) {
                given(UserContextHolder.getUserId()).willReturn(userId);
                given(memberRepository.findById(userId)).willReturn(Optional.of(mockMember));
                given(feedTypeRepository.findById(feedTypeId)).willReturn(Optional.of(mockType));
                given(feedRepository.save(any(Feed.class))).willReturn(savedFeed);

                // when
                CreateFeedResponse result = feedCommandService.createMemberFeed(request);

                // then
                assertThat(result.getFeedId()).isEqualTo(100L);
                verify(feedRepository).save(any(Feed.class));
            }
        }

        @Test
        @DisplayName("피드 타입 ID가 null이어도 정상적으로 피드를 생성한다")
        void it_saves_feed_without_type_when_id_is_null() {
            // given
            UUID userId = UUID.randomUUID();
            CreateFeedRequest request = new CreateFeedRequest("카테고리 없는 글", null);

            try (MockedStatic<UserContextHolder> ignored = mockStatic(UserContextHolder.class)) {
                given(UserContextHolder.getUserId()).willReturn(userId);
                given(memberRepository.findById(userId)).willReturn(Optional.of(createMember(userId)));
                given(feedRepository.save(any(Feed.class))).willReturn(createFeed(101L, createMember(userId), null));

                // when
                feedCommandService.createMemberFeed(request);

                // then
                verify(feedTypeRepository, never()).findById(any());
                verify(feedRepository).save(argThat(feed -> feed.getFeedType() == null));
            }
        }

        @Test
        @DisplayName("회원 ID가 존재하지 않으면 MEMBER_NOT_FOUND 예외를 던진다")
        void it_throws_member_not_found_when_creating() {
            UUID userId = UUID.randomUUID();
            try (MockedStatic<UserContextHolder> ignored = mockStatic(UserContextHolder.class)) {
                given(UserContextHolder.getUserId()).willReturn(userId);
                given(memberRepository.findById(userId)).willReturn(Optional.empty());

                assertThatThrownBy(() -> feedCommandService.createMemberFeed(new CreateFeedRequest("내용", null)))
                        .isInstanceOf(CustomException.class)
                        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MEMBER_NOT_FOUND);
            }
        }

        @Test
        @DisplayName("입력된 피드 타입이 존재하지 않으면 FEED_TYPE_NOT_FOUND 예외를 던진다")
        void it_throws_feed_type_not_found_when_creating() {
            UUID userId = UUID.randomUUID();
            Long invalidTypeId = 999L;
            try (MockedStatic<UserContextHolder> ignored = mockStatic(UserContextHolder.class)) {
                given(UserContextHolder.getUserId()).willReturn(userId);
                given(memberRepository.findById(userId)).willReturn(Optional.of(createMember(userId)));
                given(feedTypeRepository.findById(invalidTypeId)).willReturn(Optional.empty());

                assertThatThrownBy(() -> feedCommandService.createMemberFeed(new CreateFeedRequest("내용", invalidTypeId)))
                        .isInstanceOf(CustomException.class)
                        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FEED_TYPE_NOT_FOUND);
            }
        }
    }

    // --- Helper Methods ---

    private Member createMember(UUID userId) {
        return Member.builder()
                .memberId(userId)
                .memberEmail(userId + "@test.com")
                .memberNickname("테스터_" + userId.toString().substring(0, 8))
                .memberGender(MemberGender.MALE)
                .memberBday(LocalDate.of(1995, 1, 1))
                .memberScore((byte) 0)
                .memberWarnedCount((byte) 0)
                .memberCreatedAt(LocalDateTime.now())
                .build();
    }

    private FeedType createFeedType(Long id, String name) {
        return FeedType.builder()
                .feedTypeId(id)
                .feedTypeName(name)
                .feedTypeDesc(name + " 설명")
                .feedTypeIsActive(true)
                .createdAt(LocalDateTime.now())
                .build();
    }

    private Feed createFeed(Long id, Member member, FeedType type) {
        return Feed.builder()
                .id(id)
                .member(member)
                .feedType(type)
                .feedContent("피드 내용")
                .likesCount(0)
                .commentsCount(0)
                .createdAt(LocalDateTime.now())
                .build();
    }
}