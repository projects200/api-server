package com.project200.undabang.feed.controller;

import com.project200.undabang.configuration.AbstractRestDocSupport;
import com.project200.undabang.feed.dto.record.FeedPictureRecord;
import com.project200.undabang.feed.dto.record.GetMyPageFeedsRecord;
import com.project200.undabang.feed.dto.response.FeedDetailResponse;
import com.project200.undabang.feed.dto.response.GetAllMemberFeedsResponse;
import com.project200.undabang.feed.dto.response.GetMyPageFeedsResponse;
import com.project200.undabang.feed.dto.response.GetSpecificFeedResponse;
import com.project200.undabang.feed.service.FeedQueryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.BDDMockito;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static com.project200.undabang.configuration.DocumentFormatGenerator.getTypeFormat;
import static com.project200.undabang.configuration.HeadersGenerator.getCommonApiHeaders;
import static com.project200.undabang.configuration.RestDocsUtils.HEADER_ACCESS_TOKEN;
import static com.project200.undabang.configuration.RestDocsUtils.commonResponseFields;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.payload.JsonFieldType.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FeedQueryController.class)
class FeedQueryControllerTest extends AbstractRestDocSupport {

    @MockitoBean
    private FeedQueryService feedQueryService;

    @Nested
    @DisplayName("GET /api/v1/feeds/{feedId} API는")
    class GetSpecificFeed {

