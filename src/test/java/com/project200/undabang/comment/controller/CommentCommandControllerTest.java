package com.project200.undabang.comment.controller;

import com.project200.undabang.comment.dto.request.CreateCommentRequest;
import com.project200.undabang.comment.dto.response.CreateCommentResponse;
import com.project200.undabang.comment.service.CommentCommandService;
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

import java.util.UUID;

import static com.project200.undabang.configuration.DocumentFormatGenerator.getTypeFormat;
import static com.project200.undabang.configuration.HeadersGenerator.getCommonApiHeaders;
import static com.project200.undabang.configuration.RestDocsUtils.*;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CommentCommandController.class)
@DisplayName("CommentCommandController 테스트")
class CommentCommandControllerTest extends AbstractRestDocSupport {

    @MockitoBean
    private CommentCommandService commentCommandService;

    @Nested
    @DisplayName("createComment 메소드는")
    class CreateComment {

        @Test
        @DisplayName("댓글 작성을 성공한다")
        void createComment_success() throws Exception {
            // given
            UUID testMemberId = UUID.randomUUID();
            Long feedId = 1L;
            CreateCommentRequest request = new CreateCommentRequest("댓글 내용입니다.", null);
            CreateCommentResponse responseDto = new CreateCommentResponse(1L);

            BDDMockito
                    .given(commentCommandService.createComment(BDDMockito.eq(feedId),
                            BDDMockito.any(CreateCommentRequest.class)))
                    .willReturn(responseDto);

            // when
            String response = mockMvc
                    .perform(MockMvcRequestBuilders.post("/api/v1/feeds/{feedId}/comments", feedId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .accept(MediaType.APPLICATION_JSON)
                            .headers(getCommonApiHeaders(testMemberId))
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andDo(document.document(
                            requestHeaders(HEADER_ACCESS_TOKEN),
                            pathParameters(
                                    parameterWithName("feedId").attributes(getTypeFormat(JsonFieldType.NUMBER)).description("댓글을 작성할 피드의 고유 식별자(ID) 입니다.")),
                            requestFields(
                                    fieldWithPath("content").type(JsonFieldType.STRING).description("작성할 댓글의 내용입니다."),
                                    fieldWithPath("parentCommentId").type(JsonFieldType.NUMBER).description("해당 댓글이 대댓글인 경우, 부모 댓글의 고유 식별자(ID)입니다.").optional()),
                            responseFields(commonResponseFields(fieldWithPath("data.commentId").type(JsonFieldType.NUMBER).description("작성된 댓글의 고유 식별자(ID) 입니다.")))))
                    .andReturn().getResponse().getContentAsString();

            // then
            CommonResponse<CreateCommentResponse> expectedData = CommonResponse.create(responseDto);
            String expected = objectMapper.writeValueAsString(expectedData);
            Assertions.assertThat(response).as("응답 본문 검증").isEqualTo(expected);
        }

        @Test
        @DisplayName("대댓글 작성을 성공한다")
        void createReply_success() throws Exception {
            // given
            UUID testMemberId = UUID.randomUUID();
            Long feedId = 1L;
            Long parentCommentId = 1L;
            CreateCommentRequest request = new CreateCommentRequest("대댓글 내용입니다.", parentCommentId);
            CreateCommentResponse responseDto = new CreateCommentResponse(2L);

            BDDMockito
                    .given(commentCommandService.createComment(BDDMockito.eq(feedId),
                            BDDMockito.any(CreateCommentRequest.class)))
                    .willReturn(responseDto);

            // when
            mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/feeds/{feedId}/comments", feedId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .accept(MediaType.APPLICATION_JSON)
                            .headers(getCommonApiHeaders(testMemberId))
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());

            // then
            BDDMockito.then(commentCommandService).should(BDDMockito.times(1))
                    .createComment(BDDMockito.eq(feedId),
                            BDDMockito.any(CreateCommentRequest.class));
        }

        @Test
        @DisplayName("존재하지 않는 피드 ID로 작성 시 실패한다")
        void createComment_Failed_FeedNotFound() throws Exception {
            // given
            UUID testMemberId = UUID.randomUUID();
            Long feedId = 999L;
            CreateCommentRequest request = new CreateCommentRequest("댓글 내용입니다.", null);

            BDDMockito
                    .given(commentCommandService.createComment(BDDMockito.eq(feedId),
                            BDDMockito.any(CreateCommentRequest.class)))
                    .willThrow(new CustomException(ErrorCode.FEED_NOT_FOUND));

            // when
            mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/feeds/{feedId}/comments", feedId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .accept(MediaType.APPLICATION_JSON)
                            .headers(getCommonApiHeaders(testMemberId))
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound());

            // then
            BDDMockito.then(commentCommandService).should(BDDMockito.times(1))
                    .createComment(BDDMockito.eq(feedId),
                            BDDMockito.any(CreateCommentRequest.class));
        }

