package com.project200.undabang.chat.service.impl;

import com.project200.undabang.chat.dto.event.ChatMessageCreatedEvent;
import com.project200.undabang.chat.dto.event.ChatroomMemberStatusEvent;
import com.project200.undabang.chat.dto.record.SaveMessageRecord;
import com.project200.undabang.chat.dto.request.CreateChatroomRequest;
import com.project200.undabang.chat.dto.request.CreateMessageRequest;
import com.project200.undabang.chat.dto.response.CreateChatroomResponse;
import com.project200.undabang.chat.dto.response.CreateMessageResponse;
import com.project200.undabang.chat.dto.response.SaveMessageResponse;
import com.project200.undabang.chat.entity.Chat;
import com.project200.undabang.chat.entity.Chatroom;
import com.project200.undabang.chat.entity.ChatroomMember;
import com.project200.undabang.chat.entity.ChatroomMemberStatus;
import com.project200.undabang.chat.repository.ChatRepository;
import com.project200.undabang.chat.repository.ChatroomMemberRepository;
import com.project200.undabang.chat.repository.ChatroomRepository;
import com.project200.undabang.common.context.UserContextHolder;
import com.project200.undabang.common.web.exception.CustomException;
import com.project200.undabang.common.web.exception.ErrorCode;
import com.project200.undabang.member.entity.ExerciseLocation;
import com.project200.undabang.member.entity.Member;
import com.project200.undabang.member.repository.ExerciseLocationRepository;
import com.project200.undabang.member.repository.MemberBlockRepository;
import com.project200.undabang.member.repository.MemberRepository;
import com.project200.undabang.policy.entity.PolicyKey;
import com.project200.undabang.policy.service.PolicyService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatCommandServiceImplTest {

    @InjectMocks
    private ChatCommandServiceImpl chatCommandService;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private ChatroomRepository chatroomRepository;

    @Mock
    private ChatRepository chatRepository;

    @Mock
    private ChatroomMemberRepository chatroomMemberRepository;

    @Mock
    private MemberBlockRepository memberBlockRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private EntityManager em;

    private final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);
    @Mock
    private ExerciseLocationRepository exerciseLocationRepository;
    @Mock
    private PolicyService policyService;

    @Nested
    @DisplayName("createMessage 메소드는")
    class Describe_createMessage {

        private final Long chatroomId = 1L;
        private final String messageContent = "안녕하세요!";

        @Test
        @DisplayName("성공: 메시지를 저장하고, 상태를 업데이트한 뒤 알림 이벤트를 발행한다")
        void it_creates_message_and_publishes_event() {
            // given
            Member member = createMember();
            Chatroom chatroom = createChatroom(chatroomId);
            CreateMessageRequest request = new CreateMessageRequest(messageContent);
            ChatroomMember chatroomMember = createChatroomMember(chatroom, member, ChatroomMemberStatus.ACTIVE);

            Chat savedChat = mock(Chat.class);
            given(savedChat.getId()).willReturn(100L);
            given(savedChat.getChatContent()).willReturn(messageContent);
            given(savedChat.getChatroom()).willReturn(chatroom);
            given(savedChat.getSender()).willReturn(member);

            try (MockedStatic<UserContextHolder> ignored = mockStatic(UserContextHolder.class)) {
                ignored.when(UserContextHolder::getUserId).thenReturn(member.getMemberId());

                given(memberRepository.findById(member.getMemberId())).willReturn(Optional.of(member));
                given(chatroomMemberRepository.findByChatroom_IdAndMember(chatroomId, member)).willReturn(Optional.of(chatroomMember));

                given(chatroomMemberRepository.checkBlockExists(chatroom, member)).willReturn(false);
                given(chatroomMemberRepository.countByChatroomAndChatroomMemberStatus(chatroom, ChatroomMemberStatus.ACTIVE)).willReturn(2L);
                given(chatRepository.save(any(Chat.class))).willReturn(savedChat);

                // when
                CreateMessageResponse response = chatCommandService.createMessage(chatroomId, request);

                // then
                assertThat(response.getChatId()).isEqualTo(100L);
                assertThat(chatroom.getLastChatContent()).isEqualTo(messageContent);
                assertThat(chatroomMember.getLastReadChatId()).isEqualTo(100L);

                verify(chatRepository).save(any(Chat.class));

                ArgumentCaptor<ChatMessageCreatedEvent> eventCaptor = ArgumentCaptor.forClass(ChatMessageCreatedEvent.class);
                verify(eventPublisher).publishEvent(eventCaptor.capture());

                ChatMessageCreatedEvent capturedEvent = eventCaptor.getValue();
                assertThat(capturedEvent.chatId()).isEqualTo(100L);
                assertThat(capturedEvent.chatroomId()).isEqualTo(chatroomId);
                assertThat(capturedEvent.senderId()).isEqualTo(member.getMemberId());
            }
        }

        @Test
        @DisplayName("차단한 사용자에게 메시지를 보내려 하면 예외를 발생시킨다")
        void it_throws_exception_when_sending_message_to_blocked_user() {
            // given
            Member member = createMember();
            Chatroom chatroom = createChatroom(chatroomId);
            CreateMessageRequest request = new CreateMessageRequest(messageContent);
            ChatroomMember chatroomMember = createChatroomMember(chatroom, member, ChatroomMemberStatus.ACTIVE);

            try (MockedStatic<UserContextHolder> ignored = mockStatic(UserContextHolder.class)) {
                ignored.when(UserContextHolder::getUserId).thenReturn(member.getMemberId());

                given(memberRepository.findById(member.getMemberId())).willReturn(Optional.of(member));
                given(chatroomMemberRepository.findByChatroom_IdAndMember(chatroomId, member)).willReturn(Optional.of(chatroomMember));
                given(chatroomMemberRepository.checkBlockExists(chatroom, member)).willReturn(true);

                // when & then
                assertThatThrownBy(() -> chatCommandService.createMessage(chatroomId, request))
                        .isInstanceOf(CustomException.class)
                        .hasMessage(ErrorCode.MESSAGE_SEND_BLOCKED.getMessage());

                verify(chatRepository, never()).save(any());
            }
        }

        @Test
        @DisplayName("요청한 유저가 채팅방 멤버가 아니면 예외를 발생시킨다")
        void it_throws_exception_when_user_is_not_a_chatroom_member() {
            // given
            Member member = createMember();
            CreateMessageRequest request = new CreateMessageRequest(messageContent);

            try (MockedStatic<UserContextHolder> ignored = mockStatic(UserContextHolder.class)) {
                ignored.when(UserContextHolder::getUserId).thenReturn(member.getMemberId());
                given(memberRepository.findById(member.getMemberId())).willReturn(Optional.of(member));
                given(chatroomMemberRepository.findByChatroom_IdAndMember(chatroomId, member)).willReturn(Optional.empty());

                // when & then
                assertThatThrownBy(() -> chatCommandService.createMessage(chatroomId, request))
                        .isInstanceOf(CustomException.class)
                        .hasMessage(ErrorCode.CHATROOM_MEMBERS_NOT_FOUND.getMessage());
            }
        }

        @Test
        @DisplayName("채팅방에 다른 활성 멤버가 없으면 (상대방이 나갔으면) 예외를 발생시킨다")
        void it_throws_exception_when_other_member_is_inactive() {
            // given
            Member member = createMember();
            Chatroom chatroom = createChatroom(chatroomId);
            CreateMessageRequest request = new CreateMessageRequest(messageContent);
            ChatroomMember chatroomMember = createChatroomMember(chatroom, member, ChatroomMemberStatus.ACTIVE);

            try (MockedStatic<UserContextHolder> ignored = mockStatic(UserContextHolder.class)) {
                ignored.when(UserContextHolder::getUserId).thenReturn(member.getMemberId());

                given(memberRepository.findById(member.getMemberId())).willReturn(Optional.of(member));
                given(chatroomMemberRepository.findByChatroom_IdAndMember(chatroomId, member)).willReturn(Optional.of(chatroomMember));
                given(chatroomMemberRepository.checkBlockExists(chatroom, member)).willReturn(false);
                given(chatroomMemberRepository.countByChatroomAndChatroomMemberStatus(chatroom, ChatroomMemberStatus.ACTIVE)).willReturn(1L);

                // when & then
                assertThatThrownBy(() -> chatCommandService.createMessage(chatroomId, request))
                        .isInstanceOf(CustomException.class)
                        .hasMessage(ErrorCode.CHATROOM_OTHER_MEMBER_INACTIVE.getMessage());
            }
        }

        @Test
        @DisplayName("사용자가 메시지를 보낼 수 없는 상태(LEFT)이면 예외를 발생시킨다")
        void it_throws_exception_when_user_status_is_not_active() {
            // given
            Member member = createMember();
            Chatroom chatroom = createChatroom(chatroomId);
            CreateMessageRequest request = new CreateMessageRequest(messageContent);
            ChatroomMember chatroomMember = spy(createChatroomMember(chatroom, member, ChatroomMemberStatus.LEFT));

            try (MockedStatic<UserContextHolder> ignored = mockStatic(UserContextHolder.class)) {
                ignored.when(UserContextHolder::getUserId).thenReturn(member.getMemberId());

                given(memberRepository.findById(member.getMemberId())).willReturn(Optional.of(member));
                given(chatroomMemberRepository.findByChatroom_IdAndMember(chatroomId, member)).willReturn(Optional.of(chatroomMember));

                // when & then
                assertThatThrownBy(() -> chatCommandService.createMessage(chatroomId, request))
                        .isInstanceOf(CustomException.class)
                        .hasMessage(ErrorCode.CHATROOM_MEMBER_INACTIVE.getMessage());
            }
        }

        @Test
        @DisplayName("존재하지 않는 사용자 ID로 요청하면 예외를 발생시킨다")
        void it_throws_exception_when_member_not_found() {
            // given
            UUID nonExistentMemberId = UUID.randomUUID();
            CreateMessageRequest request = new CreateMessageRequest("안녕하세요!");

            try (MockedStatic<UserContextHolder> ignored = mockStatic(UserContextHolder.class)) {
                ignored.when(UserContextHolder::getUserId).thenReturn(nonExistentMemberId);
                given(memberRepository.findById(nonExistentMemberId)).willReturn(Optional.empty());

                // when & then
                assertThatThrownBy(() -> chatCommandService.createMessage(chatroomId, request))
                        .isInstanceOf(CustomException.class)
                        .hasMessage(ErrorCode.MEMBER_NOT_FOUND.getMessage());
            }
        }
    }

    private Member createMember() {
        return Member.builder()
                .memberId(UUID.randomUUID())
                .memberNickname("user_" + UUID.randomUUID().toString().substring(0, 8))
                .build();
    }

    private Chatroom createChatroom(Long id) {
        return Chatroom.builder()
                .id(id)
                .build();
    }

    private ChatroomMember createChatroomMember(Chatroom chatroom, Member member, ChatroomMemberStatus status) {
        return ChatroomMember.builder()
                .chatroom(chatroom)
                .member(member)
                .chatroomMemberStatus(status)
                .build();
    }

    // --- Helper Methods (Moved to Bottom) ---

    private ChatroomMember createChatroomMemberWithId(Long id, Chatroom chatroom, Member member, ChatroomMemberStatus status) {
        return ChatroomMember.builder()
                .chatroomMemberId(id)
                .chatroom(chatroom)
                .member(member)
                .chatroomMemberStatus(status)
                .build();
    }

    private ExerciseLocation createMockLocation(Double lat, Double lon) {
        ExerciseLocation mockLocation = mock(ExerciseLocation.class);

        Point realPoint = geometryFactory.createPoint(new Coordinate(lon, lat));

        lenient().when(mockLocation.getExerciseLocationPoint()).thenReturn(realPoint);

        return mockLocation;
    }

    private void mockMemberLocking(Member member1, Member member2) {
        List<UUID> sortedIds = Stream.of(member1.getMemberId(), member2.getMemberId()).sorted().toList();
        List<Member> sortedMembers = Stream.of(member1, member2)
                .sorted((m1, m2) -> m1.getMemberId().compareTo(m2.getMemberId()))
                .toList();
        given(memberRepository.findAllByIdWithPessimisticLock(sortedIds)).willReturn(sortedMembers);
    }

    @Nested
    @DisplayName("saveMessage 메소드는")
    class Describe_saveMessage {

        private final Long chatroomId = 1L;
        private final String messageContent = "웹소켓 메시지입니다.";

        @Test
        @DisplayName("성공: 메시지를 저장하고 관련 상태를 업데이트하며 이벤트를 발행한다")
        void it_saves_message_successfully() {
            // given
            Member member = createMember();
            Chatroom chatroom = createChatroom(chatroomId);
            SaveMessageRecord record = new SaveMessageRecord(chatroomId, member.getMemberId(), messageContent);
            ChatroomMember chatroomMember = createChatroomMember(chatroom, member, ChatroomMemberStatus.ACTIVE);

            Chat savedChat = mock(Chat.class);
            given(savedChat.getId()).willReturn(100L);
            given(savedChat.getChatContent()).willReturn(messageContent);
            given(savedChat.getChatroom()).willReturn(chatroom);
            given(savedChat.getSender()).willReturn(member);

            given(memberRepository.findMemberWithProfileImage(member.getMemberId())).willReturn(Optional.of(member));
            given(chatroomMemberRepository.findByChatroom_IdAndMember(chatroomId, member)).willReturn(Optional.of(chatroomMember));
            given(chatroomMemberRepository.checkBlockExists(chatroom, member)).willReturn(false);
            given(chatroomMemberRepository.countByChatroomAndChatroomMemberStatus(chatroom, ChatroomMemberStatus.ACTIVE)).willReturn(2L);
            given(chatRepository.save(any(Chat.class))).willReturn(savedChat);

            // when
            SaveMessageResponse response = chatCommandService.saveMessage(record);

            // then
            assertThat(response.getChatId()).isEqualTo(100L);
            assertThat(chatroom.getLastChatContent()).isEqualTo(messageContent);
            assertThat(chatroomMember.getLastReadChatId()).isEqualTo(100L);

            verify(chatRepository).save(any(Chat.class));

            ArgumentCaptor<ChatMessageCreatedEvent> eventCaptor = ArgumentCaptor.forClass(ChatMessageCreatedEvent.class);
            verify(eventPublisher).publishEvent(eventCaptor.capture());

            ChatMessageCreatedEvent capturedEvent = eventCaptor.getValue();
            assertThat(capturedEvent.chatId()).isEqualTo(100L);
            assertThat(capturedEvent.chatroomId()).isEqualTo(chatroomId);
            assertThat(capturedEvent.senderId()).isEqualTo(member.getMemberId());
        }

        @Test
        @DisplayName("존재하지 않는 사용자 ID인 경우 예외를 발생시킨다")
        void it_throws_exception_when_member_not_found() {
            // given
            UUID memberId = UUID.randomUUID();
            SaveMessageRecord record = new SaveMessageRecord(chatroomId, memberId, messageContent);

            given(memberRepository.findMemberWithProfileImage(memberId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> chatCommandService.saveMessage(record))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.MEMBER_NOT_FOUND.getMessage());
        }

        @Test
        @DisplayName("채팅방 멤버가 아닌 경우 예외를 발생시킨다")
        void it_throws_exception_when_not_chatroom_member() {
            // given
            Member member = createMember();
            SaveMessageRecord record = new SaveMessageRecord(chatroomId, member.getMemberId(), messageContent);

            given(memberRepository.findMemberWithProfileImage(member.getMemberId())).willReturn(Optional.of(member));
            given(chatroomMemberRepository.findByChatroom_IdAndMember(chatroomId, member)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> chatCommandService.saveMessage(record))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.CHATROOM_MEMBERS_NOT_FOUND.getMessage());
        }

        @Test
        @DisplayName("차단된 사용자인 경우 예외를 발생시킨다")
        void it_throws_exception_when_blocked() {
            // given
            Member member = createMember();
            Chatroom chatroom = createChatroom(chatroomId);
            SaveMessageRecord record = new SaveMessageRecord(chatroomId, member.getMemberId(), messageContent);
            ChatroomMember chatroomMember = createChatroomMember(chatroom, member, ChatroomMemberStatus.ACTIVE);

            given(memberRepository.findMemberWithProfileImage(member.getMemberId())).willReturn(Optional.of(member));
            given(chatroomMemberRepository.findByChatroom_IdAndMember(chatroomId, member)).willReturn(Optional.of(chatroomMember));
            given(chatroomMemberRepository.checkBlockExists(chatroom, member)).willReturn(true);

            // when & then
            assertThatThrownBy(() -> chatCommandService.saveMessage(record))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.MESSAGE_SEND_BLOCKED.getMessage());
        }

        @Test
        @DisplayName("상대방이 나가서 활성 멤버가 혼자뿐인 경우 예외를 발생시킨다")
        void it_throws_exception_when_other_member_inactive() {
            // given
            Member member = createMember();
            Chatroom chatroom = createChatroom(chatroomId);
            SaveMessageRecord record = new SaveMessageRecord(chatroomId, member.getMemberId(), messageContent);
            ChatroomMember chatroomMember = createChatroomMember(chatroom, member, ChatroomMemberStatus.ACTIVE);

            given(memberRepository.findMemberWithProfileImage(member.getMemberId())).willReturn(Optional.of(member));
            given(chatroomMemberRepository.findByChatroom_IdAndMember(chatroomId, member)).willReturn(Optional.of(chatroomMember));
            given(chatroomMemberRepository.checkBlockExists(chatroom, member)).willReturn(false);
            given(chatroomMemberRepository.countByChatroomAndChatroomMemberStatus(chatroom, ChatroomMemberStatus.ACTIVE)).willReturn(1L);

            // when & then
            assertThatThrownBy(() -> chatCommandService.saveMessage(record))
                    .isInstanceOf(CustomException.class)
                    .hasMessage(ErrorCode.CHATROOM_OTHER_MEMBER_INACTIVE.getMessage());
        }
    }

    @Nested
    @DisplayName("createChatroom 메소드는")
    class Describe_createChatroom {

        // 기준 좌표: 서울역 (37.555946, 126.972317)
        private final Double BASE_LAT = 37.555946;
        private final Double BASE_LON = 126.972317;

        @Test
        @DisplayName("상호 차단된 관계일 경우 예외를 발생시킨다")
        void it_throws_exception_when_members_are_blocked() {
            // given
            Member currentMember = createMember();
            Member targetMember = createMember();
            CreateChatroomRequest request = new CreateChatroomRequest(targetMember.getMemberId(), 1L, BASE_LAT, BASE_LON);

            try (MockedStatic<UserContextHolder> ignored = mockStatic(UserContextHolder.class)) {
                ignored.when(UserContextHolder::getUserId).thenReturn(currentMember.getMemberId());

                given(memberRepository.findById(currentMember.getMemberId())).willReturn(Optional.of(currentMember));
                given(memberRepository.findById(targetMember.getMemberId())).willReturn(Optional.of(targetMember));
                given(memberBlockRepository.checkMemberBlockExists(currentMember, targetMember)).willReturn(true);

                // when & then
                assertThatThrownBy(() -> chatCommandService.createChatroom(request))
                        .isInstanceOf(CustomException.class)
                        .hasMessage(ErrorCode.CHATROOM_CREATE_BLOCKED.getMessage());

                // then
                verify(chatroomRepository, never()).findChatroomBetweenMembers(any(), any());
                verify(memberRepository, never()).findAllByIdWithPessimisticLock(anyList());
            }
        }

        @Test
        @DisplayName("자기 자신과의 채팅방 생성을 시도하면 예외를 발생시킨다")
        void it_throws_exception_when_creating_chatroom_with_oneself() {
            // given
            UUID memberId = UUID.randomUUID();
            CreateChatroomRequest request = new CreateChatroomRequest(memberId, 1L, BASE_LAT, BASE_LON);

            try (MockedStatic<UserContextHolder> ignored = mockStatic(UserContextHolder.class)) {
                ignored.when(UserContextHolder::getUserId).thenReturn(memberId);

                // when & then
                assertThatThrownBy(() -> chatCommandService.createChatroom(request))
                        .isInstanceOf(CustomException.class)
                        .hasMessage(ErrorCode.SELF_CHAT_NOT_ALLOWED.getMessage());
            }
        }

        @Test
        @DisplayName("운동 장소를 찾을 수 없으면 예외를 발생시킨다")
        void it_throws_exception_when_exercise_location_not_found() {
            // given
            Member currentMember = createMember();
            Member targetMember = createMember();
            CreateChatroomRequest request = new CreateChatroomRequest(targetMember.getMemberId(), 999L, BASE_LAT, BASE_LON);

            try (MockedStatic<UserContextHolder> ignored = mockStatic(UserContextHolder.class)) {
                ignored.when(UserContextHolder::getUserId).thenReturn(currentMember.getMemberId());

                given(memberRepository.findById(currentMember.getMemberId())).willReturn(Optional.of(currentMember));
                given(memberRepository.findById(targetMember.getMemberId())).willReturn(Optional.of(targetMember));
                given(memberBlockRepository.checkMemberBlockExists(any(), any())).willReturn(false);

                // 운동 장소 조회 실패 Stubbing
                given(exerciseLocationRepository.findByExerciseLocationIdAndMember_MemberIdAndExerciseLocationDeletedAtNull(request.getExerciseLocationId(), targetMember.getMemberId()))
                        .willReturn(Optional.empty());

                // when & then
                assertThatThrownBy(() -> chatCommandService.createChatroom(request))
                        .isInstanceOf(CustomException.class)
                        .hasMessage(ErrorCode.EXERCISE_LOCATION_NOT_FOUND.getMessage());
            }
        }

        @Test
        @DisplayName("운동 장소와의 거리가 정책상 허용 범위(5km)를 초과하면 예외를 발생시킨다")
        void it_throws_exception_when_distance_is_too_far() {
            // given
            Member currentMember = createMember();
            Member targetMember = createMember();

            // 평양 부근 (아주 먼 거리)
            Double farLat = 39.0339;
            Double farLon = 125.7537;
            CreateChatroomRequest request = new CreateChatroomRequest(targetMember.getMemberId(), 1L, farLat, farLon);

            ExerciseLocation mockLocation = createMockLocation(BASE_LAT, BASE_LON);

            try (MockedStatic<UserContextHolder> ignored = mockStatic(UserContextHolder.class)) {
                ignored.when(UserContextHolder::getUserId).thenReturn(currentMember.getMemberId());
                given(memberRepository.findById(currentMember.getMemberId())).willReturn(Optional.of(currentMember));
                given(memberRepository.findById(targetMember.getMemberId())).willReturn(Optional.of(targetMember));
                given(memberBlockRepository.checkMemberBlockExists(any(), any())).willReturn(false);

                given(exerciseLocationRepository.findByExerciseLocationIdAndMember_MemberIdAndExerciseLocationDeletedAtNull(request.getExerciseLocationId(), targetMember.getMemberId()))
                        .willReturn(Optional.of(mockLocation));

                // [추가] 정책 서비스가 5000.0을 반환하도록 설정
                given(policyService.getPolicyValueAsDouble(PolicyKey.EXERCISE_LOCATION_MAX_DISTANCE_METER)).willReturn(5000.0);

                // when & then
                assertThatThrownBy(() -> chatCommandService.createChatroom(request))
                        .isInstanceOf(CustomException.class)
                        .hasMessage(ErrorCode.CHATROOM_CREATE_TOO_FAR_DISTANCE.getMessage());
            }
        }

        @Test
        @DisplayName("기존 채팅방이 없고 거리 제한을 통과하면 새로운 채팅방을 생성한다")
        void it_creates_new_chatroom_if_not_exists_and_distance_valid() {
            // given
            Member currentMember = createMember();
            Member targetMember = createMember();
            CreateChatroomRequest request = new CreateChatroomRequest(targetMember.getMemberId(), 1L, BASE_LAT + 0.001, BASE_LON);
            Chatroom newChatroom = createChatroom(1L);
            ExerciseLocation mockLocation = createMockLocation(BASE_LAT, BASE_LON);

            try (MockedStatic<UserContextHolder> ignored = mockStatic(UserContextHolder.class)) {
                ignored.when(UserContextHolder::getUserId).thenReturn(currentMember.getMemberId());

                given(memberRepository.findById(currentMember.getMemberId())).willReturn(Optional.of(currentMember));
                given(memberRepository.findById(targetMember.getMemberId())).willReturn(Optional.of(targetMember));
                given(memberBlockRepository.checkMemberBlockExists(any(), any())).willReturn(false);

                given(exerciseLocationRepository.findByExerciseLocationIdAndMember_MemberIdAndExerciseLocationDeletedAtNull(request.getExerciseLocationId(), targetMember.getMemberId()))
                        .willReturn(Optional.of(mockLocation));

                // [추가] 거리 검증을 통과해야 하므로 정책값 설정 필수
                given(policyService.getPolicyValueAsDouble(PolicyKey.EXERCISE_LOCATION_MAX_DISTANCE_METER)).willReturn(5000.0);

                given(chatroomRepository.findChatroomBetweenMembers(currentMember, targetMember))
                        .willReturn(Optional.empty())
                        .willReturn(Optional.empty());

                mockMemberLocking(currentMember, targetMember);
                given(chatroomRepository.save(any(Chatroom.class))).willReturn(newChatroom);

                // when
                CreateChatroomResponse response = chatCommandService.createChatroom(request);

                // then
                assertThat(response.getChatRoomId()).isEqualTo(newChatroom.getId());
                then(chatroomRepository).should(times(1)).save(any(Chatroom.class));
            }
        }

        @Test
        @DisplayName("신규 생성 시, 락을 잡은 후 다른 스레드가 생성한 채팅방을 발견하면 중복 생성하지 않는다")
        void it_returns_existing_chatroom_when_found_after_lock() {
            // given
            Member currentMember = createMember();
            Member targetMember = createMember();
            CreateChatroomRequest request = new CreateChatroomRequest(targetMember.getMemberId(), 1L, BASE_LAT, BASE_LON);
            Chatroom existingChatroom = createChatroom(1L);
            ExerciseLocation mockLocation = createMockLocation(BASE_LAT, BASE_LON);

            try (MockedStatic<UserContextHolder> ignored = mockStatic(UserContextHolder.class)) {
                ignored.when(UserContextHolder::getUserId).thenReturn(currentMember.getMemberId());

                given(memberRepository.findById(currentMember.getMemberId())).willReturn(Optional.of(currentMember));
                given(memberRepository.findById(targetMember.getMemberId())).willReturn(Optional.of(targetMember));
                given(memberBlockRepository.checkMemberBlockExists(any(), any())).willReturn(false);

                given(exerciseLocationRepository.findByExerciseLocationIdAndMember_MemberIdAndExerciseLocationDeletedAtNull(request.getExerciseLocationId(), targetMember.getMemberId()))
                        .willReturn(Optional.of(mockLocation));

                given(chatroomRepository.findChatroomBetweenMembers(currentMember, targetMember))
                        .willReturn(Optional.empty()) // 락 이전 조회
                        .willReturn(Optional.of(existingChatroom)); // 락 이후 조회

                given(policyService.getPolicyValueAsDouble(PolicyKey.EXERCISE_LOCATION_MAX_DISTANCE_METER)).willReturn(5000.0);

                mockMemberLocking(currentMember, targetMember);

                // when
                CreateChatroomResponse response = chatCommandService.createChatroom(request);

                // then
                assertThat(response.getChatRoomId()).isEqualTo(existingChatroom.getId());
                then(memberRepository).should(times(1)).findAllByIdWithPessimisticLock(anyList());
                then(chatroomRepository).should(never()).save(any(Chatroom.class));
            }
        }

        @Test
        @DisplayName("기존 채팅방이 있고 모든 멤버가 활성 상태이면 해당 채팅방을 반환한다")
        void it_returns_existing_chatroom_when_all_members_are_active() {
            // given
            Member currentMember = createMember();
            Member targetMember = createMember();
            CreateChatroomRequest request = new CreateChatroomRequest(targetMember.getMemberId(), 1L, BASE_LAT, BASE_LON);
            Chatroom existingChatroom = createChatroom(1L);
            ExerciseLocation mockLocation = createMockLocation(BASE_LAT, BASE_LON);

            try (MockedStatic<UserContextHolder> ignored = mockStatic(UserContextHolder.class)) {
                ignored.when(UserContextHolder::getUserId).thenReturn(currentMember.getMemberId());

                given(memberRepository.findById(currentMember.getMemberId())).willReturn(Optional.of(currentMember));
                given(memberRepository.findById(targetMember.getMemberId())).willReturn(Optional.of(targetMember));
                given(memberBlockRepository.checkMemberBlockExists(any(), any())).willReturn(false);

                given(exerciseLocationRepository.findByExerciseLocationIdAndMember_MemberIdAndExerciseLocationDeletedAtNull(request.getExerciseLocationId(), targetMember.getMemberId()))
                        .willReturn(Optional.of(mockLocation));

                given(chatroomRepository.findChatroomBetweenMembers(currentMember, targetMember)).willReturn(Optional.of(existingChatroom));
                given(chatroomMemberRepository.countByChatroomAndChatroomMemberStatus(existingChatroom, ChatroomMemberStatus.ACTIVE)).willReturn(2L);
                given(policyService.getPolicyValueAsDouble(PolicyKey.EXERCISE_LOCATION_MAX_DISTANCE_METER)).willReturn(5000.0);

                // when
                CreateChatroomResponse response = chatCommandService.createChatroom(request);

                // then
                assertThat(response.getChatRoomId()).isEqualTo(existingChatroom.getId());
                then(memberRepository).should(never()).findAllByIdWithPessimisticLock(anyList());
                verify(chatroomMemberRepository, never()).saveAll(any());
            }
        }

        @Test
        @DisplayName("기존 채팅방에 상대방만 나간 상태일 경우 재활성화하고 채팅방을 반환한다")
        void it_reactivates_and_returns_chatroom_when_target_member_left() {
            // given
            Member currentMember = createMember();
            Member targetMember = createMember();
            CreateChatroomRequest request = new CreateChatroomRequest(targetMember.getMemberId(), 1L, BASE_LAT, BASE_LON);
            Chatroom existingChatroom = createChatroom(1L);
            ChatroomMember currentChatroomMember = spy(createChatroomMemberWithId(2L, existingChatroom, currentMember, ChatroomMemberStatus.ACTIVE));
            ChatroomMember targetChatroomMember = spy(createChatroomMemberWithId(3L, existingChatroom, targetMember, ChatroomMemberStatus.LEFT));
            ExerciseLocation mockLocation = createMockLocation(BASE_LAT, BASE_LON);

            try (MockedStatic<UserContextHolder> ignored = mockStatic(UserContextHolder.class)) {
                ignored.when(UserContextHolder::getUserId).thenReturn(currentMember.getMemberId());

                given(memberRepository.findById(currentMember.getMemberId())).willReturn(Optional.of(currentMember));
                given(memberRepository.findById(targetMember.getMemberId())).willReturn(Optional.of(targetMember));
                given(memberBlockRepository.checkMemberBlockExists(any(), any())).willReturn(false);

                given(exerciseLocationRepository.findByExerciseLocationIdAndMember_MemberIdAndExerciseLocationDeletedAtNull(request.getExerciseLocationId(), targetMember.getMemberId()))
                        .willReturn(Optional.of(mockLocation));

                given(chatroomRepository.findChatroomBetweenMembers(currentMember, targetMember)).willReturn(Optional.of(existingChatroom));
                given(chatroomMemberRepository.countByChatroomAndChatroomMemberStatus(existingChatroom, ChatroomMemberStatus.ACTIVE)).willReturn(1L);
                given(chatroomMemberRepository.findByChatroomAndMember(existingChatroom, currentMember)).willReturn(Optional.of(currentChatroomMember));
                given(chatroomMemberRepository.findByChatroomAndMember(existingChatroom, targetMember)).willReturn(Optional.of(targetChatroomMember));
                given(policyService.getPolicyValueAsDouble(PolicyKey.EXERCISE_LOCATION_MAX_DISTANCE_METER)).willReturn(5000.0);

                // when
                CreateChatroomResponse response = chatCommandService.createChatroom(request);

                // then
                assertThat(response.getChatRoomId()).isEqualTo(existingChatroom.getId());
                then(currentChatroomMember).should().updateMemberStatus(ChatroomMemberStatus.ACTIVE);
                then(targetChatroomMember).should().updateMemberStatus(ChatroomMemberStatus.ACTIVE);
                then(memberRepository).should(never()).findAllByIdWithPessimisticLock(anyList());
            }
        }
    }

    @Nested
    @DisplayName("deleteChatroom 메소드는")
    class Describe_deleteChatroom {

        private final Long chatroomId = 1L;

        @Test
        @DisplayName("성공: 활성 멤버가 채팅방을 나가면 상태를 LEFT로 변경하고 시스템 메시지를 저장하며, 이벤트를 발행한다")
        void it_leaves_chatroom_successfully() {
            // given
            Member member = createMember();
            Chatroom chatroom = spy(createChatroom(chatroomId));
            ChatroomMember chatroomMember = spy(createChatroomMember(chatroom, member, ChatroomMemberStatus.ACTIVE));
            String systemMessageContent = "사용자님이 나갔습니다";

            // 시스템 메시지용 Mock Chat 객체 생성
            Chat savedSystemChat = mock(Chat.class);
            given(savedSystemChat.getChatContent()).willReturn(systemMessageContent);

            try (MockedStatic<UserContextHolder> ignored = mockStatic(UserContextHolder.class)) {
                ignored.when(UserContextHolder::getUserId).thenReturn(member.getMemberId());
                given(memberRepository.findById(member.getMemberId())).willReturn(Optional.of(member));
                given(chatroomMemberRepository.findByChatroom_IdAndMember(chatroomId, member)).willReturn(Optional.of(chatroomMember));
                given(chatroomMemberRepository.countByChatroomAndChatroomMemberStatus(chatroom, ChatroomMemberStatus.ACTIVE)).willReturn(1L);

                // 시스템 메시지 저장 시 Mock 객체 반환
                given(chatRepository.save(any(Chat.class))).willReturn(savedSystemChat);

                // when
                chatCommandService.leaveChatroom(chatroomId);

                // then
                then(chatroomMember).should(times(1)).updateMemberStatus(ChatroomMemberStatus.LEFT);
                then(chatroom).should(never()).deleteChatroom();
                verify(chatRepository, times(1)).save(any(Chat.class));

                // [추가된 부분] 이벤트 발행 검증
                ArgumentCaptor<ChatroomMemberStatusEvent> eventCaptor = ArgumentCaptor.forClass(ChatroomMemberStatusEvent.class);
                verify(eventPublisher).publishEvent(eventCaptor.capture());

                ChatroomMemberStatusEvent capturedEvent = eventCaptor.getValue();
                assertThat(capturedEvent.chatroomId()).isEqualTo(chatroomId);
                assertThat(capturedEvent.chatContent()).isEqualTo(systemMessageContent);
            }
        }

        @Test
        @DisplayName("성공: 마지막 활성 멤버가 나가면 채팅방도 논리적으로 삭제한다")
        void it_deletes_chatroom_when_last_member_leaves() {
            // given
            Member member = createMember();
            Chatroom chatroom = spy(createChatroom(chatroomId));
            ChatroomMember chatroomMember = createChatroomMember(chatroom, member, ChatroomMemberStatus.ACTIVE);

            Chat savedSystemChat = mock(Chat.class);
            given(savedSystemChat.getChatContent()).willReturn("나감");

            try (MockedStatic<UserContextHolder> ignored = mockStatic(UserContextHolder.class)) {
                ignored.when(UserContextHolder::getUserId).thenReturn(member.getMemberId());
                given(memberRepository.findById(member.getMemberId())).willReturn(Optional.of(member));
                given(chatroomMemberRepository.findByChatroom_IdAndMember(chatroomId, member)).willReturn(Optional.of(chatroomMember));
                given(chatroomMemberRepository.countByChatroomAndChatroomMemberStatus(chatroom, ChatroomMemberStatus.ACTIVE)).willReturn(0L);
                given(chatRepository.save(any(Chat.class))).willReturn(savedSystemChat);

                // when
                chatCommandService.leaveChatroom(chatroomId);

                // then
                then(chatroom).should(times(1)).deleteChatroom();
                verify(chatRepository, times(1)).save(any(Chat.class));

                // 이벤트 발행 검증 (삭제 시에도 나감 메시지는 전송됨)
                verify(eventPublisher).publishEvent(any(ChatroomMemberStatusEvent.class));
            }
        }

        @Test
        @DisplayName("성공: 이미 나간 멤버가 다시 나가기를 요청하면 아무 작업도 하지 않고 성공 처리한다")
        void it_does_nothing_if_member_already_left() {
            // given
            Member member = createMember();
            Chatroom chatroom = createChatroom(chatroomId);
            ChatroomMember chatroomMember = createChatroomMember(chatroom, member, ChatroomMemberStatus.LEFT);

            try (MockedStatic<UserContextHolder> ignored = mockStatic(UserContextHolder.class)) {
                ignored.when(UserContextHolder::getUserId).thenReturn(member.getMemberId());
                given(memberRepository.findById(member.getMemberId())).willReturn(Optional.of(member));
                given(chatroomMemberRepository.findByChatroom_IdAndMember(chatroomId, member)).willReturn(Optional.of(chatroomMember));

                // when
                chatCommandService.leaveChatroom(chatroomId);

                // then
                verify(chatroomMemberRepository, never()).countByChatroomAndChatroomMemberStatus(any(Chatroom.class), any(ChatroomMemberStatus.class));
                verify(chatRepository, never()).save(any(Chat.class));
                verify(eventPublisher, never()).publishEvent(any());
            }
        }

        @Test
        @DisplayName("실패: 채팅방 멤버가 아닐 경우 예외를 발생시킨다")
        void it_throws_exception_if_not_a_member() {
            // given
            Member member = createMember();

            try (MockedStatic<UserContextHolder> ignored = mockStatic(UserContextHolder.class)) {
                ignored.when(UserContextHolder::getUserId).thenReturn(member.getMemberId());
                given(memberRepository.findById(member.getMemberId())).willReturn(Optional.of(member));
                given(chatroomMemberRepository.findByChatroom_IdAndMember(chatroomId, member)).willReturn(Optional.empty());

                // when & then
                assertThatThrownBy(() -> chatCommandService.leaveChatroom(chatroomId))
                        .isInstanceOf(CustomException.class)
                        .hasMessage(ErrorCode.CHATROOM_MEMBERS_NOT_FOUND.getMessage());
            }
        }
    }
}