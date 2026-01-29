package com.project200.undabang.feed.service.impl;

import com.project200.undabang.common.context.UserContextHolder;
import com.project200.undabang.common.web.exception.CustomException;
import com.project200.undabang.common.web.exception.ErrorCode;
import com.project200.undabang.feed.dto.request.CreateFeedRequest;
import com.project200.undabang.feed.dto.response.CreateFeedResponse;
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
    @DisplayName("createMemberFeed 메소드는")
    class Describe_createMemberFeed {

        @Nested
        @DisplayName("유효한 요청 정보가 주어지면")
        class Context_with_valid_request {

            @Test
            @DisplayName("피드를 생성하고 저장된 피드의 ID를 반환한다")
            void it_saves_feed_and_returns_id() {
                // given
                UUID userId = UUID.randomUUID();
                Long feedTypeId = 1L;
                CreateFeedRequest request = new CreateFeedRequest("운동 완료!", feedTypeId);

                Member mockMember = createMember(userId);
                FeedType mockType = createFeedType(feedTypeId);
                Feed savedFeed = createFeedWithId(100L);

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
            @DisplayName("피드 타입 ID가 없어도(null) 정상적으로 피드를 생성한다")
            void it_saves_feed_without_type_when_id_is_null() {
                // given
                UUID userId = UUID.randomUUID();
                CreateFeedRequest request = new CreateFeedRequest("카테고리 없는 글", null);

                try (MockedStatic<UserContextHolder> ignored = mockStatic(UserContextHolder.class)) {
                    given(UserContextHolder.getUserId()).willReturn(userId);
                    given(memberRepository.findById(userId)).willReturn(Optional.of(createMember(userId)));
                    given(feedRepository.save(any(Feed.class))).willReturn(createFeedWithId(101L));

                    // when
                    feedCommandService.createMemberFeed(request);

                    // then
                    verify(feedTypeRepository, never()).findById(any());
                    verify(feedRepository).save(argThat(feed -> feed.getFeedType() == null));
                }
            }
        }

        @Nested
        @DisplayName("존재하지 않는 회원 ID가 주어지면")
        class Context_when_member_not_found {

            @Test
            @DisplayName("MEMBER_NOT_FOUND 예외를 던진다")
            void it_throws_member_not_found_exception() {
                // given
                UUID userId = UUID.randomUUID();
                CreateFeedRequest request = new CreateFeedRequest("내용", null);

                try (MockedStatic<UserContextHolder> ignored = mockStatic(UserContextHolder.class)) {
                    given(UserContextHolder.getUserId()).willReturn(userId);
                    given(memberRepository.findById(userId)).willReturn(Optional.empty());

                    // when & then
                    assertThatThrownBy(() -> feedCommandService.createMemberFeed(request))
                            .isInstanceOf(CustomException.class)
                            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MEMBER_NOT_FOUND);
                }
            }
        }

        @Nested
        @DisplayName("피드 타입 ID가 존재하지만 실제 데이터가 없는 경우")
        class Context_when_feed_type_not_found {

            @Test
            @DisplayName("FEED_TYPE_NOT_FOUND 예외를 던진다")
            void it_throws_feed_type_not_found_exception() {
                // given
                UUID userId = UUID.randomUUID();
                Long invalidTypeId = 999L;
                CreateFeedRequest request = new CreateFeedRequest("내용", invalidTypeId);

                try (MockedStatic<UserContextHolder> ignored = mockStatic(UserContextHolder.class)) {
                    given(UserContextHolder.getUserId()).willReturn(userId);
                    given(memberRepository.findById(userId)).willReturn(Optional.of(createMember(userId)));
                    given(feedTypeRepository.findById(invalidTypeId)).willReturn(Optional.empty());

                    // when & then
                    assertThatThrownBy(() -> feedCommandService.createMemberFeed(request))
                            .isInstanceOf(CustomException.class)
                            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FEED_TYPE_NOT_FOUND);
                }
            }
        }
    }

    private Member createMember(UUID userId) {
        return Member.builder()
                .memberId(userId)
                .memberEmail(userId.toString() + "@test.com")
                .memberNickname("테스터_" + userId.toString().substring(0, 8))
                .memberGender(MemberGender.MALE)
                .memberBday(LocalDate.of(1995, 1, 1))
                .memberScore((byte) 0)
                .memberWarnedCount((byte) 0)
                .memberCreatedAt(LocalDateTime.now())
                .build();
    }

    private FeedType createFeedType(Long typeId) {
        return FeedType.builder()
                .feedTypeId(typeId)
                .feedTypeName("테스트 카테고리")
                .feedTypeDesc("테스트용 카테고리 설명입니다.")
                .feedTypeIsActive(true)
                .createdAt(LocalDateTime.now())
                .build();
    }

    private Feed createFeedWithId(Long feedId) {
        Member dummyMember = Member.builder()
                .memberId(UUID.randomUUID())
                .memberEmail("author@test.com")
                .memberNickname("작성자")
                .build();

        return Feed.builder()
                .id(feedId)
                .member(dummyMember)
                .feedContent("테스트 피드 내용입니다.")
                .likesCount(0)
                .commentsCount(0)
                .createdAt(LocalDateTime.now())
                .build();
    }
}