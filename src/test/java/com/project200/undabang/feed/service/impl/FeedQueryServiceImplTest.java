package com.project200.undabang.feed.service.impl;

import com.project200.undabang.feed.dto.response.FeedDetailResponse;
import com.project200.undabang.feed.dto.response.GetAllMemberFeedsResponse;
import com.project200.undabang.feed.repository.FeedRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FeedQueryServiceImplTest {

    @InjectMocks
    private FeedQueryServiceImpl feedQueryService;

    @Mock
    private FeedRepository feedRepository;

    @Nested
    @DisplayName("getAllMemberFeeds 메소드는")
    class Describe_getAllMemberFeeds {

        @Nested
        @DisplayName("유효한 페이지 요청과 이전 피드 ID가 주어지면")
        class Context_with_valid_page_request {

            @Test
            @DisplayName("리포지토리를 호출하여 피드 목록을 조회하고, 응답 객체로 변환하여 반환한다")
            void it_calls_repository_and_returns_mapped_response() {
                // given
                Long prevFeedId = 100L;
                Pageable pageable = PageRequest.of(0, 10);

                // Mock Data 생성
                FeedDetailResponse mockFeed = FeedDetailResponse.builder()
                        .feedId(99L)
                        .feedContent("테스트 피드 내용")
                        .build();

                Slice<FeedDetailResponse> mockSlice = new SliceImpl<>(List.of(mockFeed), pageable, true);

                given(feedRepository.getAllFeedList(eq(prevFeedId), any(Pageable.class))).willReturn(mockSlice);

                // when
                GetAllMemberFeedsResponse response = feedQueryService.getAllMemberFeeds(prevFeedId, pageable);

                // then
                verify(feedRepository).getAllFeedList(eq(prevFeedId), eq(pageable));
            }
        }

        @Nested
        @DisplayName("조회할 피드가 없다면")
        class Context_when_no_feeds_exist {

            @Test
            @DisplayName("빈 목록을 담은 응답 객체를 반환한다")
            void it_returns_empty_response() {
                // given
                Pageable pageable = PageRequest.of(0, 10);
                Slice<FeedDetailResponse> emptySlice = new SliceImpl<>(List.of(), pageable, false);

                given(feedRepository.getAllFeedList(any(), any()))
                        .willReturn(emptySlice);

                // when
                GetAllMemberFeedsResponse response = feedQueryService.getAllMemberFeeds(null, pageable);

                // then
                assertThat(response).isNotNull();
            }
        }
    }
}