package com.project200.undabang.openchat.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project200.undabang.common.web.exception.CustomException;
import com.project200.undabang.common.web.exception.ErrorCode;
import com.project200.undabang.configuration.AbstractRestDocSupport;
import com.project200.undabang.openchat.dto.request.CreateOpenChatRoomRequest;
import com.project200.undabang.openchat.dto.request.UpdateOpenChatRoomRequest;
import com.project200.undabang.openchat.dto.response.CreateOpenChatRoomResponse;
import com.project200.undabang.openchat.dto.response.UpdateOpenChatRoomResponse;
import com.project200.undabang.openchat.service.OpenChatRoomCommandService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.BDDMockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.UUID;

import static com.project200.undabang.configuration.DocumentFormatGenerator.getTypeFormat;
import static com.project200.undabang.configuration.HeadersGenerator.getCommonApiHeaders;
import static com.project200.undabang.configuration.RestDocsUtils.HEADER_ACCESS_TOKEN;
import static com.project200.undabang.configuration.RestDocsUtils.commonResponseFields;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OpenChatRoomCommandController.class)
class OpenChatRoomCommandControllerTest extends AbstractRestDocSupport {

    @MockitoBean
    private OpenChatRoomCommandService openChatRoomCommandService;

    @Autowired
    private ObjectMapper objectMapper;

    private CreateOpenChatRoomRequest createCreateRequest(String url) {
        return new CreateOpenChatRoomRequest(url);
    }

    private CreateOpenChatRoomResponse createCreateResponse(long id) {
        return new CreateOpenChatRoomResponse(id);
    }

    private UpdateOpenChatRoomRequest createUpdateRequest(String url) {
        return new UpdateOpenChatRoomRequest(url);
    }

    private UpdateOpenChatRoomResponse createUpdateResponse(long id) {
        return new UpdateOpenChatRoomResponse(id);
    }

    @Nested
    @DisplayName("POST /api/v1/open-chats API는")
    class CreateOpenChatRoom {

