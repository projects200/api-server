package com.project200.undabang.like.controller;

import com.project200.undabang.common.web.exception.CustomException;
import com.project200.undabang.common.web.exception.ErrorCode;
import com.project200.undabang.common.web.response.CommonResponse;
import com.project200.undabang.configuration.AbstractRestDocSupport;
import com.project200.undabang.like.dto.CreateFeedLikeRequest;
import com.project200.undabang.like.dto.CreateFeedLikeResponse;
import com.project200.undabang.like.service.impl.FeedLikeCommandServiceImpl;
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

@WebMvcTest(FeedLikeCommandController.class)
@DisplayName("FeedLikeCommandController 테스트")
class FeedLikeCommandControllerTest extends AbstractRestDocSupport {

        @MockitoBean
        private FeedLikeCommandServiceImpl feedLikeCommandService;

        @Nested
        @DisplayName("createFeedLike 메소드는")
        class CreateFeedLike {

                @Test
                @DisplayName("피드 좋아요를 성공한다")
                void createFeedLike_success() throws Exception {
                        // given
                        UUID testMemberId = UUID.randomUUID();
                        Long feedId = 5L;
                        CreateFeedLikeRequest request = new CreateFeedLikeRequest(true);
                        CreateFeedLikeResponse responseDto = new CreateFeedLikeResponse();

                        BDDMockito
                                        .given(feedLikeCommandService.createFeedLike(BDDMockito.eq(feedId),
                                                        BDDMockito.any(CreateFeedLikeRequest.class)))
                                        .willReturn(responseDto);

                        // when
                        String response = mockMvc
                                        .perform(MockMvcRequestBuilders
                                                        .post("/api/v1/feeds/{feedId}/like", feedId)
                                                        .contentType(MediaType.APPLICATION_JSON)
                                                        .accept(MediaType.APPLICATION_JSON)
                                                        .headers(getCommonApiHeaders(testMemberId))
                                                        .content(objectMapper.writeValueAsString(request)))
                                        .andExpect(status().isCreated())
                                        .andDo(document.document(
                                                        requestHeaders(HEADER_ACCESS_TOKEN),
                                                        pathParameters(
                                                                        parameterWithName("feedId").attributes(
                                                                                        getTypeFormat(JsonFieldType.NUMBER))
                                                                                        .description("좋아요할 피드의 ID")),
                                                        requestFields(
                                                                        fieldWithPath("status")
                                                                                        .type(JsonFieldType.BOOLEAN)
                                                                                        .description("좋아요 상태 (true: 좋아요, false: 좋아요 취소)")),
                                                        responseFields(commonResponseFields()))) // Assuming data is
                                        // empty object {}
                                        // success
                                        .andReturn().getResponse().getContentAsString();

                        // then
                        CommonResponse<CreateFeedLikeResponse> expectedData = CommonResponse.create(responseDto);
                        String expected = objectMapper.writeValueAsString(expectedData);
                        assertThat(response).as("응답 본문 검증").isEqualTo(expected);
                }

                @Test
                @DisplayName("존재하지 않는 피드 ID로 좋아요 시도 시 실패한다")
                void createFeedLike_Failed_FeedNotFound() throws Exception {
                        // given
                        UUID testMemberId = UUID.randomUUID();
                        Long feedId = 999L;
                        CreateFeedLikeRequest request = new CreateFeedLikeRequest(true);

                        BDDMockito
                                        .given(feedLikeCommandService.createFeedLike(BDDMockito.eq(feedId),
                                                        BDDMockito.any(CreateFeedLikeRequest.class)))
                                        .willThrow(new CustomException(ErrorCode.FEED_NOT_FOUND));

                        // when
                        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/feeds/{feedId}/like", feedId)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .accept(MediaType.APPLICATION_JSON)
                                        .headers(getCommonApiHeaders(testMemberId))
                                        .content(objectMapper.writeValueAsString(request)))
                                        .andExpect(status().isNotFound());
                }
        }
}
