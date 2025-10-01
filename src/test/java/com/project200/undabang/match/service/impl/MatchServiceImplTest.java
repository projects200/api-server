package com.project200.undabang.match.service.impl;

import com.project200.undabang.match.entity.Match;
import com.project200.undabang.match.repository.MatchRepository;
import com.project200.undabang.member.entity.Member;
import com.project200.undabang.member.repository.MemberRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessException;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
@DisplayName("MatchServiceImpl 테스트")
class MatchServiceImplTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private MatchRepository matchRepository;

    @InjectMocks
    private MatchServiceImpl matchService;

    private Member createTestMember(UUID memberId) {
        return Member.builder()
                .memberId(memberId)
                .memberNickname("test-user-" + memberId)
                .build();
    }

    @Nested
    @DisplayName("createMatchRecordBetweenMembers 메소드는")
    class Describe_createMatchRecordBetweenMembers {

        @Test
        @DisplayName("두 회원이 모두 존재할 경우 매칭 기록을 성공적으로 생성한다")
        void creates_match_record_successfully() {
            // given
            UUID requesterId = UUID.randomUUID();
            UUID receiverId = UUID.randomUUID();
            Member requester = createTestMember(requesterId);
            Member receiver = createTestMember(receiverId);

            given(memberRepository.findById(requesterId)).willReturn(Optional.of(requester));
            given(memberRepository.findById(receiverId)).willReturn(Optional.of(receiver));

            Match newMatch = mock(Match.class);
            // Match.from()은 static 메소드이므로 mockStatic을 사용해야 함
            try (MockedStatic<Match> mockedStatic = mockStatic(Match.class)) {
                mockedStatic.when(() -> Match.from(requester, receiver)).thenReturn(newMatch);

                // when
                matchService.createMatchRecordBetweenMembers(requesterId, receiverId);

                // then
                // 비동기 메소드지만 단위 테스트에서는 동기적으로 실행 로직을 검증할 수 있음
                then(memberRepository).should().findById(requesterId);
                then(memberRepository).should().findById(receiverId);
                mockedStatic.verify(() -> Match.from(requester, receiver));
                then(matchRepository).should().save(newMatch);
            }
        }

        @Test
        @DisplayName("요청자(requester)를 찾을 수 없으면 예외를 로깅하고 조용히 종료된다")
        void logs_error_when_requester_not_found() {
            // given
            UUID requesterId = UUID.randomUUID();
            UUID receiverId = UUID.randomUUID();

            // getMember(requesterId)에서 예외 발생하도록 설정
            given(memberRepository.findById(requesterId)).willReturn(Optional.empty());

            // when
            // 예외가 외부로 전파되지 않는지 확인 (try-catch로 처리되기 때문)
            matchService.createMatchRecordBetweenMembers(requesterId, receiverId);

            // then
            // receiver를 찾는 로직이나 save 로직이 호출되지 않았는지 검증
            then(memberRepository).should(never()).findById(receiverId);
            then(matchRepository).should(never()).save(any(Match.class));
        }

        @Test
        @DisplayName("수신자(receiver)를 찾을 수 없으면 예외를 로깅하고 조용히 종료된다")
        void logs_error_when_receiver_not_found() {
            // given
            UUID requesterId = UUID.randomUUID();
            UUID receiverId = UUID.randomUUID();
            Member requester = createTestMember(requesterId);

            given(memberRepository.findById(requesterId)).willReturn(Optional.of(requester));
            // getMember(receiverId)에서 예외 발생하도록 설정
            given(memberRepository.findById(receiverId)).willReturn(Optional.empty());

            // when
            matchService.createMatchRecordBetweenMembers(requesterId, receiverId);

            // then
            // save 로직이 호출되지 않았는지 검증
            then(matchRepository).should(never()).save(any(Match.class));
        }

        @Test
        @DisplayName("DB 저장 중 예외가 발생하면 로깅하고 조용히 종료된다")
        void logs_error_when_repository_save_fails() {
            // given
            UUID requesterId = UUID.randomUUID();
            UUID receiverId = UUID.randomUUID();
            Member requester = createTestMember(requesterId);
            Member receiver = createTestMember(receiverId);

            given(memberRepository.findById(requesterId)).willReturn(Optional.of(requester));
            given(memberRepository.findById(receiverId)).willReturn(Optional.of(receiver));

            // matchRepository.save() 호출 시 DataAccessException 예외를 던지도록 설정
            willThrow(new DataAccessException("DB save failed") {
            })
                    .given(matchRepository).save(any(Match.class));

            Match newMatch = mock(Match.class);
            try (MockedStatic<Match> mockedStatic = mockStatic(Match.class)) {
                mockedStatic.when(() -> Match.from(requester, receiver)).thenReturn(newMatch);

                // when
                matchService.createMatchRecordBetweenMembers(requesterId, receiverId);

                // then
                // save가 호출되었는지 확인 (호출 자체는 이루어짐)
                then(matchRepository).should().save(newMatch);
                // 추가적으로, 여기서 슬랙 알림 등의 로직이 있다면 해당 Mock 객체의 verify를 추가할 수 있음
            }
        }
    }
}