        @Test
        @DisplayName("존재하지 않는 부모 댓글 ID로 대댓글 작성 시 실패한다")
        void createReply_Failed_ParentNotFound() throws Exception {
            // given
            UUID testMemberId = UUID.randomUUID();
            Long feedId = 1L;
            CreateCommentRequest request = new CreateCommentRequest("대댓글 내용입니다.", 999L);

            BDDMockito
                    .given(commentCommandService.createComment(BDDMockito.eq(feedId),
                            BDDMockito.any(CreateCommentRequest.class)))
                    .willThrow(new CustomException(ErrorCode.COMMENT_PARENT_NOT_FOUND));

            // when
            mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/feeds/{feedId}/comments", feedId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .accept(MediaType.APPLICATION_JSON)
                            .headers(getCommonApiHeaders(testMemberId))
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound());

            // then
            BDDMockito.then(commentCommandService).should(BDDMockito.times(1))
                    .createComment(BDDMockito.eq(feedId),
                            BDDMockito.any(CreateCommentRequest.class));
        }

        @Test
        @DisplayName("Access 토큰이 없는 경우 401 Unauthorized 오류를 반환한다")
        void createComment_Failed_Not_Having_Token() throws Exception {
            // given
            Long feedId = 1L;
            CreateCommentRequest request = new CreateCommentRequest("댓글 내용입니다.", null);

            // when & then
            mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/feeds/{feedId}/comments", feedId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .accept(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized())
                    .andDo(print());
        }
    }

    @Nested
    @DisplayName("deleteComment 메소드는")
    class DeleteComment {

        @Test
        @DisplayName("댓글 삭제를 성공한다")
        void deleteComment_success() throws Exception {
            // given
            UUID testMemberId = UUID.randomUUID();
            Long commentId = 1L;

            BDDMockito.willDoNothing().given(commentCommandService).deleteComment(commentId);

            // when
            mockMvc.perform(MockMvcRequestBuilders.delete("/api/v1/comments/{commentId}", commentId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .accept(MediaType.APPLICATION_JSON)
                            .headers(getCommonApiHeaders(testMemberId)))
                    .andExpect(status().isOk())
                    .andDo(document.document(
                            requestHeaders(HEADER_ACCESS_TOKEN),
                            pathParameters(
                                    parameterWithName("commentId").attributes(getTypeFormat(JsonFieldType.NUMBER))
                                            .description("삭제할 댓글 ID")),
                            responseFields(commonResponseFieldsOnly())));

            // then
            BDDMockito.then(commentCommandService).should(BDDMockito.times(1)).deleteComment(commentId);
        }

        @Test
        @DisplayName("존재하지 않는 댓글 ID로 삭제 시 실패한다")
        void deleteComment_Failed_CommentNotFound() throws Exception {
            // given
            UUID testMemberId = UUID.randomUUID();
            Long commentId = 999L;

            BDDMockito.willThrow(new CustomException(ErrorCode.COMMENT_NOT_FOUND))
                    .given(commentCommandService).deleteComment(commentId);

            // when
            mockMvc.perform(MockMvcRequestBuilders.delete("/api/v1/comments/{commentId}", commentId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .accept(MediaType.APPLICATION_JSON)
                            .headers(getCommonApiHeaders(testMemberId)))
                    .andExpect(status().isNotFound());

            // then
            BDDMockito.then(commentCommandService).should(BDDMockito.times(1)).deleteComment(commentId);
        }

        @Test
        @DisplayName("작성자가 아닌 회원이 삭제 시도 시 403 Forbidden 오류를 반환한다")
        void deleteComment_Failed_Forbidden() throws Exception {
            // given
            UUID testMemberId = UUID.randomUUID();
            Long commentId = 1L;

            BDDMockito.willThrow(new CustomException(ErrorCode.COMMENT_DELETE_FORBIDDEN))
                    .given(commentCommandService).deleteComment(commentId);

            // when
            mockMvc.perform(MockMvcRequestBuilders.delete("/api/v1/comments/{commentId}", commentId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .accept(MediaType.APPLICATION_JSON)
                            .headers(getCommonApiHeaders(testMemberId)))
                    .andExpect(status().isForbidden());

            // then
            BDDMockito.then(commentCommandService).should(BDDMockito.times(1)).deleteComment(commentId);
        }

        @Test
        @DisplayName("Access 토큰이 없는 경우 401 Unauthorized 오류를 반환한다")
        void deleteComment_Failed_Not_Having_Token() throws Exception {
            // given
            Long commentId = 1L;

            // when & then
            mockMvc.perform(MockMvcRequestBuilders.delete("/api/v1/comments/{commentId}", commentId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isUnauthorized())
                    .andDo(print());
        }
    }
}
