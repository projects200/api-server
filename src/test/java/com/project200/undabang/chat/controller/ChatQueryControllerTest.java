package com.project200.undabang.chat.controller;

import com.project200.undabang.chat.dto.response.ChatMessageDto;
import com.project200.undabang.chat.dto.response.GetMemberChatResponse;
import com.project200.undabang.chat.dto.response.GetMemberChatroomResponse;
import com.project200.undabang.chat.dto.response.GetNewChatResponse;
import com.project200.undabang.chat.entity.ChatType;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static com.project200.undabang.configuration.DocumentFormatGenerator.getTypeFormat;
import static com.project200.undabang.configuration.HeadersGenerator.getCommonApiHeaders;
import static com.project200.undabang.configuration.RestDocsUtils.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ChatQueryController.class)
class ChatQueryControllerTest extends AbstractRestDocSupport {

    @MockitoBean
    private ChatQueryService chatQueryService;

    private ChatMessageDto createChatMessageResponse(Long chatId, String content, boolean isMine) {
        return new ChatMessageDto(
                chatId,
                UUID.randomUUID(),
                isMine ? "currentUser" : "otherUser",
                "http://profile.url",
                "http://thumbnail.url",
                content,
                ChatType.USER,
                LocalDateTime.now(),
                isMine
        );
    }

    @Nested
    @DisplayName("GET /api/v1/chat-rooms/{chatroomId}/messages API는")
    class GetChatMessages {

