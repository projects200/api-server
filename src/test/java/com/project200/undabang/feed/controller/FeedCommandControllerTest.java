package com.project200.undabang.feed.controller;

import com.project200.undabang.configuration.AbstractRestDocSupport;
import com.project200.undabang.feed.dto.request.CreateFeedRequest;
import com.project200.undabang.feed.dto.request.UpdateFeedRequest;
import com.project200.undabang.feed.dto.response.CreateFeedPictureResponse;
import com.project200.undabang.feed.dto.response.CreateFeedResponse;
import com.project200.undabang.feed.dto.response.UpdateFeedResponse;
import com.project200.undabang.feed.service.FeedCommandService;
import com.project200.undabang.feed.service.FeedPictureService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.UUID;

import static com.networknt.schema.JsonType.*;
import static com.project200.undabang.configuration.DocumentFormatGenerator.getTypeFormat;
import static com.project200.undabang.configuration.HeadersGenerator.getCommonApiHeaders;
import static com.project200.undabang.configuration.RestDocsUtils.*;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FeedCommandController.class)
class FeedCommandControllerTest extends AbstractRestDocSupport {

    @MockitoBean
    private FeedCommandService feedCommandService;

    @MockitoBean
    private FeedPictureService feedPictureService;

    @Nested
    @DisplayName("createFeedPictures 메소드는")
    class CreateFeedPictures {

        @Test
        @DisplayName("이미지 파일 리스트를 받아 업로드하고 저장된 정보를 반환한다")
        void createFeedPictures_Success() throws Exception {
            // given
            UUID memberId = UUID.randomUUID();
            Long feedId = 100L;
            MockMultipartFile file = new MockMultipartFile("pictures", "test.jpg", "image/jpeg", "image-data".getBytes());
            List<CreateFeedPictureResponse> response = List.of(new CreateFeedPictureResponse(1L, "http://s3.url/test.jpg"));

            given(feedPictureService.createFeedPictures(eq(feedId), anyList())).willReturn(response);

            mockMvc.perform(RestDocumentationRequestBuilders.multipart("/api/v1/feeds/{feedId}/pictures", feedId)
                            .file(file)
                            .headers(getCommonApiHeaders(memberId))
                            .contentType(MediaType.MULTIPART_FORM_DATA))
                    .andExpectAll(
                            status().isCreated(),
                            jsonPath("$.data", hasSize(1)),
                            jsonPath("$.data[0].pictureId").value(1L)
                    )
                    .andDo(document.document(
                            requestHeaders(HEADER_ACCESS_TOKEN),
                            pathParameters(
                                    parameterWithName("feedId").attributes(getTypeFormat(JsonFieldType.NUMBER)).description("피드의 고유 식별자입니다.")
                            ),
                            requestParts(
                                    partWithName("pictures").attributes(getTypeFormat(JsonFieldType.ARRAY)).description("업로드할 이미지 파일 리스트 (최대 5개, jpg/jpeg/png 지원)")
                            ),
                            responseFields(
                                    commonResponseFieldsForList(
                                            fieldWithPath("data").attributes(getTypeFormat(JsonFieldType.ARRAY)).description("업로드된 사진 결과 리스트입니다."),
                                            fieldWithPath("data[].pictureId").attributes(getTypeFormat(JsonFieldType.NUMBER)).description("저장된 사진의 고유 식별자입니다."),
                                            fieldWithPath("data[].pictureUrl").attributes(getTypeFormat(JsonFieldType.STRING)).description("S3에 업로드된 사진의 URL입니다.")
                                    )
                            )
                    ));
        }
    }

    @Nested
    @DisplayName("deleteMemberFeed 메소드는")
    class DeleteMemberFeed {

        @Test
        @DisplayName("피드 삭제 요청이 유효하면 200 상태코드와 성공 메시지를 반환한다")
        void deleteMemberFeed_Success() throws Exception {
            // given
            UUID memberId = UUID.randomUUID();
            Long feedId = 100L;

            // 에러 해결: void 메서드는 doNothing()을 사용하거나 스터빙을 생략합니다.
            doNothing().when(feedCommandService).deleteMemberFeed(feedId);

            // when & then
            mockMvc.perform(RestDocumentationRequestBuilders.delete("/api/v1/feeds/{feedId}", feedId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .accept(MediaType.APPLICATION_JSON)
                            .headers(getCommonApiHeaders(memberId)))
                    .andExpectAll(
                            status().isOk(),
                            jsonPath("$.succeed").value(true)
                    )
                    .andDo(document.document(
                            requestHeaders(HEADER_ACCESS_TOKEN),
                            pathParameters(
                                    parameterWithName("feedId").attributes(getTypeFormat(JsonFieldType.NUMBER)).description("삭제할 피드의 고유 식별자입니다.")
                            ),
                            responseFields(
                                    fieldWithPath("succeed").type(BOOLEAN).description("요청 성공 여부입니다."),
                                    fieldWithPath("code").type(STRING).description("응답 코드 정보 입니다."),
                                    fieldWithPath("message").type(STRING).description("응답 메시지입니다."),
                                    fieldWithPath("data").type(NULL).description("삭제 API는 반환 데이터가 없습니다.")
                            )
                    ));

            // 호출 여부 검증
            verify(feedCommandService).deleteMemberFeed(feedId);
        }
    }