        @Test
        @DisplayName("오픈채팅방을 성공적으로 생성한다")
        void createOpenChatRoom_Success() throws Exception {
            // given
            UUID memberId = UUID.randomUUID();
            CreateOpenChatRoomRequest request = createCreateRequest("https://open.kakao.com/o/newroom");
            CreateOpenChatRoomResponse response = createCreateResponse(1L);

            BDDMockito.given(openChatRoomCommandService.createOpenChatRoom(any(CreateOpenChatRoomRequest.class)))
                    .willReturn(response);

            // when & then
            mockMvc.perform(post("/api/v1/open-chats")
                            .contentType(MediaType.APPLICATION_JSON)
                            .accept(MediaType.APPLICATION_JSON)
                            .headers(getCommonApiHeaders(memberId))
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpectAll(
                            status().isCreated(),
                            jsonPath("$.succeed").value(true),
                            jsonPath("$.code").value("CREATED"),
                            jsonPath("$.data.openChatroomId").value(1L)
                    )
                    .andDo(document.document(
                            requestHeaders(HEADER_ACCESS_TOKEN),
                            requestFields(
                                    fieldWithPath("openChatroomUrl").type(JsonFieldType.STRING).description("생성할 오픈채팅방 URL 입니다.")
                            ),
                            responseFields(commonResponseFields(
                                    fieldWithPath("data.openChatroomId").type(JsonFieldType.NUMBER).description("생성된 오픈채팅방의 식별자 입니다.")
                            ))
                    ));

            BDDMockito.then(openChatRoomCommandService).should(BDDMockito.times(1)).createOpenChatRoom(any(CreateOpenChatRoomRequest.class));
        }

        @Test
        @DisplayName("이미 오픈채팅방이 존재하면 생성에 실패한다")
        void createOpenChatRoom_Fail_AlreadyExist() throws Exception {
            // given
            UUID memberId = UUID.randomUUID();
            CreateOpenChatRoomRequest request = createCreateRequest("https://open.kakao.com/o/existing");

            BDDMockito.given(openChatRoomCommandService.createOpenChatRoom(any(CreateOpenChatRoomRequest.class)))
                    .willThrow(new CustomException(ErrorCode.OPEN_CHAT_ROOM_ALREADY_EXIST));

            // when & then
            mockMvc.perform(post("/api/v1/open-chats")
                            .contentType(MediaType.APPLICATION_JSON)
                            .accept(MediaType.APPLICATION_JSON)
                            .headers(getCommonApiHeaders(memberId))
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpectAll(
                            status().isConflict(), // ALREADY_EXIST는 409 Conflict를 반환한다고 가정
                            jsonPath("$.succeed").value(false),
                            jsonPath("$.code").value(ErrorCode.OPEN_CHAT_ROOM_ALREADY_EXIST.getCode()),
                            jsonPath("$.message").value(ErrorCode.OPEN_CHAT_ROOM_ALREADY_EXIST.getMessage())
                    );
        }
    }

    @Nested
    @DisplayName("PATCH /api/v1/open-chats/{openChatId} API는")
    class UpdateOpenChatRoom {

        @Test
        @DisplayName("오픈채팅방 정보를 성공적으로 수정한다")
        void updateOpenChatRoom_Success() throws Exception {
            // given
            UUID memberId = UUID.randomUUID();
            long openChatId = 1L;
            UpdateOpenChatRoomRequest request = createUpdateRequest("https://open.kakao.com/o/updated");
            UpdateOpenChatRoomResponse response = createUpdateResponse(openChatId);

            BDDMockito.given(openChatRoomCommandService.updateOpenChatRoom(eq(openChatId), any(UpdateOpenChatRoomRequest.class)))
                    .willReturn(response);

            // when & then
            mockMvc.perform(patch("/api/v1/open-chats/{openChatId}", openChatId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .accept(MediaType.APPLICATION_JSON)
                            .headers(getCommonApiHeaders(memberId))
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpectAll(
                            status().isOk(),
                            jsonPath("$.succeed").value(true),
                            jsonPath("$.code").value("SUCCESS"),
                            jsonPath("$.data.openChatroomId").value(openChatId)
                    )
                    .andDo(document.document(
                            requestHeaders(HEADER_ACCESS_TOKEN),
                            pathParameters(
                                    parameterWithName("openChatId").attributes(getTypeFormat(JsonFieldType.NUMBER)).description("수정할 오픈채팅방의 식별자 입니다.")
                            ),
                            requestFields(
                                    fieldWithPath("openChatroomUrl").type(JsonFieldType.STRING).description("새로운 오픈채팅방 URL 입니다.")
                            ),
                            responseFields(commonResponseFields(
                                    fieldWithPath("data.openChatroomId").type(JsonFieldType.NUMBER).description("수정된 오픈채팅방의 식별자 입니다.")
                            ))
                    ));

            BDDMockito.then(openChatRoomCommandService).should(BDDMockito.times(1)).updateOpenChatRoom(eq(openChatId), any(UpdateOpenChatRoomRequest.class));
        }

        @Test
        @DisplayName("수정 권한이 없으면 실패한다")
        void updateOpenChatRoom_Fail_AuthorizationDenied() throws Exception {
            // given
            UUID memberId = UUID.randomUUID();
            long openChatId = 2L; // 다른 사람의 오픈채팅방 ID

            String validUrl = "https://open.kakao.com/o/g1a2b3c4";
            UpdateOpenChatRoomRequest request = createUpdateRequest(validUrl);

            BDDMockito.given(openChatRoomCommandService.updateOpenChatRoom(eq(openChatId), any(UpdateOpenChatRoomRequest.class)))
                    .willThrow(new CustomException(ErrorCode.AUTHORIZATION_DENIED));

            // when & then
            mockMvc.perform(patch("/api/v1/open-chats/{openChatId}", openChatId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .accept(MediaType.APPLICATION_JSON)
                            .headers(getCommonApiHeaders(memberId))
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpectAll(
                            status().isForbidden(), // AUTHORIZATION_DENIED는 403 Forbidden을 반환한다고 가정
                            jsonPath("$.succeed").value(false),
                            jsonPath("$.code").value(ErrorCode.AUTHORIZATION_DENIED.getCode()),
                            jsonPath("$.message").value(ErrorCode.AUTHORIZATION_DENIED.getMessage())
                    );
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/open-chats/{openChatId} API는")
    class DeleteOpenChatRoom {

        @Test
        @DisplayName("오픈채팅방을 성공적으로 삭제한다")
        void deleteOpenChatRoom_Success() throws Exception {
            // given
            UUID memberId = UUID.randomUUID();
            long openChatId = 1L;

            BDDMockito.willDoNothing().given(openChatRoomCommandService).deleteOpenChatRoom(openChatId);

            // when & then
            mockMvc.perform(delete("/api/v1/open-chats/{openChatId}", openChatId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .accept(MediaType.APPLICATION_JSON)
                            .headers(getCommonApiHeaders(memberId)))
                    .andExpectAll(
                            status().isOk(),
                            jsonPath("$.succeed").value(true),
                            jsonPath("$.code").value("DELETED"),
                            jsonPath("$.data").doesNotExist()
                    )
                    .andDo(document.document(
                            requestHeaders(HEADER_ACCESS_TOKEN),
                            pathParameters(
                                    parameterWithName("openChatId").attributes(getTypeFormat(JsonFieldType.NUMBER)).description("삭제할 오픈채팅방의 식별자 입니다.")
                            ),
                            responseFields(commonResponseFields())
                    ));

            BDDMockito.then(openChatRoomCommandService).should(BDDMockito.times(1)).deleteOpenChatRoom(openChatId);
        }

        @Test
        @DisplayName("삭제 권한이 없으면 실패한다")
        void deleteOpenChatRoom_Fail_AuthorizationDenied() throws Exception {
            // given
            UUID memberId = UUID.randomUUID();
            long openChatId = 2L; // 다른 사람의 오픈채팅방 ID

            BDDMockito.willThrow(new CustomException(ErrorCode.AUTHORIZATION_DENIED))
                    .given(openChatRoomCommandService).deleteOpenChatRoom(openChatId);

            // when & then
            mockMvc.perform(delete("/api/v1/open-chats/{openChatId}", openChatId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .accept(MediaType.APPLICATION_JSON)
                            .headers(getCommonApiHeaders(memberId)))
                    .andExpectAll(
                            status().isForbidden(),
                            jsonPath("$.succeed").value(false),
                            jsonPath("$.code").value(ErrorCode.AUTHORIZATION_DENIED.getCode()),
                            jsonPath("$.message").value(ErrorCode.AUTHORIZATION_DENIED.getMessage())
                    );
        }
    }
}