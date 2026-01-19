package com.project200.undabang.member.service.impl;

import com.project200.undabang.common.context.UserContextHolder;
import com.project200.undabang.common.web.exception.CustomException;
import com.project200.undabang.common.web.exception.ErrorCode;
import com.project200.undabang.member.dto.event.MemberBlockedEvent;
import com.project200.undabang.member.dto.response.CreateMemberBlockResponse;
import com.project200.undabang.member.entity.Member;
import com.project200.undabang.member.entity.MemberBlock;
import com.project200.undabang.member.repository.MemberBlockRepository;
import com.project200.undabang.member.repository.MemberRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MemberBlockCommandServiceImplTest {

    @InjectMocks
    private MemberBlockCommandServiceImpl memberBlockCommandService;

    @Mock
    private MemberBlockRepository memberBlockRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private Member createMember(UUID id, String nickname) {
        return Member.builder()
                .memberId(id)
                .memberNickname(nickname)
                .memberEmail(nickname + "@test.com")
                .memberBday(LocalDate.of(2000, 1, 1))
                .build();
    }

    private MemberBlock createSoftDeleteMemberBlock(long memberBlockId, Member blocker, Member blocked) {
        return MemberBlock.builder()
                .id(memberBlockId)
                .blocker(blocker)
                .blocked(blocked)
                .memberBlockCreatedAt(LocalDateTime.now())
                .memberBlockDeletedAt(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("createMemberBlock 메소드는")
    class Describe_createMemberBlock {

        @Nested
        @DisplayName("유효한 차단 요청이 주어지고, 기존 차단 이력이 없다면")
        class Context_with_valid_new_block_request {

            @Test
            @DisplayName("새로운 차단 정보를 저장하고 이벤트를 발행한다")
            void it_creates_new_block_and_publishes_event() {
                // given
                final UUID MEMBER_ID = UUID.randomUUID();
                final UUID BLOCKED_MEMBER_ID = UUID.randomUUID();
                Member blocker = createMember(MEMBER_ID, "차단하는사람");
                Member blocked = createMember(BLOCKED_MEMBER_ID, "차단당하는사람");

                MemberBlock savedBlockMock = mock(MemberBlock.class);
                when(savedBlockMock.getId()).thenReturn(1L);

                try (MockedStatic<UserContextHolder> mocked = mockStatic(UserContextHolder.class)) {
                    mocked.when(UserContextHolder::getUserId).thenReturn(MEMBER_ID);
                    when(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.of(blocker));
                    when(memberRepository.findById(BLOCKED_MEMBER_ID)).thenReturn(Optional.of(blocked));
                    when(memberBlockRepository.findByBlockerAndBlocked(blocker, blocked)).thenReturn(Optional.empty());
                    when(memberBlockRepository.save(any(MemberBlock.class))).thenReturn(savedBlockMock);

                    // when
                    CreateMemberBlockResponse response = memberBlockCommandService.createMemberBlock(BLOCKED_MEMBER_ID);

                    // then
                    assertThat(response).isNotNull();
                    assertThat(response.getMemberBlockId()).isEqualTo(1L);

                    verify(memberBlockRepository, times(1)).save(any(MemberBlock.class));
                    verify(eventPublisher, times(1)).publishEvent(any(MemberBlockedEvent.class));
                }
            }
        }

        @Nested
        @DisplayName("과거에 차단했다가 해제한 이력이 있다면 (재차단)")
        class Context_with_history_of_unblock {

            @Test
            @DisplayName("기존 차단 정보를 다시 활성화하고 이벤트를 발행한다")
            void it_reactivates_existing_block_and_publishes_event() {
                // given
                final UUID MEMBER_ID = UUID.randomUUID();
                final UUID BLOCKED_MEMBER_ID = UUID.randomUUID();
                Member blocker = createMember(MEMBER_ID, "차단하는사람");
                Member blocked = createMember(BLOCKED_MEMBER_ID, "차단당하는사람");
                MemberBlock existingBlock = createSoftDeleteMemberBlock(1L, blocker, blocked);

                try (MockedStatic<UserContextHolder> mocked = mockStatic(UserContextHolder.class)) {
                    mocked.when(UserContextHolder::getUserId).thenReturn(MEMBER_ID);
                    when(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.of(blocker));
                    when(memberRepository.findById(BLOCKED_MEMBER_ID)).thenReturn(Optional.of(blocked));
                    when(memberBlockRepository.findByBlockerAndBlocked(blocker, blocked)).thenReturn(Optional.of(existingBlock));

                    // when
                    CreateMemberBlockResponse response = memberBlockCommandService.createMemberBlock(BLOCKED_MEMBER_ID);

                    // then
                    assertThat(response).isNotNull();
                    assertThat(existingBlock.getMemberBlockDeletedAt()).isNull(); // reBlock() 확인

                    verify(memberBlockRepository, never()).save(any(MemberBlock.class)); // save가 아닌 update(dirty checking)
                    verify(eventPublisher, times(1)).publishEvent(any(MemberBlockedEvent.class));
                }
            }
        }

        @Nested
        @DisplayName("이미 차단된 상태라면")
        class Context_already_blocked {

            @Test
            @DisplayName("CustomException(MEMBER_BLOCK_DUPLICATED)을 던진다")
            void it_throws_duplicate_exception() {
                // given
                final UUID MEMBER_ID = UUID.randomUUID();
                final UUID BLOCKED_MEMBER_ID = UUID.randomUUID();
                Member blocker = createMember(MEMBER_ID, "차단하는사람");
                Member blocked = createMember(BLOCKED_MEMBER_ID, "차단당하는사람");
                MemberBlock existingBlock = MemberBlock.of(blocker, blocked);

                try (MockedStatic<UserContextHolder> mocked = mockStatic(UserContextHolder.class)) {
                    mocked.when(UserContextHolder::getUserId).thenReturn(MEMBER_ID);
                    when(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.of(blocker));
                    when(memberRepository.findById(BLOCKED_MEMBER_ID)).thenReturn(Optional.of(blocked));
                    when(memberBlockRepository.findByBlockerAndBlocked(blocker, blocked)).thenReturn(Optional.of(existingBlock));

                    // when & then
                    assertThatThrownBy(() -> memberBlockCommandService.createMemberBlock(BLOCKED_MEMBER_ID))
                            .isInstanceOf(CustomException.class)
                            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MEMBER_BLOCK_DUPLICATED);
                }
            }
        }

        @Nested
        @DisplayName("자기 자신을 차단하려고 하면")
        class Context_self_blocking {

            @Test
            @DisplayName("CustomException(SELF_REQUEST_NOT_ALLOWED)을 던진다")
            void it_throws_self_request_exception() {
                // given
                final UUID MEMBER_ID = UUID.randomUUID();

                try (MockedStatic<UserContextHolder> mocked = mockStatic(UserContextHolder.class)) {
                    mocked.when(UserContextHolder::getUserId).thenReturn(MEMBER_ID);

                    // when & then
                    assertThatThrownBy(() -> memberBlockCommandService.createMemberBlock(MEMBER_ID))
                            .isInstanceOf(CustomException.class)
                            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MEMBER_SELF_REQUEST_NOT_ALLOWED);
                }
            }
        }

        @Nested
        @DisplayName("차단할 회원이 존재하지 않으면")
        class Context_target_not_found {

            @Test
            @DisplayName("CustomException(MEMBER_NOT_FOUND)을 던진다")
            void it_throws_member_not_found_exception() {
                // given
                final UUID MEMBER_ID = UUID.randomUUID();
                final UUID BLOCKED_MEMBER_ID = UUID.randomUUID();
                Member blocker = createMember(MEMBER_ID, "차단하는사람");

                try (MockedStatic<UserContextHolder> mocked = mockStatic(UserContextHolder.class)) {
                    mocked.when(UserContextHolder::getUserId).thenReturn(MEMBER_ID);
                    when(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.of(blocker));
                    when(memberRepository.findById(BLOCKED_MEMBER_ID)).thenReturn(Optional.empty());

                    // when & then
                    assertThatThrownBy(() -> memberBlockCommandService.createMemberBlock(BLOCKED_MEMBER_ID))
                            .isInstanceOf(CustomException.class)
                            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MEMBER_NOT_FOUND);
                }
            }
        }
    }

    @Nested
    @DisplayName("unBlockMember 메소드는")
    class Describe_unBlockMember {

        @Nested
        @DisplayName("존재하는 차단 이력에 대해 해제 요청을 하면")
        class Context_with_existing_block {

            @Test
            @DisplayName("성공적으로 차단을 해제(Soft Delete)한다")
            void it_unblocks_member_successfully() {
                // given
                final UUID MEMBER_ID = UUID.randomUUID();
                final UUID BLOCKED_MEMBER_ID = UUID.randomUUID();
                Member blocker = createMember(MEMBER_ID, "차단하는사람");
                Member blocked = createMember(BLOCKED_MEMBER_ID, "차단당하는사람");
                MemberBlock existingBlock = MemberBlock.of(blocker, blocked);

                try (MockedStatic<UserContextHolder> mocked = mockStatic(UserContextHolder.class)) {
                    mocked.when(UserContextHolder::getUserId).thenReturn(MEMBER_ID);
                    when(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.of(blocker));
                    when(memberRepository.findById(BLOCKED_MEMBER_ID)).thenReturn(Optional.of(blocked));
                    when(memberBlockRepository.findByBlockerAndBlockedAndMemberBlockDeletedAtNull(blocker, blocked))
                            .thenReturn(Optional.of(existingBlock));

                    // when
                    memberBlockCommandService.unBlockMember(BLOCKED_MEMBER_ID);

                    // then
                    assertThat(existingBlock.getMemberBlockDeletedAt()).isNotNull();
                }
            }
        }

        @Nested
        @DisplayName("차단 이력이 존재하지 않는데 해제하려 하면")
        class Context_with_no_block_history {

            @Test
            @DisplayName("CustomException(MEMBER_BLOCK_NOT_FOUND)를 던진다")
            void it_throws_block_not_found_exception() {
                // given
                final UUID MEMBER_ID = UUID.randomUUID();
                final UUID BLOCKED_MEMBER_ID = UUID.randomUUID();
                Member blocker = createMember(MEMBER_ID, "차단하는사람");
                Member blocked = createMember(BLOCKED_MEMBER_ID, "차단당하는사람");

                try (MockedStatic<UserContextHolder> mocked = mockStatic(UserContextHolder.class)) {
                    mocked.when(UserContextHolder::getUserId).thenReturn(MEMBER_ID);
                    when(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.of(blocker));
                    when(memberRepository.findById(BLOCKED_MEMBER_ID)).thenReturn(Optional.of(blocked));
                    when(memberBlockRepository.findByBlockerAndBlockedAndMemberBlockDeletedAtNull(blocker, blocked))
                            .thenReturn(Optional.empty());

                    // when & then
                    assertThatThrownBy(() -> memberBlockCommandService.unBlockMember(BLOCKED_MEMBER_ID))
                            .isInstanceOf(CustomException.class)
                            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MEMBER_BLOCK_NOT_FOUND);
                }
            }
        }
    }
}