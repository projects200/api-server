package com.project200.undabang.feed.controller;

import com.project200.undabang.configuration.AbstractRestDocSupport;
import com.project200.undabang.feed.dto.request.CreateFeedRequest;
import com.project200.undabang.feed.dto.response.CreateFeedResponse;
import com.project200.undabang.feed.service.FeedCommandService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.UUID;

import static com.networknt.schema.JsonType.NUMBER;
import static com.networknt.schema.JsonType.STRING;
import static com.project200.undabang.configuration.HeadersGenerator.getCommonApiHeaders;
import static com.project200.undabang.configuration.RestDocsUtils.HEADER_ACCESS_TOKEN;
import static com.project200.undabang.configuration.RestDocsUtils.commonResponseFields;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FeedCommandController.class)
class FeedCommandControllerTest extends AbstractRestDocSupport {

    @MockitoBean
    private FeedCommandService feedCommandService;

    @Nested
    @DisplayName("POST /api/v1/feeds API는")
    class CreateMemberFeed {

        @Test
        @DisplayName("새로운 피드를 생성하고 201 상태코드를 반환한다")
        void createMemberFeed_Success() throws Exception {
            // given
            UUID memberId = UUID.randomUUID();
            CreateFeedRequest request = new CreateFeedRequest("오늘도 오운완!", 1L);
            CreateFeedResponse response = new CreateFeedResponse(100L);

            given(feedCommandService.createMemberFeed(any(CreateFeedRequest.class))).willReturn(response);

            // when & then
            mockMvc.perform(post("/api/v1/feeds")
                            .content(objectMapper.writeValueAsString(request))
                            .contentType(MediaType.APPLICATION_JSON)
                            .accept(MediaType.APPLICATION_JSON)
                            .headers(getCommonApiHeaders(memberId)))
                    .andExpectAll(
                            status().isCreated(), // 201 확인
                            jsonPath("$.succeed").value(true),
                            jsonPath("$.data.feedId").value(100L)
                    )
                    .andDo(document.document(
                            requestHeaders(HEADER_ACCESS_TOKEN),
                            requestFields(
                                    fieldWithPath("feedContent").type(STRING).description("피드 본문 내용입니다. 비어있을 수 없습니다."),
                                    fieldWithPath("feedTypeId").type(NUMBER).description("피드 타입 식별자입니다. 선택 사항입니다.").optional()
                            ),
                            responseFields(commonResponseFields(
                                    fieldWithPath("data.feedId").type(NUMBER).description("성공적으로 생성된 피드의 고유 식별자입니다.")
                            ))
                    ));

            verify(feedCommandService).createMemberFeed(any(CreateFeedRequest.class));
        }
    }
}