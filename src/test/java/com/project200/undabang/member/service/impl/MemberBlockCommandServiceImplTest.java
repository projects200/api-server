package com.project200.undabang.member.service.impl;

import com.project200.undabang.common.context.UserContextHolder;
import com.project200.undabang.common.web.exception.CustomException;
import com.project200.undabang.common.web.exception.ErrorCode;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
    @DisplayName("CreateMemberBlock 메소드는")
    class Describe_CreateMemberBlock {

        private final UUID MEMBER_ID = UUID.randomUUID();
        private final UUID BLOCKED_MEMBER_ID = UUID.randomUUID();

        @Test
        @DisplayName("성공적으로 새로운 차단을 생성한다")
        void it_creates_a_new_block_successfully() {
            // given
            Member blocker = createMember(MEMBER_ID, "차단하는사람");
            Member blocked = createMember(BLOCKED_MEMBER_ID, "차단당하는사람");

            MemberBlock savedBlockMock = mock(MemberBlock.class);
            when(savedBlockMock.getId()).thenReturn(1L);

            when(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.of(blocker));
            when(memberRepository.findById(BLOCKED_MEMBER_ID)).thenReturn(Optional.of(blocked));
            when(memberBlockRepository.findByBlockerAndBlocked(blocker, blocked)).thenReturn(Optional.empty());
            when(memberBlockRepository.save(any(MemberBlock.class))).thenReturn(savedBlockMock);

            // when
            CreateMemberBlockResponse response;
            try (MockedStatic<UserContextHolder> mocked = mockStatic(UserContextHolder.class)) {
                mocked.when(UserContextHolder::getUserId).thenReturn(MEMBER_ID);
                response = memberBlockCommandService.createMemberBlock(BLOCKED_MEMBER_ID);
            }

            // then
            assertThat(response).isNotNull();
            assertThat(response.getMemberBlockId()).isEqualTo(1L);
            verify(memberBlockRepository, times(1)).save(any(MemberBlock.class));
        }

        @Test
        @DisplayName("이미 차단된 사용자를 다시 차단하려고 하면 예외를 발생시킨다")
        void it_throws_exception_when_blocking_an_already_blocked_member() {
            // given
            Member blocker = createMember(MEMBER_ID, "차단하는사람");
            Member blocked = createMember(BLOCKED_MEMBER_ID, "차단당하는사람");

            MemberBlock existingBlock = MemberBlock.of(blocker, blocked);

            when(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.of(blocker));
            when(memberRepository.findById(BLOCKED_MEMBER_ID)).thenReturn(Optional.of(blocked));
            when(memberBlockRepository.findByBlockerAndBlocked(blocker, blocked)).thenReturn(Optional.of(existingBlock));

            // when & then
            try (MockedStatic<UserContextHolder> mocked = mockStatic(UserContextHolder.class)) {
                mocked.when(UserContextHolder::getUserId).thenReturn(MEMBER_ID);

                CustomException exception = assertThrows(CustomException.class, () ->
                        memberBlockCommandService.createMemberBlock(BLOCKED_MEMBER_ID)
                );

                assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.MEMBER_BLOCK_DUPLICATED);
            }
        }

        @Test
        @DisplayName("차단 해제했던 사용자를 다시 차단한다 (재차단)")
        void it_reblocks_a_previously_unblocked_member() {
            // given
            long memberBlockId = 1L;
            Member blocker = createMember(MEMBER_ID, "차단하는사람");
            Member blocked = createMember(BLOCKED_MEMBER_ID, "차단당하는사람");

            MemberBlock existingBlock = createSoftDeleteMemberBlock(memberBlockId, blocker, blocked);

            when(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.of(blocker));
            when(memberRepository.findById(BLOCKED_MEMBER_ID)).thenReturn(Optional.of(blocked));
            when(memberBlockRepository.findByBlockerAndBlocked(blocker, blocked)).thenReturn(Optional.of(existingBlock));

            // when
            CreateMemberBlockResponse response;
            try (MockedStatic<UserContextHolder> mocked = mockStatic(UserContextHolder.class)) {
                mocked.when(UserContextHolder::getUserId).thenReturn(MEMBER_ID);
                response = memberBlockCommandService.createMemberBlock(BLOCKED_MEMBER_ID);
            }

            // then
            assertThat(response).isNotNull();
            assertThat(existingBlock.getMemberBlockDeletedAt()).isNull(); // reBlock() 호출로 상태가 변경되었는지 검증
        }

        @Test
        @DisplayName("자기 자신을 차단하려고 하면 예외를 발생시킨다")
        void it_throws_exception_when_blocking_oneself() {
            // given
            UUID SAME_ID = UUID.randomUUID();

            // when & then
            try (MockedStatic<UserContextHolder> mocked = mockStatic(UserContextHolder.class)) {
                mocked.when(UserContextHolder::getUserId).thenReturn(SAME_ID);

                CustomException exception = assertThrows(CustomException.class, () ->
                        memberBlockCommandService.createMemberBlock(SAME_ID)
                );

                assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.MEMBER_SELF_REQUEST_NOT_ALLOWED);
            }
        }

        @Test
        @DisplayName("차단하려는 대상 회원이 존재하지 않으면 예외를 발생시킨다")
        void it_throws_exception_when_blocked_member_not_found() {
            // given
            Member blocker = createMember(MEMBER_ID, "차단하는사람");
            when(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.of(blocker));
            when(memberRepository.findById(BLOCKED_MEMBER_ID)).thenReturn(Optional.empty());

            // when & then
            try (MockedStatic<UserContextHolder> mocked = mockStatic(UserContextHolder.class)) {
                mocked.when(UserContextHolder::getUserId).thenReturn(MEMBER_ID);

                CustomException exception = assertThrows(CustomException.class, () ->
                        memberBlockCommandService.createMemberBlock(BLOCKED_MEMBER_ID)
                );

                assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.MEMBER_NOT_FOUND);
            }
        }
    }

    @Nested
    @DisplayName("deleteMemberBlock 메소드는")
    class Describe_deleteMemberBlock {
        private final UUID MEMBER_ID = UUID.randomUUID();
        private final UUID BLOCKED_MEMBER_ID = UUID.randomUUID();

        @Test
        @DisplayName("유효한 차단 기록을 성공적으로 해제한다")
        void it_unblocks_a_member_successfully() {
            // given
            Member blocker = createMember(MEMBER_ID, "차단하는사람");
            Member blocked = createMember(BLOCKED_MEMBER_ID, "차단당하는사람");
            MemberBlock existingBlock = MemberBlock.of(blocker, blocked);

            when(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.of(blocker));
            when(memberRepository.findById(BLOCKED_MEMBER_ID)).thenReturn(Optional.of(blocked));
            when(memberBlockRepository.findByBlockerAndBlockedAndMemberBlockDeletedAtNull(blocker, blocked))
                    .thenReturn(Optional.of(existingBlock));

            // when
            try (MockedStatic<UserContextHolder> mocked = mockStatic(UserContextHolder.class)) {
                mocked.when(UserContextHolder::getUserId).thenReturn(MEMBER_ID);
                memberBlockCommandService.unBlockMember(BLOCKED_MEMBER_ID);
            }

            // then
            assertThat(existingBlock.getMemberBlockDeletedAt()).isNotNull();
        }

        @Test
        @DisplayName("차단 기록이 존재하지 않을 때 해제하려고 하면 예외를 발생시킨다")
        void it_throws_exception_when_block_not_found() {
            // given
            Member blocker = createMember(MEMBER_ID, "차단하는사람");
            Member blocked = createMember(BLOCKED_MEMBER_ID, "차단당하는사람");

            when(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.of(blocker));
            when(memberRepository.findById(BLOCKED_MEMBER_ID)).thenReturn(Optional.of(blocked));
            when(memberBlockRepository.findByBlockerAndBlockedAndMemberBlockDeletedAtNull(blocker, blocked))
                    .thenReturn(Optional.empty());

            // when & then
            try (MockedStatic<UserContextHolder> mocked = mockStatic(UserContextHolder.class)) {
                mocked.when(UserContextHolder::getUserId).thenReturn(MEMBER_ID);

                CustomException exception = assertThrows(CustomException.class, () ->
                        memberBlockCommandService.unBlockMember(BLOCKED_MEMBER_ID)
                );

                assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.MEMBER_BLOCK_NOT_FOUND);
            }
        }
    }
}