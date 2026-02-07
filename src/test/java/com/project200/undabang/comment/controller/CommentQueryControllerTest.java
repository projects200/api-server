package com.project200.undabang.comment.controller;

import com.project200.undabang.comment.dto.response.CommentResponse;
import com.project200.undabang.comment.service.CommentQueryService;
import com.project200.undabang.common.web.exception.CustomException;
import com.project200.undabang.common.web.exception.ErrorCode;
import com.project200.undabang.common.web.response.CommonResponse;
import com.project200.undabang.configuration.AbstractRestDocSupport;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.BDDMockito;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.project200.undabang.configuration.DocumentFormatGenerator.getTypeFormat;
import static com.project200.undabang.configuration.HeadersGenerator.getCommonApiHeaders;
import static com.project200.undabang.configuration.RestDocsUtils.*;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CommentQueryController.class)
@DisplayName("CommentQueryController 테스트")
class CommentQueryControllerTest extends AbstractRestDocSupport {

    @MockitoBean
    private CommentQueryService commentQueryService;

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
        @DisplayName("피드의 댓글 목록 조회를 성공한다")
        void getComments_success() throws Exception {
            // given
            UUID testMemberId = UUID.randomUUID();
            Long feedId = 1L;
            List<CommentResponse> comments = createSampleComments();

            BDDMockito.given(commentQueryService.getComments(feedId)).willReturn(comments);

            // when
            String response = mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/feeds/{feedId}/comments", feedId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .headers(getCommonApiHeaders(testMemberId)))
                    .andExpect(status().isOk())
                    .andDo(document.document(
                            requestHeaders(HEADER_ACCESS_TOKEN),
                            pathParameters(
                                    parameterWithName("feedId").attributes(getTypeFormat(JsonFieldType.NUMBER)).description("댓글을 조회할 피드 ID")),
                            responseFields(commonResponseFieldsForList(
                                    fieldWithPath("data[].commentId").type(JsonFieldType.NUMBER).description("댓글 ID"),
                                    fieldWithPath("data[].memberId").type(JsonFieldType.STRING).description("댓글 작성자의 회원 고유 식별자(ID) 입니다."),
                                    fieldWithPath("data[].memberNickname").type(JsonFieldType.STRING).description("댓글 작성자의 닉네임입니다."),
                                    fieldWithPath("data[].memberProfileImageUrl").type(JsonFieldType.STRING).description("댓글 작성자의 프로필 이미지 URL입니다.").optional(),
                                    fieldWithPath("data[].memberThumbnailUrl").type(JsonFieldType.STRING).description("댓글 작성자 프로필 썸네일 URL입니다.").optional(),
                                    fieldWithPath("data[].content").type(JsonFieldType.STRING).description("댓글 내용입니다."),
                                    fieldWithPath("data[].likesCount").type(JsonFieldType.NUMBER).description("댓글 좋아요 수 입니다."),
                                    fieldWithPath("data[].createdAt").type(JsonFieldType.STRING).description("댓글이 작성된 시간입니다."),
                                    fieldWithPath("data[].children").type(JsonFieldType.ARRAY).description("대댓글 목록입니다."),
                                    fieldWithPath("data[].children[].commentId").type(JsonFieldType.NUMBER).description("대댓글 고유 식별자(ID) 입니다."),
                                    fieldWithPath("data[].children[].memberId").type(JsonFieldType.STRING).description("대댓글 작성자 회원 고유 식별자(ID) 입니다."),
                                    fieldWithPath("data[].children[].memberNickname").type(JsonFieldType.STRING).description("대댓글 작성자 닉네임 입니다."),
                                    fieldWithPath("data[].children[].memberProfileImageUrl").type(JsonFieldType.STRING).description("대댓글 작성자 프로필 이미지 URL").optional(),
                                    fieldWithPath("data[].children[].memberThumbnailUrl").type(JsonFieldType.STRING).description("대댓글 작성자 프로필 썸네일 URL").optional(),
                                    fieldWithPath("data[].children[].content").type(JsonFieldType.STRING).description("대댓글 내용"),
                                    fieldWithPath("data[].children[].likesCount").type(JsonFieldType.NUMBER).description("대댓글 좋아요 수"),
                                    fieldWithPath("data[].children[].createdAt").type(JsonFieldType.STRING).description("대댓글 작성 시간"),
                                    fieldWithPath("data[].children[].children").type(JsonFieldType.ARRAY).description("대댓글의 대댓글 (현재 미지원)")))))
                    .andReturn().getResponse().getContentAsString();

            // then
            CommonResponse<List<CommentResponse>> expectedData = CommonResponse.success(comments);
            String expected = objectMapper.writeValueAsString(expectedData);
            Assertions.assertThat(response).as("응답 본문 검증").isEqualTo(expected);
        }

        @Test
        @DisplayName("존재하지 않는 피드 ID로 조회 시 실패한다")
        void getComments_Failed_FeedNotFound() throws Exception {
            // given
            UUID testMemberId = UUID.randomUUID();
            Long feedId = 999L;

            BDDMockito.given(commentQueryService.getComments(feedId))
                    .willThrow(new CustomException(ErrorCode.FEED_NOT_FOUND));

            // when
            mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/feeds/{feedId}/comments", feedId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .headers(getCommonApiHeaders(testMemberId)))
                    .andExpect(status().isNotFound());

            // then
            BDDMockito.then(commentQueryService).should(BDDMockito.times(1)).getComments(feedId);
        }

        @Test
        @DisplayName("Access 토큰이 없는 경우 401 Unauthorized 오류를 반환한다")
        void getComments_Failed_Not_Having_Token() throws Exception {
            // given
            Long feedId = 1L;

            // when & then
            mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/feeds/{feedId}/comments", feedId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isUnauthorized())
                    .andDo(print());
        }
    }
}
