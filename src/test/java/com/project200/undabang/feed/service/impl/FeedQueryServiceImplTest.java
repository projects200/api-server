package com.project200.undabang.feed.service.impl;

import com.project200.undabang.common.context.UserContextHolder;
import com.project200.undabang.common.web.exception.CustomException;
import com.project200.undabang.common.web.exception.ErrorCode;
import com.project200.undabang.feed.dto.response.FeedDetailResponse;
import com.project200.undabang.feed.dto.response.GetAllMemberFeedsResponse;
import com.project200.undabang.feed.repository.FeedRepository;
import com.project200.undabang.member.entity.Member;
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
}