        @Test
        @DisplayName("특정 피드의 상세 정보를 조회한다")
        void getSpecificFeed_Success() throws Exception {
            // given
            UUID memberId = UUID.randomUUID();
            Long feedId = 100L;

            List<FeedPictureRecord> pictures = List.of(
                    new FeedPictureRecord(501L, "https://example.com/img1.jpg"),
                    new FeedPictureRecord(502L, "https://example.com/img2.jpg")
            );

            GetSpecificFeedResponse response = GetSpecificFeedResponse.builder()
                    .feedId(feedId)
                    .feedContent("피드 상세 조회 내용입니다.")
                    .feedLikesCount(123)
                    .feedCommentsCount(12)
                    .feedTypeId(2L)
                    .feedTypeName("러닝다방")
                    .feedTypeDesc("러닝다방 설명")
                    .feedIsLiked(true)         // 내가 좋아요 눌렀는지
                    .feedHasCommented(false)   // 내가 댓글 달았는지
                    .memberId(UUID.randomUUID())
                    .nickname("러닝고수")
                    .profileUrl("https://example.com/profile.jpg")
                    .thumbnailUrl("https://example.com/thumb.jpg")
                    .feedCreatedAt(LocalDateTime.now())
                    .feedPictures(pictures)
                    .build();

            BDDMockito.given(feedQueryService.getSpecificFeed(eq(feedId))).willReturn(response);

            // when & then
            mockMvc.perform(RestDocumentationRequestBuilders.get("/api/v1/feeds/{feedId}", feedId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .accept(MediaType.APPLICATION_JSON)
                            .headers(getCommonApiHeaders(memberId)))
                    .andExpectAll(
                            status().isOk(),
                            jsonPath("$.succeed").value(true),
                            jsonPath("$.code").value("SUCCESS"),
                            jsonPath("$.data.feedId").value(feedId),
                            jsonPath("$.data.nickname").value("러닝고수"),
                            jsonPath("$.data.feedPictures").isArray(),
                            jsonPath("$.data.feedPictures[0].feedPictureUrl").value("https://example.com/img1.jpg")
                    )
                    .andDo(document.document(
                            requestHeaders(HEADER_ACCESS_TOKEN),
                            pathParameters(
                                    parameterWithName("feedId").attributes(getTypeFormat(NUMBER)).description("조회할 피드의 고유 식별자(ID)입니다.")
                            ),
                            responseFields(commonResponseFields(
                                    fieldWithPath("data.feedId").type(NUMBER).description("피드 식별자를 의미합니다."),
                                    fieldWithPath("data.feedContent").type(STRING).description("피드 내용을 의미합니다."),
                                    fieldWithPath("data.feedLikesCount").type(NUMBER).description("좋아요 수를 의미합니다."),
                                    fieldWithPath("data.feedCommentsCount").type(NUMBER).description("댓글 수를 의미합니다."),
                                    fieldWithPath("data.feedTypeId").type(NUMBER).description("피드 타입 식별자를 의미합니다."),
                                    fieldWithPath("data.feedTypeName").type(STRING).description("피드 타입 이름을 의미합니다."),
                                    fieldWithPath("data.feedTypeDesc").type(STRING).description("피드 타입 설명을 의미합니다."),
                                    fieldWithPath("data.feedIsLiked").type(BOOLEAN).description("해당 회원이 피드에 좋아요를 누른 여부를 반환합니다."),
                                    fieldWithPath("data.feedHasCommented").type(BOOLEAN).description("해당 회원이 피드에 댓글을 작성하였는지 여부를 반환합니다."),
                                    fieldWithPath("data.memberId").type(STRING).description("작성자 회원 식별자를 의미합니다."),
                                    fieldWithPath("data.nickname").type(STRING).description("작성자 닉네임을 의미합니다."),
                                    fieldWithPath("data.profileUrl").type(STRING).description("작성자 프로필 URL을 의미합니다.").optional(),
                                    fieldWithPath("data.thumbnailUrl").type(STRING).description("작성자 썸네일 URL을 의미합니다.").optional(),
                                    fieldWithPath("data.feedCreatedAt").type(STRING).description("작성일시 정보를 나타냅니다."),
                                    fieldWithPath("data.feedPictures").type(ARRAY).description("피드 사진 목록을 의미합니다."),
                                    fieldWithPath("data.feedPictures[].feedPictureId").type(NUMBER).description("피드 사진 식별자를 의미합니다."),
                                    fieldWithPath("data.feedPictures[].feedPictureUrl").type(STRING).description("피드 사진 URL을 의미합니다.")
                            ))
                    ));

            BDDMockito.then(feedQueryService).should(BDDMockito.times(1)).getSpecificFeed(eq(feedId));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/feeds API는")
    class GetAllMemberFeeds {

        @Test
        @DisplayName("모든 회원의 피드 목록을 무한 스크롤(Slice) 방식으로 조회한다")
        void getAllMemberFeeds_Success() throws Exception {
            // given
            UUID memberId = UUID.randomUUID();
            Long prevFeedId = 100L;
            int pageSize = 10;

            List<FeedPictureRecord> pictures = List.of(
                    new FeedPictureRecord(501L, "https://example.com/img1.jpg")
            );

            FeedDetailResponse feedDetail = FeedDetailResponse.builder()
                    .feedId(99L)
                    .feedContent("테스트 피드입니다.")
                    .feedLikesCount(10)
                    .feedCommentsCount(2)
                    .feedTypeId(1L)
                    .feedTypeName("수영다방")
                    .feedTypeDesc("수영다방 피드에 대한 설명")
                    .feedIsLiked(true)
                    .feedHasCommented(false)
                    .memberId(UUID.randomUUID())
                    .nickname("수영왕")
                    .profileUrl("https://example.com/profile.jpg")
                    .thumbnailUrl("https://example.com/thumb.jpg")
                    .feedCreatedAt(LocalDateTime.now())
                    .feedPictures(pictures)
                    .build();

            GetAllMemberFeedsResponse response = GetAllMemberFeedsResponse.builder()
                    .feeds(List.of(feedDetail))   // 필드명: feeds
                    .hasNext(true)                // 필드명: hasNext
                    .build();

            BDDMockito.given(feedQueryService.getAllMemberFeeds(eq(prevFeedId), any(Pageable.class)))
                    .willReturn(response);

            // when & then
            mockMvc.perform(get("/api/v1/feeds")
                            .param("prevFeedId", String.valueOf(prevFeedId))
                            .param("size", String.valueOf(pageSize))
                            .contentType(MediaType.APPLICATION_JSON)
                            .accept(MediaType.APPLICATION_JSON)
                            .headers(getCommonApiHeaders(memberId)))
                    .andExpectAll(
                            status().isOk(),
                            jsonPath("$.succeed").value(true),
                            jsonPath("$.code").value("SUCCESS"),
                            jsonPath("$.data.hasNext").value(true),
                            jsonPath("$.data.feeds").isArray(),
                            jsonPath("$.data.feeds[0].feedId").value(99L),
                            jsonPath("$.data.feeds[0].nickname").value("수영왕")
                    )
                    .andDo(document.document(
                            requestHeaders(HEADER_ACCESS_TOKEN),
                            queryParameters(
                                    parameterWithName("prevFeedId").attributes(getTypeFormat(JsonFieldType.NUMBER)).description("이전 페이지의 마지막 피드 식별자를 의미합니다.").optional(),
                                    parameterWithName("size").attributes(getTypeFormat(JsonFieldType.NUMBER)).description("페이지 크기를 의미합니다. 기본값은 10 이지만, 입력하지 않아도 됩니다.").optional()
                            ),
                            responseFields(commonResponseFields(
                                    fieldWithPath("data.hasNext").type(BOOLEAN).description("다음 페이지 존재 여부를 의미합니다."),
                                    fieldWithPath("data.feeds").type(ARRAY).description("조회된 피드 목록 리스트 입니다."),
                                    fieldWithPath("data.feeds[].feedId").type(NUMBER).description("피드 식별자를 의미합니다."),
                                    fieldWithPath("data.feeds[].feedContent").type(STRING).description("피드 내용을 의미합니다."),
                                    fieldWithPath("data.feeds[].feedLikesCount").type(NUMBER).description("좋아요 수를 의미합니다."),
                                    fieldWithPath("data.feeds[].feedCommentsCount").type(NUMBER).description("댓글 수를 의미합니다."),
                                    fieldWithPath("data.feeds[].feedTypeId").type(NUMBER).description("피드 타입 식별자를 의미합니다."),
                                    fieldWithPath("data.feeds[].feedTypeName").type(STRING).description("피드 타입 이름을 의미합니다."),
                                    fieldWithPath("data.feeds[].feedTypeDesc").type(STRING).description("피드 타입 설명을 의미합니다."),
                                    fieldWithPath("data.feeds[].feedIsLiked").type(BOOLEAN).description("해당 회원이 피드에 좋아요를 누른 여부를 반환합니다."),
                                    fieldWithPath("data.feeds[].feedHasCommented").type(BOOLEAN).description("해당 회원이 피드에 댓글을 작성하였는지 여부를 반환합니다."),
                                    fieldWithPath("data.feeds[].memberId").type(STRING).description("작성자 회원 식별자를 의미합니다."),
                                    fieldWithPath("data.feeds[].nickname").type(STRING).description("작성자 닉네임을 의미합니다."),
                                    fieldWithPath("data.feeds[].profileUrl").type(STRING).description("작성자 프로필 URL을 의미합니다.").optional(),
                                    fieldWithPath("data.feeds[].thumbnailUrl").type(STRING).description("작성자 썸네일 URL을 의미합니다.").optional(),
                                    fieldWithPath("data.feeds[].feedCreatedAt").type(STRING).description("작성일시 정보를 나타냅니다."),
                                    fieldWithPath("data.feeds[].feedPictures").type(ARRAY).description("피드 사진 목록을 의미합니다."),
                                    fieldWithPath("data.feeds[].feedPictures[].feedPictureId").type(NUMBER).description("피드 사진 식별자를 의미합니다."),
                                    fieldWithPath("data.feeds[].feedPictures[].feedPictureUrl").type(STRING).description("피드 사진 URL을 의미합니다.")
                            ))
                    ));

            BDDMockito.then(feedQueryService).should(BDDMockito.times(1)).getAllMemberFeeds(eq(prevFeedId), any(Pageable.class));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/mypage/feeds API는")
    class GetMyPageFeeds {

        @Test
        @DisplayName("현재 사용자가 작성한 피드 목록을 프로필 정보와 함께 조회한다")
        void getMyPageFeeds_Success() throws Exception {
            // given
            UUID memberId = UUID.randomUUID();
            Long prevFeedId = 50L;
            int pageSize = 10;

            List<FeedPictureRecord> pictures = List.of(
                    new FeedPictureRecord(1001L, "https://example.com/my_feed_img.jpg")
            );

            // 마이페이지 전용 피드 레코드 생성
            GetMyPageFeedsRecord myFeed = GetMyPageFeedsRecord.builder()
                    .feedId(49L)
                    .feedContent("내가 작성한 피드 내용입니다.")
                    .feedLikesCount(50)
                    .feedCommentsCount(5)
                    .feedTypeId(1L)
                    .feedTypeName("수영다방")
                    .feedTypeDesc("수영다방 설명")
                    .feedCreatedAt(LocalDateTime.now())
                    .feedIsLiked(true)
                    .feedHasCommented(true)
                    .feedPictures(pictures)
                    .build();

            // 최종 응답 객체 (헤더 정보 + 피드 목록)
            GetMyPageFeedsResponse response = GetMyPageFeedsResponse.builder()
                    .memberId(memberId)
                    .nickname("운동하는개발자")
                    .thumbnailUrl("https://example.com/my_thumb.jpg")
                    .profileUrl("https://example.com/my_profile.jpg")
                    .hasNext(false)
                    .feeds(List.of(myFeed))
                    .build();

            BDDMockito.given(feedQueryService.getMyPageFeeds(eq(prevFeedId), any(Pageable.class)))
                    .willReturn(response);

            // when & then
            mockMvc.perform(RestDocumentationRequestBuilders.get("/api/v1/mypage/feeds")
                            .param("prevFeedId", String.valueOf(prevFeedId))
                            .param("size", String.valueOf(pageSize))
                            .contentType(MediaType.APPLICATION_JSON)
                            .accept(MediaType.APPLICATION_JSON)
                            .headers(getCommonApiHeaders(memberId)))
                    .andExpectAll(
                            status().isOk(),
                            jsonPath("$.succeed").value(true),
                            jsonPath("$.data.memberId").value(memberId.toString()),
                            jsonPath("$.data.nickname").value("운동하는개발자"),
                            jsonPath("$.data.feeds").isArray(),
                            jsonPath("$.data.feeds[0].feedContent").value("내가 작성한 피드 내용입니다.")
                    )
                    .andDo(document.document(
                            requestHeaders(HEADER_ACCESS_TOKEN),
                            queryParameters(
                                    parameterWithName("prevFeedId").attributes(getTypeFormat(NUMBER)).description("이전 페이지의 마지막 피드 식별자입니다.").optional(),
                                    parameterWithName("size").attributes(getTypeFormat(NUMBER)).description("조회할 페이지 크기입니다.").optional()
                            ),
                            responseFields(commonResponseFields(
                                    fieldWithPath("data.memberId").type(STRING).description("조회 중인 회원(본인)의 식별자 정보 (UUID) 입니다."),
                                    fieldWithPath("data.nickname").type(STRING).description("회원의 닉네임입니다."),
                                    fieldWithPath("data.thumbnailUrl").type(STRING).description("회원의 썸네일 URL입니다.").optional(),
                                    fieldWithPath("data.profileUrl").type(STRING).description("회원의 프로필 원본 URL입니다.").optional(),
                                    fieldWithPath("data.hasNext").type(BOOLEAN).description("다음 페이지 존재 여부를 반환합니다."),
                                    fieldWithPath("data.feeds").type(ARRAY).description("작성한 피드 목록 리스트입니다."),
                                    fieldWithPath("data.feeds[].feedId").type(NUMBER).description("피드 식별자입니다."),
                                    fieldWithPath("data.feeds[].feedContent").type(STRING).description("피드 내용입니다."),
                                    fieldWithPath("data.feeds[].feedLikesCount").type(NUMBER).description("좋아요 수입니다."),
                                    fieldWithPath("data.feeds[].feedCommentsCount").type(NUMBER).description("댓글 수입니다."),
                                    fieldWithPath("data.feeds[].feedTypeId").type(NUMBER).description("피드 타입 식별자입니다."),
                                    fieldWithPath("data.feeds[].feedTypeName").type(STRING).description("피드 타입 이름입니다."),
                                    fieldWithPath("data.feeds[].feedTypeDesc").type(STRING).description("피드 타입 설명입니다."),
                                    fieldWithPath("data.feeds[].feedCreatedAt").type(STRING).description("피드 작성일시입니다."),
                                    fieldWithPath("data.feeds[].feedIsLiked").type(BOOLEAN).description("내가 이 피드에 좋아요를 눌렀는지 여부입니다."),
                                    fieldWithPath("data.feeds[].feedHasCommented").type(BOOLEAN).description("내가 이 피드에 댓글을 달았는지 여부입니다."),
                                    fieldWithPath("data.feeds[].feedPictures").type(ARRAY).description("피드 사진 목록입니다."),
                                    fieldWithPath("data.feeds[].feedPictures[].feedPictureId").type(NUMBER).description("사진 식별자입니다."),
                                    fieldWithPath("data.feeds[].feedPictures[].feedPictureUrl").type(STRING).description("사진 URL입니다.")
                            ))
                    ));

            BDDMockito.then(feedQueryService).should(BDDMockito.times(1)).getMyPageFeeds(eq(prevFeedId), any(Pageable.class));
        }
    }
}