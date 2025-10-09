package com.project200.undabang.chat.controller;

import com.project200.undabang.chat.dto.response.GetMemberChatroomResponse;
import com.project200.undabang.chat.service.ChatQueryService;
import com.project200.undabang.common.web.exception.CustomException;
import com.project200.undabang.common.web.exception.ErrorCode;
import com.project200.undabang.common.web.response.CommonResponse;
import com.project200.undabang.configuration.AbstractRestDocSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.BDDMockito;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static com.project200.undabang.configuration.HeadersGenerator.getCommonApiHeaders;
import static com.project200.undabang.configuration.RestDocsUtils.HEADER_ACCESS_TOKEN;
import static com.project200.undabang.configuration.RestDocsUtils.commonResponseFieldsForList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ChatQueryController.class)
class ChatQueryControllerTest extends AbstractRestDocSupport {

    @MockitoBean
    private ChatQueryService chatQueryService;

    private List<GetMemberChatroomResponse> createRichChatroomResponseList(LocalDateTime now) {
        return List.of(
                createChatroomResponse(101L, "운동메이트 김민준", "네, 그럼 내일 6시에 헬스장에서 뵐게요!", 2L, now.minusMinutes(5)),
                createChatroomResponse(102L, "트레이너 C", "이모티콘", 0L, now.minusMinutes(30)),
                createChatroomResponse(103L, "박서준 축구", "안녕하세요, 오늘 회의록 정리해서 보내드립니다. 검토해보시고 피드백 부탁드리겠습니다. 내용이 길어서...", 1L, now.minusHours(3)),
                createChatroomResponse(104L, "런닝 이지은", "사진", 0L, now.minusDays(1).withHour(18).withMinute(30)),
                createChatroomResponseWithNoProfile(105L, "(알수없음)", "혹시 중고거래 가능하신가요?", 1L, now.minusDays(3)),
                createChatroomResponse(106L, "마성의 러너", "이번주 정모는 강남역 OOO에서 진행됩니다. 꼭 참석해주세요!", 99L, now.minusWeeks(1)),
                createChatroomResponseWithNoChat(107L, "엣지러너", "https://example.com/profile_new.jpg", "https://example.com/thumbnail_new.jpg")
        );
    }

    private GetMemberChatroomResponse createChatroomResponse(Long chatRoomId, String otherNickname, String lastChat, Long unreadCount, LocalDateTime receivedAt) {
        return GetMemberChatroomResponse.builder()
                .chatRoomId(chatRoomId)
                .otherMemberNickname(otherNickname)
                .otherMemberProfileImageUrl("https://example.com/profile_" + chatRoomId + ".jpg")
                .otherMemberThumbnailImageUrl("https://example.com/thumbnail_" + chatRoomId + ".jpg")
                .lastChatContent(lastChat)
                .lastChatReceivedAt(receivedAt)
                .unreadCount(unreadCount)
                .build();
    }

    private GetMemberChatroomResponse createChatroomResponseWithNoProfile(Long chatRoomId, String otherNickname, String lastChat, Long unreadCount, LocalDateTime receivedAt) {
        return GetMemberChatroomResponse.builder()
                .chatRoomId(chatRoomId)
                .otherMemberNickname(otherNickname)
                .otherMemberProfileImageUrl(null)
                .otherMemberThumbnailImageUrl(null)
                .lastChatContent(lastChat)
                .lastChatReceivedAt(receivedAt)
                .unreadCount(unreadCount)
                .build();
    }

    private GetMemberChatroomResponse createChatroomResponseWithNoChat(Long chatRoomId, String otherNickname, String profileUrl, String thumbUrl) {
        return GetMemberChatroomResponse.builder()
                .chatRoomId(chatRoomId)
                .otherMemberNickname(otherNickname)
                .otherMemberProfileImageUrl(profileUrl)
                .otherMemberThumbnailImageUrl(thumbUrl)
                .lastChatContent(null)
                .lastChatReceivedAt(null)
                .unreadCount(0L)
                .build();
    }

    @Nested
    @DisplayName("GET /api/v1/chat-rooms API는")
    class GetChatRoomList {

