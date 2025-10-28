package com.project200.undabang.member.service.impl;

import com.project200.undabang.common.context.UserContextHolder;
import com.project200.undabang.common.web.exception.CustomException;
import com.project200.undabang.common.web.exception.ErrorCode;
import com.project200.undabang.member.dto.record.MemberBlockRecord;
import com.project200.undabang.member.dto.response.GetBlockedMembersResponse;
import com.project200.undabang.member.entity.Member;
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
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MemberBlockQueryServiceImplTest {

    @InjectMocks
    private MemberBlockQueryServiceImpl memberBlockQueryService;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private MemberBlockRepository memberBlockRepository;

    private Member createMember(UUID id, String nickname) {
        return Member.builder()
                .memberId(id)
                .memberNickname(nickname)
                .memberEmail(nickname + "@test.com")
                .memberBday(LocalDate.of(2000, 1, 1))
                .build();
    }

    @Nested
    @DisplayName("getBlockedMembers 메소드는")
    class Describe_getBlockedMembers {

        private final UUID MEMBER_ID = UUID.randomUUID();

        @Test
        @DisplayName("차단한 회원 목록을 DTO 리스트로 변환하여 성공적으로 반환한다")
        void it_returns_list_of_dto_successfully() {
            // given
            Member requester = createMember(MEMBER_ID, "요청자");
            List<MemberBlockRecord> mockRecords = List.of(
                    new MemberBlockRecord(1L, UUID.randomUUID(), "차단된사람1", "url1", "thumb1", LocalDateTime.now()),
                    new MemberBlockRecord(2L, UUID.randomUUID(), "차단된사람2", "url2", "thumb2", LocalDateTime.now())
            );

            // [수정] 각 리포지토리가 자신의 책임을 수행하도록 모킹(Mocking)
            when(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.of(requester));
            when(memberBlockRepository.findAllMemberBlockRecordsByMember(requester)).thenReturn(mockRecords);

            // when
            List<GetBlockedMembersResponse> result;
            try (MockedStatic<UserContextHolder> mocked = mockStatic(UserContextHolder.class)) {
                mocked.when(UserContextHolder::getUserId).thenReturn(MEMBER_ID);
                result = memberBlockQueryService.getBlockedMembers();
            }

            // then
            assertThat(result).hasSize(2);
            assertThat(result.get(0).getNickname()).isEqualTo("차단된사람1");

            // [수정] 각 리포지토리의 메소드가 정확히 1번씩 호출되었는지 검증
            verify(memberRepository, times(1)).findById(MEMBER_ID);
            verify(memberBlockRepository, times(1)).findAllMemberBlockRecordsByMember(requester);
        }

        @Test
        @DisplayName("차단한 회원이 없으면 비어있는 DTO 리스트를 반환한다")
        void it_returns_empty_list_when_no_blocks_exist() {
            // given
            Member requester = createMember(MEMBER_ID, "요청자");
            when(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.of(requester));
            when(memberBlockRepository.findAllMemberBlockRecordsByMember(requester)).thenReturn(List.of());

            // when
            List<GetBlockedMembersResponse> result;
            try (MockedStatic<UserContextHolder> mocked = mockStatic(UserContextHolder.class)) {
                mocked.when(UserContextHolder::getUserId).thenReturn(MEMBER_ID);
                result = memberBlockQueryService.getBlockedMembers();
            }

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("요청한 회원이 존재하지 않으면 예외를 발생시킨다")
        void it_throws_exception_when_member_not_found() {
            // given
            when(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.empty());

            // when & then
            try (MockedStatic<UserContextHolder> mocked = mockStatic(UserContextHolder.class)) {
                mocked.when(UserContextHolder::getUserId).thenReturn(MEMBER_ID);

                CustomException exception = assertThrows(CustomException.class, () ->
                        memberBlockQueryService.getBlockedMembers()
                );

                assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.MEMBER_NOT_FOUND);

                // [중요] Member를 찾지 못했으므로, MemberBlockRepository는 절대 호출되지 않아야 함
                verify(memberBlockRepository, never()).findAllMemberBlockRecordsByMember(any(Member.class));
            }
        }
    }
}