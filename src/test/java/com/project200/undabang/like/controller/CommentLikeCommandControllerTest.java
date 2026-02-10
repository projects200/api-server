package com.project200.undabang.like.controller;

import com.project200.undabang.common.web.exception.CustomException;
import com.project200.undabang.common.web.exception.ErrorCode;
import com.project200.undabang.common.web.response.CommonResponse;
import com.project200.undabang.configuration.AbstractRestDocSupport;
import com.project200.undabang.like.dto.request.CreateCommentLikeRequest;
import com.project200.undabang.like.dto.response.CreateCommentLikeResponse;
import com.project200.undabang.like.service.impl.CommentCommandLikeServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.BDDMockito;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.UUID;

import static com.project200.undabang.configuration.DocumentFormatGenerator.getTypeFormat;
import static com.project200.undabang.configuration.HeadersGenerator.getCommonApiHeaders;
import static com.project200.undabang.configuration.RestDocsUtils.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CommentLikeCommandController.class)
@DisplayName("CommentCommandLikeController 테스트")
class CommentLikeCommandControllerTest extends AbstractRestDocSupport {

    @MockitoBean
    private CommentCommandLikeServiceImpl commentCommandLikeService;

    @Nested
    @DisplayName("createCommentLike 메소드는")
    class CreateCommentLike {

        @Test
        @DisplayName("댓글 좋아요를 성공한다")
        void createCommentLike_success() throws Exception {
            // given
            UUID testMemberId = UUID.randomUUID();
            Long commentId = 5L;
            CreateCommentLikeRequest request = new CreateCommentLikeRequest(true);
            CreateCommentLikeResponse responseDto = new CreateCommentLikeResponse(true, 1);

            BDDMockito
                    .given(commentCommandLikeService.createCommentLike(BDDMockito.eq(commentId),
                            BDDMockito.any(CreateCommentLikeRequest.class)))
                    .willReturn(responseDto);

            // when
            String response = mockMvc
                    .perform(MockMvcRequestBuilders
                            .post("/api/v1/comments/{commentId}/like", commentId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .accept(MediaType.APPLICATION_JSON)
                            .headers(getCommonApiHeaders(testMemberId))
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andDo(document.document(
                            requestHeaders(HEADER_ACCESS_TOKEN),
                            pathParameters(
                                    parameterWithName("commentId").attributes(
                                                    getTypeFormat(JsonFieldType.NUMBER))
                                            .description("좋아요할 댓글의 ID")),
                            requestFields(
                                    fieldWithPath("liked")
                                            .type(JsonFieldType.BOOLEAN)
                                            .description("좋아요 상태 (true: 좋아요, false: 좋아요 취소)")),
                            responseFields(commonResponseFields(
                                    fieldWithPath("data.liked")
                                            .type(JsonFieldType.BOOLEAN)
                                            .description("좋아요 여부"),
                                    fieldWithPath("data.likesCount")
                                            .type(JsonFieldType.NUMBER)
                                            .description("댓글 좋아요 수")))))
                    .andReturn().getResponse().getContentAsString();

            // then
            CommonResponse<CreateCommentLikeResponse> expectedData = CommonResponse.create(responseDto);
            String expected = objectMapper.writeValueAsString(expectedData);
            assertThat(response).as("응답 본문 검증").isEqualTo(expected);
        }

        @Test
        @DisplayName("존재하지 않는 댓글 ID로 좋아요 시도 시 실패한다")
        void createCommentLike_Failed_CommentNotFound() throws Exception {
            // given
            UUID testMemberId = UUID.randomUUID();
            Long commentId = 999L;
            CreateCommentLikeRequest request = new CreateCommentLikeRequest(true);

            BDDMockito
                    .given(commentCommandLikeService.createCommentLike(BDDMockito.eq(commentId),
                            BDDMockito.any(CreateCommentLikeRequest.class)))
                    .willThrow(new CustomException(ErrorCode.COMMENT_NOT_FOUND));

            // when
            mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/comments/{commentId}/like", commentId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .accept(MediaType.APPLICATION_JSON)
                            .headers(getCommonApiHeaders(testMemberId))
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound());
        }
    }
}