        @Test
        @DisplayName("다양한 종류의 채팅방 목록을 성공적으로 조회한다")
        void getChatRoomList_Success() throws Exception {
            // given: 실제 앱과 유사한 풍부한 Mock 데이터 준비 (최신순으로 정렬됨)
            UUID memberId = UUID.randomUUID();
            LocalDateTime now = LocalDateTime.now();

            List<GetMemberChatroomResponse> richResponseList = createRichChatroomResponseList(now);

            BDDMockito.given(chatQueryService.getMemberChatroomList()).willReturn(richResponseList);

            // when
            String response = mockMvc.perform(get("/api/v1/chat-rooms")
                            .contentType(MediaType.APPLICATION_JSON)
                            .accept(MediaType.APPLICATION_JSON)
                            .headers(getCommonApiHeaders(memberId)))
                    .andExpect(status().isOk())
                    // then: 응답 본문의 정렬 순서 및 주요 값 검증
                    .andExpect(jsonPath("$.data[0].chatRoomId").value(101L)) // 가장 최신
                    .andExpect(jsonPath("$.data[0].unreadCount").value(2))
                    .andExpect(jsonPath("$.data[4].otherMemberProfileImageUrl").doesNotExist()) // 프로필 없는 유저
                    .andExpect(jsonPath("$.data[6].lastChatContent").doesNotExist()) // 대화 없는 방
                    // andDo: API 문서화 수행
                    .andDo(document.document(
                            requestHeaders(HEADER_ACCESS_TOKEN),
                            responseFields(commonResponseFieldsForList(
                                    fieldWithPath("data[].chatRoomId").type(JsonFieldType.NUMBER).description("채팅방 식별자 정보 입니다."),
                                    fieldWithPath("data[].otherMemberNickname").type(JsonFieldType.STRING).description("상대방 닉네임 정보 입니다."),
                                    fieldWithPath("data[].otherMemberProfileImageUrl").type(JsonFieldType.STRING).description("상대방 프로필 이미지 URL 입니다. 프로필 이미지가 없는 경우 기본 이미지를 사용해주세요").optional(),
                                    fieldWithPath("data[].otherMemberThumbnailImageUrl").type(JsonFieldType.STRING).description("상대방 썸네일 이미지 URL 입니다. 썸네일 이미지가 없는 경우 프로필 이미지를 사용해주세요").optional(),
                                    fieldWithPath("data[].lastChatContent").type(JsonFieldType.STRING).description("특정 채팅방의 마지막 대화 내용 입니다.").optional(),
                                    fieldWithPath("data[].lastChatReceivedAt").type(JsonFieldType.STRING).description("특정 채팅방의 마지막 대화 수신 시간을 나타냅니다.").optional(),
                                    fieldWithPath("data[].unreadCount").type(JsonFieldType.NUMBER).description("특정 채팅방의 읽지 않은 메시지 수를 의미합니다.")
                            ))
                    ))
                    .andReturn().getResponse().getContentAsString();

            // then: 전체 응답 본문 비교
            String expected = objectMapper.writeValueAsString(CommonResponse.success(richResponseList));
            assertThat(response).isEqualTo(expected);
            BDDMockito.then(chatQueryService).should(BDDMockito.times(1)).getMemberChatroomList();
        }

        @Test
        @DisplayName("참여중인 채팅방이 없을 경우 빈 목록을 반환한다")
        void getChatRoomList_Success_Empty() throws Exception {
            // given
            UUID memberId = UUID.randomUUID();
            BDDMockito.given(chatQueryService.getMemberChatroomList()).willReturn(Collections.emptyList());

            // when & then
            mockMvc.perform(get("/api/v1/chat-rooms")
                            .headers(getCommonApiHeaders(memberId)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").isEmpty())
                    .andDo(document.document(
                            requestHeaders(HEADER_ACCESS_TOKEN),
                            responseFields(commonResponseFieldsForList(
                                    // data가 빈 배열일 경우, 내부 필드를 기술하지 않습니다.
                            ))
                    ));
        }

        @Test
        @DisplayName("사용자를 찾을 수 없을 경우 404 Not Found를 반환한다")
        void getChatRoomList_Fail_MemberNotFound() throws Exception {
            // given
            UUID memberId = UUID.randomUUID();
            BDDMockito.given(chatQueryService.getMemberChatroomList()).willThrow(new CustomException(ErrorCode.MEMBER_NOT_FOUND));

            // when & then
            mockMvc.perform(get("/api/v1/chat-rooms")
                            .headers(getCommonApiHeaders(memberId)))
                    .andExpect(status().isNotFound());

            BDDMockito.then(chatQueryService).should(BDDMockito.times(1)).getMemberChatroomList();
        }
    }
}