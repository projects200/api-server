package com.project200.undabang.openchat.service.impl;

import com.project200.undabang.common.context.UserContextHolder;
import com.project200.undabang.common.web.exception.CustomException;
import com.project200.undabang.common.web.exception.ErrorCode;
import com.project200.undabang.member.entity.Member;
import com.project200.undabang.member.enums.MemberGender;
import com.project200.undabang.member.repository.MemberRepository;
import com.project200.undabang.openchat.dto.request.CreateOpenChatRoomRequest;
import com.project200.undabang.openchat.dto.request.UpdateOpenChatRoomRequest;
import com.project200.undabang.openchat.entity.OpenChatRoom;
import com.project200.undabang.openchat.repository.OpenChatRoomRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OpenChatRoomCommandServiceImplTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private OpenChatRoomRepository openChatRoomRepository;

    @InjectMocks
    private OpenChatRoomCommandServiceImpl openChatRoomCommandService;

    @Nested
    @DisplayName("createOpenChatRoom() 메소드는")
    class Describe_createOpenChatRoom {

        @Test
        @DisplayName("정상 생성 시 생성된 id를 반환한다")
        void creates_open_chat_room() {
            UUID userId = UUID.randomUUID();
            Member member = createMember(userId);
            String url = "https://open.chat/new";
            CreateOpenChatRoomRequest request = createRequest(url);

            try (MockedStatic<UserContextHolder> ignored = mockStatic(UserContextHolder.class)) {
                given(UserContextHolder.getUserId()).willReturn(userId);
                given(memberRepository.findById(userId)).willReturn(Optional.of(member));
                given(openChatRoomRepository.existsByMemberAndDeletedAtNull(member)).willReturn(false);
                given(openChatRoomRepository.existsByUrlAndDeletedAtNull(url)).willReturn(false);

                OpenChatRoom saved = mock(OpenChatRoom.class);
                given(saved.getId()).willReturn(1L);
                given(openChatRoomRepository.save(any(OpenChatRoom.class))).willReturn(saved);

                var response = openChatRoomCommandService.createOpenChatRoom(request);

                assertThat(response).isNotNull();
                assertThat(response.getOpenChatroomId()).isEqualTo(1L);
            }
        }

        @Test
        @DisplayName("존재하지 않는 회원이면 MEMBER_NOT_FOUND 예외")
        void throws_when_member_not_found() {
            // GIVEN
            UUID userId = UUID.randomUUID();
            String url = "https://open.chat/new";
            CreateOpenChatRoomRequest request = createRequest(url);

            try (MockedStatic<UserContextHolder> ignored = mockStatic(UserContextHolder.class)) {
                given(UserContextHolder.getUserId()).willReturn(userId);
                given(memberRepository.findById(userId)).willReturn(Optional.empty());

                // WHEN & THEN
                assertThatThrownBy(() -> openChatRoomCommandService.createOpenChatRoom(request))
                        .isInstanceOf(CustomException.class)
                        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MEMBER_NOT_FOUND);
            }
        }

        @Test
        @DisplayName("이미 오픈채팅이 존재하면 OPEN_CHAT_ROOM_ALREADY_EXIST 예외")
        void throws_when_already_exists() {
            // GIVEN
            UUID userId = UUID.randomUUID();
            Member member = createMember(userId);
            String url = "https://open.chat/exist";
            CreateOpenChatRoomRequest request = createRequest(url);

            try (MockedStatic<UserContextHolder> ignored = mockStatic(UserContextHolder.class)) {
                given(UserContextHolder.getUserId()).willReturn(userId);
                given(memberRepository.findById(userId)).willReturn(Optional.of(member));
                given(openChatRoomRepository.existsByMemberAndDeletedAtNull(member)).willReturn(true);

                // WHEN & THEN
                assertThatThrownBy(() -> openChatRoomCommandService.createOpenChatRoom(request))
                        .isInstanceOf(CustomException.class)
                        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.OPEN_CHAT_ROOM_ALREADY_EXIST);
            }
        }

        @Test
        @DisplayName("사용 중인 URL이면 OPEN_CHAT_ROOM_URL_DUPLICATED 예외")
        void throws_when_url_is_duplicated_on_precheck() {
            // GIVEN
            UUID userId = UUID.randomUUID();
            Member member = createMember(userId);
            String url = "https://open.chat/duplicated";
            CreateOpenChatRoomRequest request = createRequest(url);

            try (MockedStatic<UserContextHolder> ignored = mockStatic(UserContextHolder.class)) {
                given(UserContextHolder.getUserId()).willReturn(userId);
                given(memberRepository.findById(userId)).willReturn(Optional.of(member));
                given(openChatRoomRepository.existsByMemberAndDeletedAtNull(member)).willReturn(false); // 1차 검사 통과
                given(openChatRoomRepository.existsByUrlAndDeletedAtNull(url)).willReturn(true); // 2차 검사 실패

                // WHEN & THEN
                assertThatThrownBy(() -> openChatRoomCommandService.createOpenChatRoom(request))
                        .isInstanceOf(CustomException.class)
                        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.OPEN_CHAT_ROOM_URL_DUPLICATED);
            }
        }


        @Test
        @DisplayName("저장 시 DataIntegrityViolationException 발생하면 OPEN_CHAT_ROOM_URL_DUPLICATED 예외")
        void throws_when_save_duplicate_url() {
            // GIVEN
            UUID userId = UUID.randomUUID();
            Member member = createMember(userId);
            String url = "https://open.chat/dup";
            CreateOpenChatRoomRequest request = createRequest(url);

            try (MockedStatic<UserContextHolder> ignored = mockStatic(UserContextHolder.class)) {
                given(UserContextHolder.getUserId()).willReturn(userId);
                given(memberRepository.findById(userId)).willReturn(Optional.of(member));
                given(openChatRoomRepository.existsByMemberAndDeletedAtNull(member)).willReturn(false);
                given(openChatRoomRepository.existsByUrlAndDeletedAtNull(url)).willReturn(false);
                given(openChatRoomRepository.save(any(OpenChatRoom.class))).willThrow(new DataIntegrityViolationException("dup"));

                // WHEN & THEN
                assertThatThrownBy(() -> openChatRoomCommandService.createOpenChatRoom(request))
                        .isInstanceOf(CustomException.class)
                        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.OPEN_CHAT_ROOM_URL_DUPLICATED);
            }
        }

        @Test
        @DisplayName("http로 시작하는 URL은 https로 정규화되어 생성된다")
        void normalizes_http_url_to_https() {
            // GIVEN
            UUID userId = UUID.randomUUID();
            Member member = createMember(userId);
            String httpUrl = "http://open.chat/new";
            String expectedHttpsUrl = "https://open.chat/new";
            CreateOpenChatRoomRequest request = createRequest(httpUrl);

            // ArgumentCaptor를 사용하여 OpenChatRoom.of()에 전달된 URL을 캡처
            ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);

            try (
                    MockedStatic<UserContextHolder> userContextHolder = mockStatic(UserContextHolder.class);
                    MockedStatic<OpenChatRoom> openChatRoomStatic = mockStatic(OpenChatRoom.class)
            ) {
                userContextHolder.when(UserContextHolder::getUserId).thenReturn(userId);
                given(memberRepository.findById(userId)).willReturn(Optional.of(member));
                given(openChatRoomRepository.existsByMemberAndDeletedAtNull(member)).willReturn(false);
                given(openChatRoomRepository.existsByUrlAndDeletedAtNull(expectedHttpsUrl)).willReturn(false);

                OpenChatRoom mockedOpenChatRoom = mock(OpenChatRoom.class);
                OpenChatRoom saved = mock(OpenChatRoom.class);

                openChatRoomStatic.when(() -> OpenChatRoom.of(eq(member), urlCaptor.capture()))
                        .thenReturn(mockedOpenChatRoom);

                given(saved.getId()).willReturn(1L);
                given(openChatRoomRepository.save(mockedOpenChatRoom)).willReturn(saved);

                // WHEN
                openChatRoomCommandService.createOpenChatRoom(request);

                // THEN
                // 캡처된 URL이 예상대로 https로 변경되었는지 검증
                assertThat(urlCaptor.getValue()).isEqualTo(expectedHttpsUrl);
            }
        }
    }

    @Nested
    @DisplayName("updateOpenChatRoom() 메소드는")
    class Describe_updateOpenChatRoom {

        @Test
        @DisplayName("유효한 다른 URL로 정상 수정된다")
        void updates_successfully_with_different_url() {
            // GIVEN
            UUID userId = UUID.randomUUID();
            Member member = createMember(userId);
            Long openChatId = 10L;
            String oldUrl = "https://open.chat/old";
            String newUrl = "https://open.chat/new";
            UpdateOpenChatRoomRequest request = updateRequest(newUrl);

            OpenChatRoom existing = mock(OpenChatRoom.class);
            given(existing.getMember()).willReturn(member);
            given(existing.isSameUrl(newUrl)).willReturn(false);
            given(existing.getId()).willReturn(openChatId);

            given(openChatRoomRepository.findByIdAndDeletedAtNull(openChatId)).willReturn(Optional.of(existing));
            given(openChatRoomRepository.existsByUrlAndIdNotAndDeletedAtNull(newUrl, openChatId)).willReturn(false);

            try (MockedStatic<UserContextHolder> ignored = mockStatic(UserContextHolder.class)) {
                given(UserContextHolder.getUserId()).willReturn(userId);
                given(memberRepository.findById(userId)).willReturn(Optional.of(member));

                // WHEN
                var response = openChatRoomCommandService.updateOpenChatRoom(openChatId, request);

                // THEN
                verify(existing).updateOpenChatUrl(newUrl);
                assertThat(response).isNotNull();
                assertThat(response.getOpenChatroomId()).isEqualTo(openChatId);
            }
        }

        @Test
        @DisplayName("같은 URL이면 변경 없이 id를 반환한다")
        void returns_id_when_same_url() {
            // GIVEN
            UUID userId = UUID.randomUUID();
            Member member = createMember(userId);
            Long openChatId = 10L;
            String url = "https://open.chat/same";
            UpdateOpenChatRoomRequest request = updateRequest(url);

            OpenChatRoom existing = mock(OpenChatRoom.class);
            given(existing.getMember()).willReturn(member);
            given(existing.isSameUrl(url)).willReturn(true);

            given(openChatRoomRepository.findByIdAndDeletedAtNull(openChatId)).willReturn(Optional.of(existing));

            try (MockedStatic<UserContextHolder> ignored = mockStatic(UserContextHolder.class)) {
                given(UserContextHolder.getUserId()).willReturn(userId);
                given(memberRepository.findById(userId)).willReturn(Optional.of(member));

                // WHEN
                var response = openChatRoomCommandService.updateOpenChatRoom(openChatId, request);

                // THEN
                assertThat(response).isNotNull();
                assertThat(response.getOpenChatroomId()).isEqualTo(openChatId);
                verify(existing, never()).updateOpenChatUrl(anyString());
            }
        }

        @Test
        @DisplayName("수정하려는 오픈채팅이 없으면 OPEN_CHAT_ROOM_NOT_FOUND 예외")
        void throws_when_chat_room_to_update_not_found() {
            // GIVEN
            UUID userId = UUID.randomUUID();
            Member member = createMember(userId);
            Long nonExistentChatId = 99L;
            String url = "https://open.chat/other";
            UpdateOpenChatRoomRequest request = updateRequest(url);

            given(openChatRoomRepository.findByIdAndDeletedAtNull(nonExistentChatId)).willReturn(Optional.empty());

            try (MockedStatic<UserContextHolder> ignored = mockStatic(UserContextHolder.class)) {
                given(UserContextHolder.getUserId()).willReturn(userId);
                given(memberRepository.findById(userId)).willReturn(Optional.of(member));

                // WHEN & THEN
                assertThatThrownBy(() -> openChatRoomCommandService.updateOpenChatRoom(nonExistentChatId, request))
                        .isInstanceOf(CustomException.class)
                        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.OPEN_CHAT_ROOM_NOT_FOUND);
            }
        }

        @Test
        @DisplayName("다른 사용자의 오픈채팅이면 AUTHORIZATION_DENIED 예외")
        void throws_when_not_owner_on_update() {
            // GIVEN
            UUID userId = UUID.randomUUID();
            UUID otherId = UUID.randomUUID();
            Member member = createMember(userId);
            Member owner = createMember(otherId);
            Long openChatId = 11L;
            String url = "https://open.chat/other";
            UpdateOpenChatRoomRequest request = updateRequest(url);

            OpenChatRoom existing = mock(OpenChatRoom.class);
            given(existing.getMember()).willReturn(owner);
            given(openChatRoomRepository.findByIdAndDeletedAtNull(openChatId)).willReturn(Optional.of(existing));

            try (MockedStatic<UserContextHolder> ignored = mockStatic(UserContextHolder.class)) {
                given(UserContextHolder.getUserId()).willReturn(userId);
                given(memberRepository.findById(userId)).willReturn(Optional.of(member));

                // WHEN & THEN
                assertThatThrownBy(() -> openChatRoomCommandService.updateOpenChatRoom(openChatId, request))
                        .isInstanceOf(CustomException.class)
                        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.AUTHORIZATION_DENIED);
            }
        }

        @Test
        @DisplayName("변경하려는 URL이 이미 사용중이면 OPEN_CHAT_ROOM_URL_DUPLICATED 예외")
        void throws_when_new_url_is_duplicated() {
            // GIVEN
            UUID userId = UUID.randomUUID();
            Member member = createMember(userId);
            Long openChatId = 12L;
            String newUrl = "https://open.chat/duplicated";
            UpdateOpenChatRoomRequest request = updateRequest(newUrl);

            OpenChatRoom existing = mock(OpenChatRoom.class);
            given(existing.getMember()).willReturn(member);
            given(existing.isSameUrl(newUrl)).willReturn(false);

            given(openChatRoomRepository.findByIdAndDeletedAtNull(openChatId)).willReturn(Optional.of(existing));
            given(openChatRoomRepository.existsByUrlAndIdNotAndDeletedAtNull(newUrl, openChatId)).willReturn(true);

            try (MockedStatic<UserContextHolder> ignored = mockStatic(UserContextHolder.class)) {
                given(UserContextHolder.getUserId()).willReturn(userId);
                given(memberRepository.findById(userId)).willReturn(Optional.of(member));

                // WHEN & THEN
                assertThatThrownBy(() -> openChatRoomCommandService.updateOpenChatRoom(openChatId, request))
                        .isInstanceOf(CustomException.class)
                        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.OPEN_CHAT_ROOM_URL_DUPLICATED);
            }
        }

        @Test
        @DisplayName("http로 시작하는 URL로 수정하면 https로 정규화되어 updateOpenChatUrl이 호출된다")
        void normalizes_http_url_to_https_on_update() {
            UUID userId = UUID.randomUUID();
            Member member = createMember(userId);
            Long openChatId = 30L;
            String httpUrl = "http://open.chat/update";
            String expectedHttpsUrl = "https://open.chat/update";
            UpdateOpenChatRoomRequest request = updateRequest(httpUrl);

            OpenChatRoom existing = mock(OpenChatRoom.class);
            given(existing.getMember()).willReturn(member);
            given(existing.isSameUrl(expectedHttpsUrl)).willReturn(false);
            given(existing.getId()).willReturn(openChatId);

            given(openChatRoomRepository.findByIdAndDeletedAtNull(openChatId)).willReturn(Optional.of(existing));
            given(openChatRoomRepository.existsByUrlAndIdNotAndDeletedAtNull(expectedHttpsUrl, openChatId)).willReturn(false);

            try (MockedStatic<UserContextHolder> ignored = mockStatic(UserContextHolder.class)) {
                given(UserContextHolder.getUserId()).willReturn(userId);
                given(memberRepository.findById(userId)).willReturn(Optional.of(member));

                var response = openChatRoomCommandService.updateOpenChatRoom(openChatId, request);

                ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
                verify(existing).updateOpenChatUrl(urlCaptor.capture());
                assertThat(urlCaptor.getValue()).isEqualTo(expectedHttpsUrl);
                assertThat(response).isNotNull();
                assertThat(response.getOpenChatroomId()).isEqualTo(openChatId);
            }
        }
    }

    @Nested
    @DisplayName("deleteOpenChatRoom() 메소드는")
    class Describe_deleteOpenChatRoom {

        @Test
        @DisplayName("정상 삭제는 softDelete 호출")
        void deletes_successfully() {
            // GIVEN
            UUID userId = UUID.randomUUID();
            Member member = createMember(userId);
            Long openChatId = 20L;

            OpenChatRoom existing = mock(OpenChatRoom.class);
            given(existing.getMember()).willReturn(member);
            given(openChatRoomRepository.findByIdAndDeletedAtNull(openChatId)).willReturn(Optional.of(existing));

            try (MockedStatic<UserContextHolder> ignored = mockStatic(UserContextHolder.class)) {
                given(UserContextHolder.getUserId()).willReturn(userId);
                given(memberRepository.findById(userId)).willReturn(Optional.of(member));

                // WHEN
                openChatRoomCommandService.deleteOpenChatRoom(openChatId);

                // THEN
                verify(existing).softDelete();
            }
        }

        @Test
        @DisplayName("삭제하려는 오픈채팅이 없으면 OPEN_CHAT_ROOM_NOT_FOUND 예외")
        void throws_when_chat_room_to_delete_not_found() {
            // GIVEN
            UUID userId = UUID.randomUUID();
            Member member = createMember(userId);
            Long nonExistentChatId = 99L;

            given(openChatRoomRepository.findByIdAndDeletedAtNull(nonExistentChatId)).willReturn(Optional.empty());

            try (MockedStatic<UserContextHolder> ignored = mockStatic(UserContextHolder.class)) {
                given(UserContextHolder.getUserId()).willReturn(userId);
                given(memberRepository.findById(userId)).willReturn(Optional.of(member));

                // WHEN & THEN
                assertThatThrownBy(() -> openChatRoomCommandService.deleteOpenChatRoom(nonExistentChatId))
                        .isInstanceOf(CustomException.class)
                        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.OPEN_CHAT_ROOM_NOT_FOUND);
            }
        }

        @Test
        @DisplayName("삭제 시 소유자가 아니면 AUTHORIZATION_DENIED 예외")
        void throws_when_not_owner_on_delete() {
            // GIVEN
            UUID userId = UUID.randomUUID();
            UUID otherId = UUID.randomUUID();
            Member member = createMember(userId);
            Member owner = createMember(otherId);
            Long openChatId = 21L;

            OpenChatRoom existing = mock(OpenChatRoom.class);
            given(existing.getMember()).willReturn(owner);
            given(openChatRoomRepository.findByIdAndDeletedAtNull(openChatId)).willReturn(Optional.of(existing));

            try (MockedStatic<UserContextHolder> ignored = mockStatic(UserContextHolder.class)) {
                given(UserContextHolder.getUserId()).willReturn(userId);
                given(memberRepository.findById(userId)).willReturn(Optional.of(member));

                // WHEN & THEN
                assertThatThrownBy(() -> openChatRoomCommandService.deleteOpenChatRoom(openChatId))
                        .isInstanceOf(CustomException.class)
                        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.AUTHORIZATION_DENIED);
            }
        }
    }

    private Member createMember(UUID memberId) {
        return Member.builder()
                .memberId(memberId)
                .memberEmail(memberId + "@test.com")
                .memberNickname("test-user-" + memberId)
                .memberGender(MemberGender.UNKNOWN)
                .memberBday(LocalDate.now().minusYears(20))
                .build();
    }

    private CreateOpenChatRoomRequest createRequest(String url) {
        return new CreateOpenChatRoomRequest() {
            @Override
            public String getOpenChatroomUrl() {
                return url;
            }
        };
    }

    private UpdateOpenChatRoomRequest updateRequest(String url) {
        return new UpdateOpenChatRoomRequest() {
            @Override
            public String getOpenChatroomUrl() {
                return url;
            }
        };
    }
}