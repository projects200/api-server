package com.project200.undabang.feed.service.impl;

import com.project200.undabang.common.context.UserContextHolder;
import com.project200.undabang.common.entity.Picture;
import com.project200.undabang.common.web.exception.CustomException;
import com.project200.undabang.common.web.exception.ErrorCode;
import com.project200.undabang.feed.dto.response.FeedDetailResponse;
import com.project200.undabang.feed.dto.response.GetAllMemberFeedsResponse;
import com.project200.undabang.feed.dto.response.GetMyPageFeedsResponse;
import com.project200.undabang.feed.dto.response.GetSpecificFeedResponse;
import com.project200.undabang.feed.repository.FeedRepository;
import com.project200.undabang.member.entity.Member;
import com.project200.undabang.member.entity.MemberPicture;
import com.project200.undabang.member.repository.MemberRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FeedQueryServiceImplTest {

    @InjectMocks
    private FeedQueryServiceImpl feedQueryService;

    @Mock
    private FeedRepository feedRepository;

    @Mock
    private MemberRepository memberRepository;

    @Nested
    @DisplayName("getSpecificFeed 메소드는")
    class Describe_getSpecificFeed {

        @Nested
        @DisplayName("존재하는 회원과 존재하는 피드 ID가 주어지면")
        class Context_with_existing_member_and_feed {

            @Test
            @DisplayName("리포지토리에서 조회한 상세 정보를 반환한다")
            void it_returns_specific_feed_response() {
                // given
                Long feedId = 100L;
                UUID userId = UUID.randomUUID();

                Member mockMember = createMockMember(userId);
                GetSpecificFeedResponse mockResponse = GetSpecificFeedResponse.builder()
                        .feedId(feedId)
                        .feedContent("Detail Content")
                        .build();

                try (MockedStatic<UserContextHolder> ignored = mockStatic(UserContextHolder.class)) {
                    given(UserContextHolder.getUserId()).willReturn(userId);
                    given(memberRepository.findById(userId)).willReturn(Optional.of(mockMember));
                    given(feedRepository.getSpecificFeed(eq(mockMember), eq(feedId)))
                            .willReturn(Optional.of(mockResponse));

                    // when
                    GetSpecificFeedResponse result = feedQueryService.getSpecificFeed(feedId);

                    // then
                    assertThat(result).isNotNull();
                    assertThat(result.getFeedId()).isEqualTo(feedId);
                    assertThat(result.getFeedContent()).isEqualTo("Detail Content");

                    verify(feedRepository).getSpecificFeed(eq(mockMember), eq(feedId));
                }
            }
        }

        @Nested
        @DisplayName("존재하지 않는 회원 ID(로그인 정보 오류 등)인 경우")
        class Context_when_member_not_found {

            @Test
            @DisplayName("MEMBER_NOT_FOUND 예외를 던진다")
            void it_throws_member_not_found_exception() {
                // given
                Long feedId = 100L;
                UUID userId = UUID.randomUUID();

                try (MockedStatic<UserContextHolder> ignored = mockStatic(UserContextHolder.class)) {
                    given(UserContextHolder.getUserId()).willReturn(userId);
                    // 회원 조회 실패 설정
                    given(memberRepository.findById(userId)).willReturn(Optional.empty());

                    // when & then
                    assertThatThrownBy(() -> feedQueryService.getSpecificFeed(feedId))
                            .isInstanceOf(CustomException.class)
                            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MEMBER_NOT_FOUND);

                    verify(feedRepository, never()).getSpecificFeed(any(), any());
                }
            }
        }

        @Nested
        @DisplayName("회원은 존재하지만 피드 ID에 해당하는 데이터가 없는 경우")
        class Context_when_feed_not_found {

            @Test
            @DisplayName("FEED_NOT_FOUND 예외를 던진다")
            void it_throws_feed_not_found_exception() {
                // given
                Long feedId = 999L;
                UUID userId = UUID.randomUUID();
                Member mockMember = createMockMember(userId);

                try (MockedStatic<UserContextHolder> ignored = mockStatic(UserContextHolder.class)) {
                    given(UserContextHolder.getUserId()).willReturn(userId);
                    given(memberRepository.findById(userId)).willReturn(Optional.of(mockMember));

                    // 리포지토리가 빈 Optional 반환
                    given(feedRepository.getSpecificFeed(eq(mockMember), eq(feedId)))
                            .willReturn(Optional.empty());

                    // when & then
                    assertThatThrownBy(() -> feedQueryService.getSpecificFeed(feedId))
                            .isInstanceOf(CustomException.class)
                            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FEED_NOT_FOUND);
                }
            }
        }
    }

    @Nested
    @DisplayName("getAllMemberFeeds 메소드는")
    class Describe_getAllMemberFeeds {

        @Nested
        @DisplayName("유효한 페이지 요청과 이전 피드 ID가 주어지면")
        class Context_with_valid_page_request {

            @Test
            @DisplayName("리포지토리를 호출하고, 조회된 Slice 데이터를 응답 객체에 정확히 매핑하여 반환한다")
            void it_calls_repository_and_returns_mapped_response() {
                // given
                Long prevFeedId = 100L;
                Pageable pageable = PageRequest.of(0, 10);
                UUID userId = UUID.randomUUID();

                Member mockMember = createMockMember(userId);
                FeedDetailResponse mockFeed = createMockFeedResponse(99L, "테스트 피드 내용");
                Slice<FeedDetailResponse> mockSlice = new SliceImpl<>(List.of(mockFeed), pageable, true);

                try (MockedStatic<UserContextHolder> ignored = mockStatic(UserContextHolder.class)) {
                    given(UserContextHolder.getUserId()).willReturn(userId);
                    given(memberRepository.findById(userId)).willReturn(Optional.of(mockMember));
                    given(feedRepository.getAllFeedList(eq(mockMember), eq(prevFeedId), any(Pageable.class)))
                            .willReturn(mockSlice);

                    // when
                    GetAllMemberFeedsResponse response = feedQueryService.getAllMemberFeeds(prevFeedId, pageable);

                    // then
                    verify(feedRepository).getAllFeedList(eq(mockMember), eq(prevFeedId), eq(pageable));

                    assertThat(response).isNotNull();
                    assertThat(response.getFeeds()).hasSize(1);
                    assertThat(response.getFeeds().get(0).getFeedId()).isEqualTo(99L);
                    assertThat(response.isHasNext()).isTrue();
                }
            }
        }

        @Nested
        @DisplayName("조회할 피드가 없다면")
        class Context_when_no_feeds_exist {

            @Test
            @DisplayName("빈 리스트와 hasNext=false가 담긴 응답 객체를 반환한다")
            void it_returns_empty_response() {
                // given
                Pageable pageable = PageRequest.of(0, 10);
                UUID userId = UUID.randomUUID();

                Member mockMember = createMockMember(userId);
                Slice<FeedDetailResponse> emptySlice = new SliceImpl<>(List.of(), pageable, false);

                try (MockedStatic<UserContextHolder> ignored = mockStatic(UserContextHolder.class)) {
                    given(UserContextHolder.getUserId()).willReturn(userId);
                    given(memberRepository.findById(userId)).willReturn(Optional.of(mockMember));
                    given(feedRepository.getAllFeedList(eq(mockMember), any(), any(Pageable.class)))
                            .willReturn(emptySlice);

                    // when
                    GetAllMemberFeedsResponse response = feedQueryService.getAllMemberFeeds(null, pageable);

                    // then
                    assertThat(response).isNotNull();
                    assertThat(response.getFeeds()).isEmpty();
                    assertThat(response.isHasNext()).isFalse();
                }
            }
        }
    }

    @Nested
    @DisplayName("존재하지 않는 회원 ID가 주어지면")
    class Context_when_member_not_found {

        @Test
        @DisplayName("MEMBER_NOT_FOUND 예외를 던진다")
        void it_throws_custom_exception() {
            Long prevFeedId = 100L;
            Pageable pageable = PageRequest.of(0, 10);
            UUID userId = UUID.randomUUID();

            try (MockedStatic<UserContextHolder> ignored = mockStatic(UserContextHolder.class)) {
                given(UserContextHolder.getUserId()).willReturn(userId);
                // 회원을 찾지 못해 Optional.empty() 반환 설정
                given(memberRepository.findById(userId)).willReturn(Optional.empty());

                assertThatThrownBy(() -> feedQueryService.getAllMemberFeeds(prevFeedId, pageable))
                        .isInstanceOf(CustomException.class)
                        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MEMBER_NOT_FOUND);

                // 예외가 발생했으므로 feedRepository는 호출되지 않아야 함
                verify(feedRepository, never()).getAllFeedList(any(), any(), any());
            }
        }
    }

    @Nested
    @DisplayName("getMyPageFeeds 메소드는")
    class Describe_getMyPageFeeds {

        @Nested
        @DisplayName("유효한 회원 ID와 피드 목록이 존재하면")
        class Context_with_valid_member_and_feeds {

            @Test
            @DisplayName("Fetch Join으로 회원 정보를 가져오고, 피드 목록과 결합된 응답을 반환한다")
            void it_returns_combined_mypage_response() {
                // given
                UUID userId = UUID.randomUUID();
                Pageable pageable = PageRequest.of(0, 10);

                // 프로필 사진이 포함된 Mock Member
                Member mockMember = createMockMemberWithProfile(userId);

                // 피드 목록 Mock 데이터
                FeedDetailResponse feedResponse = createMockFeedResponse(100L, "My Page Feed");
                Slice<FeedDetailResponse> mockSlice = new SliceImpl<>(List.of(feedResponse), pageable, true);

                try (MockedStatic<UserContextHolder> ignored = mockStatic(UserContextHolder.class)) {
                    given(UserContextHolder.getUserId()).willReturn(userId);
                    // findById가 아닌 findMemberWithProfileImage를 호출해야 함
                    given(memberRepository.findMemberWithProfileImage(userId)).willReturn(Optional.of(mockMember));
                    given(feedRepository.getMyPageFeedList(eq(mockMember), any(), eq(pageable))).willReturn(mockSlice);

                    // when
                    GetMyPageFeedsResponse result = feedQueryService.getMyPageFeeds(null, pageable);

                    // then
                    assertThat(result.getMemberId()).isEqualTo(userId);
                    assertThat(result.getNickname()).isEqualTo("운동하는개발자");
                    assertThat(result.getProfileUrl()).isEqualTo("https://example.com/profile.jpg"); // Optional 체이닝 결과
                    assertThat(result.getFeeds()).hasSize(1);
                    assertThat(result.isHasNext()).isTrue();

                    verify(memberRepository).findMemberWithProfileImage(userId);
                    verify(feedRepository).getMyPageFeedList(mockMember, null, pageable);
                }
            }
        }

        @Nested
        @DisplayName("회원의 프로필 사진 정보가 없는 경우")
        class Context_with_member_no_profile_picture {

            @Test
            @DisplayName("프로필 관련 URL들을 null로 설정하여 안전하게 응답을 반환한다")
            void it_returns_response_with_null_urls() {
                // given
                UUID userId = UUID.randomUUID();
                Member memberNoPic = Member.builder()
                        .memberId(userId)
                        .memberNickname("사진없는유저")
                        .build(); // memberPicture가 null인 상태

                Slice<FeedDetailResponse> emptySlice = new SliceImpl<>(List.of(), PageRequest.of(0, 10), false);

                try (MockedStatic<UserContextHolder> ignored = mockStatic(UserContextHolder.class)) {
                    given(UserContextHolder.getUserId()).willReturn(userId);
                    given(memberRepository.findMemberWithProfileImage(userId)).willReturn(Optional.of(memberNoPic));
                    given(feedRepository.getMyPageFeedList(any(), any(), any())).willReturn(emptySlice);

                    // when
                    GetMyPageFeedsResponse result = feedQueryService.getMyPageFeeds(null, PageRequest.of(0, 10));

                    // then
                    assertThat(result.getThumbnailUrl()).isNull();
                    assertThat(result.getProfileUrl()).isNull();
                    assertThat(result.getNickname()).isEqualTo("사진없는유저");
                }
            }
        }

        @Nested
        @DisplayName("존재하지 않는 회원 ID가 주어지면")
        class Context_with_non_existing_member {

            @Test
            @DisplayName("MEMBER_NOT_FOUND 예외를 던진다")
            void it_throws_member_not_found_exception() {
                // given
                UUID userId = UUID.randomUUID();

                try (MockedStatic<UserContextHolder> ignored = mockStatic(UserContextHolder.class)) {
                    given(UserContextHolder.getUserId()).willReturn(userId);
                    given(memberRepository.findMemberWithProfileImage(userId)).willReturn(Optional.empty());

                    // when & then
                    assertThatThrownBy(() -> feedQueryService.getMyPageFeeds(null, PageRequest.of(0, 10)))
                            .isInstanceOf(CustomException.class)
                            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MEMBER_NOT_FOUND);
                }
            }
        }
    }

    private Member createMockMember(UUID memberId) {
        return Member.builder()
                .memberId(memberId)
                .build();
    }

    private FeedDetailResponse createMockFeedResponse(Long feedId, String content) {
        return FeedDetailResponse.builder()
                .feedId(feedId)
                .feedContent(content)
                .build();
    }

    private Member createMockMemberWithProfile(UUID userId) {
        Picture picture = Picture.builder()
                .pictureUrl("https://example.com/profile.jpg")
                .build();

        MemberPicture memberPicture = MemberPicture.builder()
                .memberPicturesUrl("https://example.com/thumb.jpg")
                .picture(picture)
                .build();

        return Member.builder()
                .memberId(userId)
                .memberNickname("운동하는개발자")
                .memberPicture(memberPicture)
                .build();
    }
}