    @Nested
    @DisplayName("updateMemberFeed 메소드는")
    class UpdateMemberFeed {

        @Test
        @DisplayName("피드 수정 요청이 유효하면 200 상태코드와 수정된 정보를 반환한다")
        void updateMemberFeed_Success() throws Exception {
            // given
            UUID memberId = UUID.randomUUID();
            Long feedId = 100L;
            UpdateFeedRequest request = new UpdateFeedRequest("오늘도 러닝 완료! 수정합니다.", 2L);

            UpdateFeedResponse response = UpdateFeedResponse.builder()
                    .feedId(feedId)
                    .feedContent("오늘도 러닝 완료! 수정합니다.")
                    .feedLikesCount(10)
                    .feedCommentsCount(2)
                    .feedTypeId(2L)
                    .feedTypeName("러닝다방")
                    .feedTypeDesc("러닝을 즐기는 사람들의 공간")
                    .build();

            given(feedCommandService.updateMemberFeed(eq(feedId), any(UpdateFeedRequest.class)))
                    .willReturn(response);

            // when & then
            mockMvc.perform(RestDocumentationRequestBuilders.patch("/api/v1/feeds/{feedId}", feedId)
                            .content(objectMapper.writeValueAsString(request))
                            .contentType(MediaType.APPLICATION_JSON)
                            .accept(MediaType.APPLICATION_JSON)
                            .headers(getCommonApiHeaders(memberId)))
                    .andExpectAll(
                            status().isOk(),
                            jsonPath("$.succeed").value(true),
                            jsonPath("$.data.feedId").value(feedId),
                            jsonPath("$.data.feedContent").value("오늘도 러닝 완료! 수정합니다."),
                            jsonPath("$.data.feedTypeName").value("러닝다방")
                    )
                    .andDo(document.document(
                            requestHeaders(HEADER_ACCESS_TOKEN),
                            pathParameters(
                                    parameterWithName("feedId").attributes(getTypeFormat(JsonFieldType.NUMBER)).description("수정할 피드의 고유 식별자입니다.")
                            ),
                            requestFields(
                                    fieldWithPath("feedContent").type(STRING).description("수정할 피드의 본문 내용입니다."),
                                    fieldWithPath("feedTypeId").type(NUMBER).description("변경할 피드 타입 식별자입니다. 카테고리를 유지하거나 변경할 때 사용하며, null을 보내면 카테고리가 해제됩니다.").optional()
                            ),
                            responseFields(commonResponseFields(
                                    fieldWithPath("data.feedId").type(NUMBER).description("수정된 피드의 식별자입니다."),
                                    fieldWithPath("data.feedContent").type(STRING).description("수정된 피드의 본문 내용입니다."),
                                    fieldWithPath("data.feedLikesCount").type(NUMBER).description("수정된 피드의 현재 좋아요 수입니다."),
                                    fieldWithPath("data.feedCommentsCount").type(NUMBER).description("수정된 피드의 현재 댓글 수입니다."),
                                    fieldWithPath("data.feedTypeId").type(NUMBER).description("수정된 피드 타입 식별자입니다.").optional(),
                                    fieldWithPath("data.feedTypeName").type(STRING).description("수정된 피드 타입 이름입니다.").optional(),
                                    fieldWithPath("data.feedTypeDesc").type(STRING).description("수정된 피드 타입 설명입니다.").optional()
                            ))
                    ));

            verify(feedCommandService).updateMemberFeed(eq(feedId), any(UpdateFeedRequest.class));
        }
    }

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

    @Nested
    @DisplayName("deleteFeedPictures 메소드는")
    class DeleteFeedPictures {

        @Test
        @DisplayName("피드 사진 삭제 요청이 유효하면 200 상태코드를 반환한다")
        void deleteFeedPictures_Success() throws Exception {
            // given
            UUID memberId = UUID.randomUUID();
            Long feedId = 1L;
            Long pictureId = 100L;

            // Service는 void 반환이므로 doNothing() 설정
            doNothing().when(feedPictureService).deleteFeedPictures(feedId, pictureId);

            // when & then
            mockMvc.perform(RestDocumentationRequestBuilders.delete("/api/v1/feeds/{feedId}/pictures/{pictureId}", feedId, pictureId)
                            .headers(getCommonApiHeaders(memberId))
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpectAll(
                            status().isOk(),
                            jsonPath("$.succeed").value(true),
                            jsonPath("$.data").isEmpty() // null 확인
                    )
                    .andDo(document.document(
                            requestHeaders(HEADER_ACCESS_TOKEN),
                            pathParameters(
                                    parameterWithName("feedId").attributes(getTypeFormat(JsonFieldType.NUMBER)).description("삭제할 사진이 속한 피드의 식별자입니다."),
                                    parameterWithName("pictureId").attributes(getTypeFormat(JsonFieldType.NUMBER)).description("삭제할 사진의 고유 식별자입니다.")
                            ),
                            responseFields(commonResponseFields(
                                    fieldWithPath("data").attributes(getTypeFormat(JsonFieldType.NULL)).description("삭제 성공 시 데이터는 반환되지 않습니다.")
                            ))
                    ));
        }
    }
}