        @Test
        @DisplayName("채팅방의 메시지 목록과 상태 정보를 성공적으로 조회한다")
        void getMessages_Success() throws Exception {
            // given
            Long chatroomId = 1L;
            Long prevChatId = 11L;
            int size = 10;
            UUID memberId = UUID.randomUUID();

            List<ChatMessageDto> messages = List.of(createChatMessageResponse(10L, "이전 메시지", false));
            Pageable pageable = PageRequest.of(0, size);
            Slice<ChatMessageDto> mockSlice = new SliceImpl<>(messages, pageable, false); // 마지막 페이지라고 가정

            // [수정] opponentBlocked 필드를 포함한 Mock 응답 생성
            GetMemberChatResponse mockResponse = GetMemberChatResponse.from(mockSlice, true, false); // 상대방 활성, 차단 안됨

            given(chatQueryService.getMemberChat(anyLong(), any(), any(Pageable.class)))
                    .willReturn(mockResponse);

            // when & then
            mockMvc.perform(get("/api/v1/chat-rooms/{chatroomId}/messages", chatroomId)
                            .param("prevChatId", String.valueOf(prevChatId))
                            .param("size", String.valueOf(size))
                            .headers(getCommonApiHeaders(memberId))
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content").isArray())
                    .andExpect(jsonPath("$.data.hasNext").value(false))
                    .andExpect(jsonPath("$.data.opponentActive").value(true))
                    // [추가] opponentBlocked 필드 검증
                    .andExpect(jsonPath("$.data.opponentBlocked").value(false))
                    .andDo(document.document(
                            requestHeaders(HEADER_ACCESS_TOKEN),
                            pathParameters(
                                    parameterWithName("chatroomId").attributes(getTypeFormat(JsonFieldType.NUMBER)).description("채팅방의 식별자 ID")
                            ),
                            queryParameters(
                                    parameterWithName("prevChatId").attributes(getTypeFormat(JsonFieldType.NUMBER)).description("이전 페이지의 마지막 메시지 ID (마지막 커서 위치)를 의미합니다. 첫 페이지 조회 시에는 생략합니다.").optional(),
                                    parameterWithName("size").attributes(getTypeFormat(JsonFieldType.NUMBER)).description("한 페이지에 보여줄 메시지의 개수를 의미합니다.").optional()
                            ),
                            responseFields(commonResponseFields(
                                    fieldWithPath("data.content[]").type(JsonFieldType.ARRAY).description("메시지 객체 목록"),
                                    fieldWithPath("data.content[].chatId").type(JsonFieldType.NUMBER).description("메시지 식별자 ID를 의미합니다."),
                                    fieldWithPath("data.content[].senderId").type(JsonFieldType.STRING).description("발신자의 회원 식별자 정보 (UUID)를 의미합니다."),
                                    fieldWithPath("data.content[].senderNickname").type(JsonFieldType.STRING).description("발신자 닉네임을 의미합니다."),
                                    fieldWithPath("data.content[].senderProfileUrl").type(JsonFieldType.STRING).description("발신자 프로필 이미지 URL을 의미합니다. 만약 존재하지 않으면 기본 이미지를 사용하셔야 합니다.").optional(),
                                    fieldWithPath("data.content[].senderThumbnailUrl").type(JsonFieldType.STRING).description("발신자 썸네일 이미지 URL을 의미합니다. 만약 존재하지 않으면 프로필 이미지를 사용하셔야 합니다.").optional(),
                                    fieldWithPath("data.content[].chatContent").type(JsonFieldType.STRING).description("메시지 내용입니다."),
                                    fieldWithPath("data.content[].chatType").type(JsonFieldType.STRING).description("메시지 타입을 의미합니다. 발신한 사람이 USER인지 SYSTEM인지를 나타냅니다."),
                                    fieldWithPath("data.content[].sentAt").type(JsonFieldType.STRING).description("메시지 발신 시간을 의미합니다."),
                                    fieldWithPath("data.content[].mine").type(JsonFieldType.BOOLEAN).description("내(기기 소유주)가 보낸 메시지가 맞는지 확인하는 컬럼입니다."),
                                    fieldWithPath("data.hasNext").type(JsonFieldType.BOOLEAN).description("다음에 조회할 페이지가 있는지 여부를 나타냅니다."),
                                    fieldWithPath("data.opponentActive").type(JsonFieldType.BOOLEAN).description("상대방이 현재 채팅방에 참여중(ACTIVE)인지 여부를 나타냅니다."),
                                    fieldWithPath("data.opponentBlocked").type(JsonFieldType.BOOLEAN).description("내가 상대방을 차단했는지 여부를 나타냅니다.")
                            ))
                    ));
        }

        @Test
        @DisplayName("사용자가 채팅방 멤버가 아닐 경우 404 NOT FOUND를 반환한다")
        void getMessages_Fail_AccessDenied() throws Exception {
            // 이 테스트는 서비스 계층에서 예외를 던지는 것을 테스트하므로 변경할 필요가 없습니다.
            // given
            Long chatroomId = 1L;
            UUID memberId = UUID.randomUUID();

            given(chatQueryService.getMemberChat(anyLong(), any(), any(Pageable.class)))
                    .willThrow(new CustomException(ErrorCode.CHATROOM_MEMBERS_NOT_FOUND));

            // when & then
            mockMvc.perform(get("/api/v1/chat-rooms/{chatroomId}/messages", chatroomId)
                            .headers(getCommonApiHeaders(memberId))
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/chat-rooms/{chatroomId}/messages/new API는")
    class GetNewMessages {

        private final Long chatroomId = 1L;

        @Test
        @DisplayName("새로운 메시지와 상태 정보를 성공적으로 조회한다")
        void getNewMessages_Success() throws Exception {
            // given
            UUID memberId = UUID.randomUUID();

            // 서비스가 반환할 Mock 응답 데이터 생성
            List<ChatMessageDto> newMessages = List.of(
                    createChatMessageResponse(101L, "새로운 메시지 1", false),
                    createChatMessageResponse(102L, "새로운 메시지 2", true)
            );
            GetNewChatResponse mockResponse = GetNewChatResponse.of(newMessages, true); // 상대방 활성 상태

            given(chatQueryService.getNewChat(chatroomId)).willReturn(mockResponse);

            // when & then
            mockMvc.perform(get("/api/v1/chat-rooms/{chatroomId}/messages/new", chatroomId)
                            .headers(getCommonApiHeaders(memberId))
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.succeed").value(true))
                    .andExpect(jsonPath("$.data.newChats").isArray())
                    .andExpect(jsonPath("$.data.newChats[0].chatId").value(101L))
                    .andExpect(jsonPath("$.data.opponentActive").value(true))
                    .andDo(document.document(
                            requestHeaders(HEADER_ACCESS_TOKEN),
                            pathParameters(
                                    parameterWithName("chatroomId").attributes(getTypeFormat(JsonFieldType.NUMBER)).description("새로운 메시지를 조회할 채팅방의 식별자 ID")
                            ),
                            responseFields(commonResponseFields(
                                    fieldWithPath("data.newChats[]").type(JsonFieldType.ARRAY).description("새로운 메시지 객체 목록. 새로운 메시지가 없으면 빈 배열([])을 반환합니다."),
                                    fieldWithPath("data.newChats[].chatId").type(JsonFieldType.NUMBER).description("채팅 식별자값을 의미합니다."),
                                    fieldWithPath("data.newChats[].senderId").type(JsonFieldType.STRING).description("발신자의 회원 식별자 정보(UUID)를 의미합니다."),
                                    fieldWithPath("data.newChats[].senderNickname").type(JsonFieldType.STRING).description("발신자가 현재 사용하고 있는 닉네임 정보를 의미합니다."),
                                    fieldWithPath("data.newChats[].senderProfileUrl").type(JsonFieldType.STRING).description("발신자 프로필 이미지 URL 링크입니다. 없으면 기본 프로필 사진을 사용하시면 됩니다.").optional(),
                                    fieldWithPath("data.newChats[].senderThumbnailUrl").type(JsonFieldType.STRING).description("발신자 썸네일 이미지 URL 링크입니다. 없으면 프로필 이미지 사진을 사용하시면 됩니다.").optional(),
                                    fieldWithPath("data.newChats[].chatContent").type(JsonFieldType.STRING).description("메시지 내용을 의미합니다."),
                                    fieldWithPath("data.newChats[].chatType").type(JsonFieldType.STRING).description("메시지 타입을 의미합니다. 유저와 시스템중 하나의 정보만을 담고 있습니다."),
                                    fieldWithPath("data.newChats[].sentAt").type(JsonFieldType.STRING).description("메시지 발신 시간을 의미합니다."),
                                    fieldWithPath("data.newChats[].mine").type(JsonFieldType.BOOLEAN).description("로그인한 회원이 보낸 메시지인지 여부를 나타냅니다."),
                                    fieldWithPath("data.opponentActive").type(JsonFieldType.BOOLEAN).description("상대방이 현재 채팅방에 참여중인지 여부를 나타냅니다.")
                            ))
                    ));
        }

        @Test
        @DisplayName("새로운 메시지가 없을 경우, 빈 배열과 상태 정보를 반환한다")
        void getNewMessages_Success_Empty() throws Exception {
            // given
            UUID memberId = UUID.randomUUID();
            // 새로운 메시지가 없는 상황의 Mock 응답
            GetNewChatResponse mockResponse = GetNewChatResponse.of(Collections.emptyList(), true);

            given(chatQueryService.getNewChat(chatroomId)).willReturn(mockResponse);

            // when & then
            mockMvc.perform(get("/api/v1/chat-rooms/{chatroomId}/messages/new", chatroomId)
                            .headers(getCommonApiHeaders(memberId))
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.newChats").isEmpty())
                    .andExpect(jsonPath("$.data.opponentActive").value(true));
        }

        @Test
        @DisplayName("사용자가 채팅방 멤버가 아닐 경우 404 NOT FOUND를 반환한다")
        void getNewMessages_Fail_NotMember() throws Exception {
            // given
            UUID memberId = UUID.randomUUID();

            given(chatQueryService.getNewChat(chatroomId))
                    .willThrow(new CustomException(ErrorCode.CHATROOM_MEMBERS_NOT_FOUND));

            // when & then
            mockMvc.perform(get("/api/v1/chat-rooms/{chatroomId}/messages/new", chatroomId)
                            .headers(getCommonApiHeaders(memberId))
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.succeed").value(false))
                    .andExpect(jsonPath("$.code").value(ErrorCode.CHATROOM_MEMBERS_NOT_FOUND.getCode()));
        }
    }

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
                .otherMemberId(UUID.randomUUID()) // 추가된 필드
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
                .otherMemberId(UUID.randomUUID()) // 추가된 필드
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
                .otherMemberId(UUID.randomUUID()) // 추가된 필드
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

            given(chatQueryService.getMemberChatroomList()).willReturn(richResponseList);

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
                                    fieldWithPath("data[].otherMemberId").type(JsonFieldType.STRING).description("채팅방을 유지중인 다른 회원의 회원 식별자 정보 입니다."),
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
            given(chatQueryService.getMemberChatroomList()).willReturn(Collections.emptyList());

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
            given(chatQueryService.getMemberChatroomList()).willThrow(new CustomException(ErrorCode.MEMBER_NOT_FOUND));

            // when & then
            mockMvc.perform(get("/api/v1/chat-rooms")
                            .headers(getCommonApiHeaders(memberId)))
                    .andExpect(status().isNotFound());

            BDDMockito.then(chatQueryService).should(BDDMockito.times(1)).getMemberChatroomList();
        }
    }
}