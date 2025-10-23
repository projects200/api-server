package com.project200.undabang.member.repository;

import com.project200.undabang.configuration.TestQuerydslConfig;
import com.project200.undabang.member.entity.Member;
import com.project200.undabang.member.entity.MemberBlock;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(TestQuerydslConfig.class)
class MemberBlockRepositoryTest {

    @Autowired
    private MemberBlockRepository memberBlockRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private TestEntityManager em;

    private Member createMember(String nickname) {
        return Member.builder()
                .memberId(UUID.randomUUID())
                .memberEmail(nickname + "@test.com")
                .memberNickname(nickname)
                .memberBday(LocalDate.of(2000, 1, 1))
                .build();
    }

    private void saveAndFlush(Object... entities) {
        for (Object entity : entities) {
            em.persist(entity);
        }
        em.flush();
        em.clear();
    }

    @Nested
    @DisplayName("findByBlockerAndBlocked 메소드는")
    class Describe_findByBlockerAndBlocked {

        @Test
        @DisplayName("유효한 차단 관계가 존재할 경우, MemberBlock 엔티티를 Optional로 감싸 반환한다")
        void it_returns_optional_of_member_block_when_block_exists() {
            // given
            Member blocker = createMember("차단하는사람");
            Member blocked = createMember("차단당한사람");
            MemberBlock memberBlock = MemberBlock.of(blocker, blocked);
            saveAndFlush(blocker, blocked, memberBlock);

            // when
            Optional<MemberBlock> foundBlockOpt = memberBlockRepository.findByBlockerAndBlocked(blocker, blocked);

            // then
            assertThat(foundBlockOpt).isPresent();
            assertThat(foundBlockOpt.get().getId()).isEqualTo(memberBlock.getId());
            assertThat(foundBlockOpt.get().getBlocker().getMemberId()).isEqualTo(blocker.getMemberId());
            assertThat(foundBlockOpt.get().getBlocked().getMemberId()).isEqualTo(blocked.getMemberId());
        }

        @Test
        @DisplayName("차단 관계가 존재하지 않을 경우, 비어있는 Optional을 반환한다")
        void it_returns_empty_optional_when_block_does_not_exist() {
            // given
            Member user1 = createMember("사용자1");
            Member user2 = createMember("사용자2");
            saveAndFlush(user1, user2);

            // when
            Optional<MemberBlock> foundBlockOpt = memberBlockRepository.findByBlockerAndBlocked(user1, user2);

            // then
            assertThat(foundBlockOpt).isEmpty();
        }
    }
}